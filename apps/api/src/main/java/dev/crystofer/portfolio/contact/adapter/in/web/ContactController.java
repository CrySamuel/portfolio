package dev.crystofer.portfolio.contact.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.crystofer.portfolio.contact.adapter.in.web.dto.ContactRequest;
import dev.crystofer.portfolio.contact.domain.model.ContactMessage;
import dev.crystofer.portfolio.contact.domain.port.in.SubmitContactMessageUseCase;
import dev.crystofer.portfolio.shared.config.properties.ContactProperties;
import dev.crystofer.portfolio.shared.domain.EmailAddress;
import dev.crystofer.portfolio.shared.error.RateLimitExceededException;
import dev.crystofer.portfolio.shared.web.ContactRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Recebe as mensagens do formulario de contato.
 *
 * <p><strong>A unica rota de escrita desta API.</strong> As outras quatro publicam conteudo que
 * entrou por migracao; esta aceita dado de quem o portfolio nao conhece, e tudo o que ela faz de
 * diferente vem dessa inversao - validacao de corpo, limite de taxa, campo-armadilha e hash de
 * origem.
 *
 * <p><strong>Responde 202, e nao 201.</strong> O 201 prometeria um recurso criado e um endereco
 * para consulta-lo, e nao ha nem um nem outro: mensagem de contato nao tem representacao publica, e
 * expor uma seria publicar o que estranhos escrevem. O 202 diz o que de fato aconteceu - a mensagem
 * foi aceita e sera processada -, que e exatamente a promessa desta cadeia: a partir do commit, ela
 * nao se perde mais.
 *
 * <p><strong>Sem corpo na resposta.</strong> Devolver o identificador daria a quem envia um numero
 * sequencial da tabela, que informa quantas mensagens o portfolio ja recebeu - dado de negocio
 * entregue a quem nao precisa dele.
 */
@RestController
@RequestMapping("/api/v1/contact")
@Tag(name = "Contato", description = "Recebimento de mensagens do formulario")
public class ContactController {

  private static final Logger log = LoggerFactory.getLogger(ContactController.class);

  private final SubmitContactMessageUseCase envio;
  private final ContactRateLimiter limitador;
  private final String sal;

  ContactController(
      SubmitContactMessageUseCase envio,
      ContactRateLimiter limitador,
      ContactProperties properties) {
    this.envio = envio;
    this.limitador = limitador;
    this.sal = resolverSal(properties);
  }

  /**
   * O sal do hash, resolvido uma vez no boot.
   *
   * <p><strong>Sem sal configurado, sorteia-se um em vez de hashear sem ele.</strong> Hash de IP
   * sem sal e reversivel por forca bruta em minutos - o espaco IPv4 tem 4 bilhoes de itens -, entao
   * "sem sal" equivaleria a guardar o IP em claro, que e precisamente o que a coluna existe para
   * evitar.
   *
   * <p>O custo do sal sorteado e conhecido: os hashes deixam de ser comparaveis entre reinicios.
   * Perder correlacao e melhor que perder a protecao, e em producao a variavel esta cadastrada.
   */
  private static String resolverSal(ContactProperties properties) {
    if (properties.saltConfigured()) {
      return properties.ipHashSalt();
    }

    var aleatorio = new byte[32];
    new SecureRandom().nextBytes(aleatorio);
    log.warn(
        "Sem sal configurado para o hash de IP: um foi sorteado para esta execucao."
            + " Os hashes nao serao comparaveis apos um reinicio.");
    return HexFormat.of().formatHex(aleatorio);
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(
      summary = "Envia uma mensagem de contato",
      description =
          "A mensagem e persistida antes de qualquer tentativa de envio de e-mail, entao ela nao"
              + " se perde se o provedor estiver fora do ar.")
  @ApiResponse(responseCode = "202", description = "Mensagem aceita e enfileirada para notificacao")
  @ApiResponse(responseCode = "400", description = "Corpo invalido")
  @ApiResponse(responseCode = "429", description = "Limite de mensagens por hora excedido")
  public void enviar(@Valid @RequestBody ContactRequest requisicao, HttpServletRequest http) {
    String origem = hashDaOrigem(http);

    // O limite vem antes de tudo, inclusive do honeypot: quem esta sendo
    // limitado nao deve conseguir descobrir nada sobre as outras defesas
    // variando o corpo da requisicao.
    if (!limitador.tryConsume(origem)) {
      throw new RateLimitExceededException(limitador.secondsUntilRefill(origem));
    }

    // Campo-armadilha preenchido: responde 202 e nao grava nada. **O silencio e
    // a defesa.** Devolver erro ensinaria ao robo qual campo evitar, e a
    // proxima tentativa passaria - com 202, ele registra sucesso e vai embora.
    if (requisicao.website() != null && !requisicao.website().isBlank()) {
      log.info("Mensagem descartada pelo campo-armadilha");
      return;
    }

    envio.submit(
        ContactMessage.received(
            requisicao.name(),
            new EmailAddress(requisicao.email()),
            requisicao.subject(),
            requisicao.message(),
            origem,
            http.getHeader("User-Agent")));
  }

  /**
   * O IP da origem, ja hasheado - <strong>o endereco cru nunca sai deste metodo</strong>.
   *
   * <p>{@code X-Forwarded-For} e lido porque a aplicacao roda atras do proxy da plataforma, onde o
   * IP da conexao e sempre o do proxy. O <em>primeiro</em> valor da lista e o cliente; os seguintes
   * sao os saltos intermediarios.
   *
   * <p>⚠️ <strong>O cabecalho e falsificavel, e isso e aceito aqui.</strong> Qualquer um pode
   * mandar um {@code X-Forwarded-For} inventado e contornar o limite de taxa. Confiar nele mesmo
   * assim e a escolha certa <em>neste</em> caso: o limite e uma barreira contra volume acidental e
   * robo preguicoso, nao contra adversario dedicado - contra esse, quem trabalha e o Turnstile.
   * Ignorar o cabecalho seria pior: sem ele, todos os visitantes compartilhariam o IP do proxy e
   * cinco mensagens por hora valeriam para o site inteiro.
   */
  private String hashDaOrigem(HttpServletRequest http) {
    String encaminhado = http.getHeader("X-Forwarded-For");
    String ip =
        encaminhado == null || encaminhado.isBlank()
            ? http.getRemoteAddr()
            : encaminhado.split(",")[0].trim();

    return hash(ip);
  }

  /**
   * SHA-256 do IP com o sal, em hexadecimal.
   *
   * <p>{@code MessageDigest} nao e seguro para uso concorrente, entao a instancia e criada a cada
   * chamada em vez de guardada num campo. Reusar uma instancia entre requisicoes produziria hashes
   * corrompidos sob carga - o tipo de defeito que nao aparece em teste e aparece em producao.
   */
  private String hash(String ip) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      digest.update(sal.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest.digest(ip.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException impossivel) {
      // SHA-256 e obrigatorio em toda implementacao da plataforma Java desde
      // sempre. Se faltar, a JVM esta quebrada de um jeito que nenhum fallback
      // aqui resolveria.
      throw new IllegalStateException("SHA-256 indisponivel nesta JVM", impossivel);
    }
  }
}
