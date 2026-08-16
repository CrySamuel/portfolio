package dev.crystofer.portfolio.shared.config;

import java.util.function.Consumer;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import dev.crystofer.portfolio.shared.config.properties.GitHubProperties;

/**
 * O cliente HTTP de saida do sistema - hoje, so o do GitHub.
 *
 * <p><strong>Timeout nao e detalhe de configuracao, e a primeira linha de defesa.</strong> Sem ele
 * o cliente espera pelo padrao do sistema operacional, que pode passar de um minuto: a chamada
 * trava, a thread fica presa e o disjuntor do commit 42 nunca chega a contar a falha, porque a
 * requisicao nao terminou. Os dois valores vem da secao 3.10 e sao configuraveis para que o teste
 * possa encurta-los.
 *
 * <p><strong>Um bean por integracao, e nao um {@code RestClient} generico.</strong> Cabecalhos,
 * raiz e prazos sao decisoes sobre <em>aquela</em> API; um cliente compartilhado obrigaria cada
 * chamador a lembrar de repeti-las, e esquecer significa chamada anonima com outro formato de
 * resposta.
 */
@Configuration
@EnableConfigurationProperties(GitHubProperties.class)
public class RestClientConfig {

  /**
   * A versao do contrato, fixada.
   *
   * <p>O GitHub versiona a API por cabecalho e muda o default com o tempo. Sem declarar, a resposta
   * pode mudar de forma num dia qualquer, sem deploy nenhum do nosso lado - e o sintoma seria um
   * campo virando nulo em producao.
   */
  private static final String API_VERSION = "2022-11-28";

  private static final String ACCEPT_REST = "application/vnd.github+json";

  @Bean
  RestClient gitHubRestClient(GitHubProperties properties) {
    return build(properties, properties.baseUrl(), ACCEPT_REST);
  }

  /**
   * Cliente separado para o GraphQL, e a separacao e do proprio GitHub: outro endereco, outro
   * metodo, outro formato de resposta. O que os dois compartilham - prazos e credencial - vem do
   * mesmo lugar aqui embaixo.
   */
  @Bean
  RestClient gitHubGraphQlClient(GitHubProperties properties) {
    return build(properties, properties.graphqlUrl(), "application/json");
  }

  private static RestClient build(GitHubProperties properties, String baseUrl, String accept) {
    // HttpClientSettings, e nao ClientHttpRequestFactorySettings: o Boot 4
    // renomeou o tipo e o mudou de org.springframework.http.client para
    // org.springframework.boot.http.client. O nome antigo nao existe mais, entao
    // a migracao falha na compilacao - que e o modo certo de falhar.
    var settings =
        HttpClientSettings.defaults()
            .withTimeouts(properties.connectTimeout(), properties.readTimeout());

    return RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
        .defaultHeaders(cabecalhos(properties, accept))
        .build();
  }

  /**
   * O cabecalho de autorizacao so existe quando ha token.
   *
   * <p>Mandar {@code Authorization: Bearer } com valor vazio nao e o mesmo que nao mandar: o GitHub
   * responde <strong>401</strong> em vez de tratar a chamada como anonima. Seria uma falha que so
   * aparece na maquina de quem nao cadastrou o token - ou seja, em desenvolvimento.
   */
  private static Consumer<HttpHeaders> cabecalhos(GitHubProperties properties, String accept) {
    return headers -> {
      headers.set(HttpHeaders.ACCEPT, accept);
      headers.set("X-GitHub-Api-Version", API_VERSION);
      if (properties.hasToken()) {
        headers.setBearerAuth(properties.token());
      }
    };
  }
}
