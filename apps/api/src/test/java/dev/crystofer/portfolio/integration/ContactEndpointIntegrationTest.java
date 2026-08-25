package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import dev.crystofer.portfolio.shared.web.ServiceKeyAuthFilter;

/**
 * {@code POST /api/v1/contact} contra a aplicacao inteira.
 *
 * <p><strong>Cada teste usa um IP proprio, e isso nao e detalhe.</strong> O limitador e um bean
 * unico, entao o balde de um teste sobreviveria ao proximo - e a ordem de execucao passaria a
 * decidir quem recebe 429. IPs distintos dao a cada cenario um balde limpo sem precisar reiniciar
 * nada.
 *
 * <p>O limite e reduzido a tres por hora para que o cenario de excesso caiba num teste. O numero de
 * producao e configuracao, e o que se prova aqui e o <em>comportamento</em> - inclusive o {@code
 * Retry-After}, que e o que separa um 429 util de um que so diz nao.
 */
@TestPropertySource(properties = "portfolio.contact.max-per-hour=3")
class ContactEndpointIntegrationTest extends AbstractIntegrationTest {

  private static final String ROTA = "/api/v1/contact";

  private static String corpo(String nome, String email, String assunto, String mensagem) {
    return corpoComArmadilha(nome, email, assunto, mensagem, "");
  }

  private static String corpoComArmadilha(
      String nome, String email, String assunto, String mensagem, String armadilha) {
    return """
        {"name":"%s","email":"%s","subject":"%s","message":"%s","website":"%s"}
        """
        .formatted(nome, email, assunto, mensagem, armadilha);
  }

  private ResponseEntity<String> enviar(String json, String ip) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(ServiceKeyAuthFilter.HEADER, CHAVE_DE_SERVICO);
    headers.set("X-Forwarded-For", ip);
    return restTemplate.exchange(
        ROTA, HttpMethod.POST, new HttpEntity<>(json, headers), String.class);
  }

  @AfterEach
  void limpar() {
    jdbcTemplate.update("DELETE FROM contact_message");
  }

  private int quantasGravadas() {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM contact_message", Integer.class);
  }

  /**
   * O caminho feliz, e ele responde 202 sem corpo.
   *
   * <p>202 e nao 201 porque nao ha recurso a consultar depois - mensagem de contato nao tem
   * representacao publica. E sem corpo porque devolver o identificador entregaria a quem envia um
   * numero sequencial da tabela, que conta quantas mensagens o portfolio ja recebeu.
   */
  @Test
  @DisplayName("deve aceitar a mensagem com 202 e grava-la")
  void shouldAccept_validMessage() {
    var resposta = enviar(corpo("Fulana", "fulana@exemplo.com", "Vaga", "Ola!"), "203.0.113.10");

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(resposta.getBody()).isNullOrEmpty();
    assertThat(quantasGravadas()).isEqualTo(1);
  }

  /**
   * O IP e gravado como hash, e nunca como endereco.
   *
   * <p>O teste confere as duas metades: que o valor tem a forma de um SHA-256 em hexadecimal, e que
   * <strong>o endereco de origem nao aparece</strong> em lugar nenhum da linha. A segunda e a que
   * importa - um hash correto ao lado de uma coluna que ainda guardasse o IP nao protegeria nada.
   */
  @Test
  @DisplayName("deve gravar o hash da origem, e nunca o IP")
  void shouldStore_hashedOrigin() {
    enviar(corpo("Fulana", "fulana@exemplo.com", "Vaga", "Ola!"), "203.0.113.11");

    var linha = jdbcTemplate.queryForMap("SELECT ip_hash, user_agent FROM contact_message");
    String hash = ((String) linha.get("ip_hash")).trim();

    assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
    assertThat(hash).doesNotContain("203.0.113");
    assertThat(linha.get("user_agent")).isNotNull();
  }

  /**
   * Corpo invalido volta 400, e o Bean Validation e quem decide.
   *
   * <p><strong>Este teste prova que o {@code spring-boot-starter-validation} esta no
   * classpath.</strong> Sem ele as anotacoes do DTO nao fazem nada e nao avisam - o corpo invalido
   * chegaria ao dominio, que lancaria, e o visitante receberia 500 no lugar de 400. Era o defeito
   * do Music Style API, e foi ele que tirou o projeto do curriculo.
   */
  @ParameterizedTest(name = "{0} invalido deve dar 400")
  @CsvSource({
    "nome,     '',      fulana@exemplo.com, Vaga, Ola!",
    "email,    Fulana,  nao-e-email,        Vaga, Ola!",
    "assunto,  Fulana,  fulana@exemplo.com, '',   Ola!",
    "mensagem, Fulana,  fulana@exemplo.com, Vaga, ''",
  })
  @DisplayName("deve recusar corpo invalido com 400")
  void shouldReject_invalidBody(
      String campo, String nome, String email, String assunto, String mensagem) {
    var resposta = enviar(corpo(nome, email, assunto, mensagem), "203.0.113.12");

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(quantasGravadas()).isZero();
  }

  /**
   * O campo-armadilha responde <strong>sucesso</strong> e nao grava nada.
   *
   * <p>E a parte contraintuitiva da defesa: devolver erro ensinaria ao robo qual campo evitar, e a
   * tentativa seguinte passaria. Com 202 ele registra sucesso e vai embora - e a caixa de entrada
   * continua limpa.
   */
  @Test
  @DisplayName("deve descartar em silencio quando o campo-armadilha vem preenchido")
  void shouldSilentlyDiscard_whenHoneypotFilled() {
    var resposta =
        enviar(
            corpoComArmadilha(
                "Robo", "robo@exemplo.com", "Promocao", "Compre isto", "http://spam.example"),
            "203.0.113.13");

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(quantasGravadas()).isZero();
  }

  /**
   * Excedido o limite, a resposta e 429 <strong>com {@code Retry-After}</strong>.
   *
   * <p>O cabecalho e o que torna o 429 acionavel: sem ele, a unica estrategia de quem consome e
   * insistir, que e exatamente o comportamento que o limite existe para conter.
   */
  @Test
  @DisplayName("deve responder 429 com Retry-After ao exceder o limite")
  void shouldReturn429_withRetryAfter() {
    String ip = "203.0.113.14";
    String json = corpo("Fulana", "fulana@exemplo.com", "Vaga", "Ola!");

    for (int i = 0; i < 3; i++) {
      assertThat(enviar(json, ip).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    var excedida = enviar(json, ip);

    assertThat(excedida.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

    String retryAfter = excedida.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
    assertThat(retryAfter).isNotNull();
    assertThat(Long.parseLong(retryAfter)).isPositive();

    // A quarta nao entrou: o limite recusa antes de gravar.
    assertThat(quantasGravadas()).isEqualTo(3);
  }

  /**
   * O limite e por remetente, e nao global.
   *
   * <p>Sem isso, cinco mensagens por hora valeriam para o site inteiro e um unico visitante
   * insistente calaria todos os outros - que e o efeito de ignorar o {@code X-Forwarded-For} atras
   * do proxy da plataforma, onde toda conexao chega com o mesmo IP.
   */
  @Test
  @DisplayName("nao deve limitar um remetente por causa de outro")
  void shouldLimit_perSender() {
    String json = corpo("Fulana", "fulana@exemplo.com", "Vaga", "Ola!");

    for (int i = 0; i < 3; i++) {
      enviar(json, "203.0.113.15");
    }
    assertThat(enviar(json, "203.0.113.15").getStatusCode())
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

    assertThat(enviar(json, "203.0.113.16").getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
  }
}
