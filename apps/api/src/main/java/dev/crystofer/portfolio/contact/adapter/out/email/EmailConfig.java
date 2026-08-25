package dev.crystofer.portfolio.contact.adapter.out.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import dev.crystofer.portfolio.contact.domain.port.out.SendContactEmailPort;
import dev.crystofer.portfolio.shared.config.properties.EmailProperties;

/**
 * Escolhe por onde a notificacao sai.
 *
 * <p><strong>A escolha fica num lugar so, e explicita.</strong> A alternativa - dois
 * {@code @Component} com {@code @ConditionalOnProperty} - espalharia a decisao por dois arquivos e
 * dependeria de os dois conjuntos de condicoes serem exatamente complementares. Bastaria um erro de
 * digitacao no nome da propriedade para a aplicacao subir com <em>nenhum</em> adaptador - ou, pior,
 * com <em>dois</em>, e ai qual ganha depende da ordem de varredura.
 *
 * <p><strong>Esta configuracao mora no modulo, e nao em {@code shared}.</strong> Ela menciona tipos
 * de {@code contact}, e {@code shared} nao pode depender de modulo nenhum - a regra de fronteira do
 * ArchUnit reprovaria o build. O {@code EmailProperties} pode ficar em {@code shared} porque a seta
 * aponta ao contrario: modulo depende de shared, e nao o inverso.
 *
 * <p>O aviso no boot existe porque a alternativa e pior. Sem ele, uma implantacao sem a chave
 * subiria calada, marcaria toda mensagem como entregue e ninguem descobriria ate alguem perguntar
 * por que nao recebeu resposta.
 */
@Configuration
@EnableConfigurationProperties(EmailProperties.class)
class EmailConfig {

  private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

  @Bean
  SendContactEmailPort sendContactEmailPort(EmailProperties properties) {
    if (!properties.configured()) {
      log.warn(
          "Sem credencial de e-mail: nenhuma notificacao sera enviada de verdade."
              + " As mensagens continuam sendo gravadas.");
      return new LoggingEmailAdapter();
    }

    return new ResendEmailAdapter(clienteDoProvedor(properties), properties);
  }

  /**
   * O cliente do provedor, com credencial e prazos.
   *
   * <p>Prazo de leitura de dez segundos, e nao os tres do GitHub. Quem espera aqui e uma thread de
   * segundo plano, sem ninguem do outro lado - o custo de esperar mais e nenhum, e o custo de
   * desistir cedo e uma mensagem marcada como falha que teria sido entregue.
   */
  private static RestClient clienteDoProvedor(EmailProperties properties) {
    var settings =
        HttpClientSettings.defaults()
            .withTimeouts(properties.connectTimeout(), properties.readTimeout());

    return RestClient.builder()
        .baseUrl(properties.baseUrl())
        .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
