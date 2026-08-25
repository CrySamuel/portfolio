package dev.crystofer.portfolio.contact.adapter.out.email;

import java.util.Map;

import org.springframework.web.client.RestClient;

import dev.crystofer.portfolio.contact.domain.model.ContactMessage;
import dev.crystofer.portfolio.contact.domain.port.out.SendContactEmailPort;
import dev.crystofer.portfolio.shared.config.properties.EmailProperties;

/**
 * Envia a notificacao pelo Resend.
 *
 * <p><strong>Nao trata erro, e a ausencia e a decisao.</strong> O {@code RestClient} ja lanca em
 * qualquer resposta fora de 2xx, e e exatamente isso que o caso de uso espera para gravar {@code
 * FAILED}. Um {@code catch} aqui converteria a falha em silencio - e a mensagem seria marcada como
 * entregue sem ter saido, que e o unico desfecho inaceitavel desta cadeia.
 *
 * <p><strong>O {@code Reply-To} e o visitante, e o remetente nao.</strong> A tentacao e colocar o
 * e-mail de quem escreveu no campo {@code from}, para responder direto - e isso e falsificacao de
 * remetente: o dominio do envio nao pertence ao visitante, entao SPF e DKIM reprovam e a mensagem
 * cai em spam ou e recusada. Com {@code Reply-To}, responder no cliente de e-mail vai para o
 * visitante, e a autenticacao continua valida.
 *
 * <p><strong>O corpo vai em texto puro, e nao em HTML.</strong> Notificacao interna com quatro
 * campos nao ganha nada com marcacao, e texto puro tem duas vantagens concretas: nao carrega
 * conteudo de terceiro para dentro de um documento renderizado - o texto vem de um desconhecido -,
 * e pontua melhor nos filtros de spam, que desconfiam de HTML sem versao textual.
 *
 * <p><strong>O conteudo do visitante nunca entra no assunto.</strong> Ele vai inteiro no corpo, e o
 * assunto e montado aqui: assunto e cabecalho, e cabecalho aceita quebra de linha como separador -
 * um texto com quebra ali abriria injecao de cabecalho, que e como se acrescenta um destinatario
 * oculto a uma mensagem alheia.
 */
class ResendEmailAdapter implements SendContactEmailPort {

  private final RestClient http;
  private final EmailProperties properties;

  ResendEmailAdapter(RestClient http, EmailProperties properties) {
    this.http = http;
    this.properties = properties;
  }

  @Override
  public void send(ContactMessage message) {
    http.post()
        .uri("/emails")
        .body(
            Map.of(
                "from", properties.sender(),
                "to", properties.recipient(),
                "reply_to", message.email().value(),
                "subject", assunto(message),
                "text", corpo(message)))
        .retrieve()
        .toBodilessEntity();
  }

  /**
   * O assunto e montado, e o unico trecho variavel dele e o nome - ja aparado e limitado a 120
   * caracteres pelo invariante do dominio.
   *
   * <p>As quebras de linha saem mesmo assim. O dominio recusa nome vazio e nome longo, mas nao
   * recusa quebra de linha no meio - e nao deveria, porque isso e regra do transporte e nao do
   * conteudo. Quem conhece a regra do transporte e este adaptador.
   */
  private static String assunto(ContactMessage message) {
    return "Contato pelo portfolio: " + message.name().replaceAll("[\\r\\n]", " ");
  }

  private static String corpo(ContactMessage message) {
    return """
        De: %s <%s>
        Assunto declarado: %s

        %s
        """
        .formatted(message.name(), message.email().value(), message.subject(), message.message());
  }
}
