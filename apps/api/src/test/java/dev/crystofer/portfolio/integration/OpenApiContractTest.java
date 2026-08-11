package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * O documento OpenAPI publicado, tratado como contrato e nao como documentacao.
 *
 * <p>Deste arquivo sai o {@code @portfolio/api-client}, entao imprecisao aqui nao fica na
 * documentacao: vira tipo TypeScript errado no front. Nada disto tem efeito no Java - o endpoint
 * responde igual com ou sem as anotacoes -, o que significa que sem um teste a regressao seria
 * invisivel deste lado e apareceria como bug do outro.
 *
 * <p>As tres propriedades afirmadas abaixo estavam todas erradas quando o cliente foi gerado pela
 * primeira vez, e nenhuma quebrava teste nenhum.
 */
class OpenApiContractTest extends AbstractIntegrationTest {

  private final BasicJsonTester json = new BasicJsonTester(getClass());

  /**
   * O tipo de midia da resposta.
   *
   * <p>Sem {@code produces} no mapeamento, o springdoc publica a resposta sob {@code *}/{@code *} -
   * "qualquer coisa". O gerador entao nao encontra {@code application/json} e produz um cliente que
   * nao sabe o que recebe.
   */
  @Test
  @DisplayName("a resposta do perfil e publicada como application/json, e nao como coringa")
  void shouldPublishJsonMediaType_forTheProfileResponse() {
    String body = fetchApiDocs();

    assertThat(json.from(body))
        .extractingJsonPathMapValue("$.paths./api/v1/profile.get.responses.200.content")
        .containsOnlyKeys("application/json");
  }

  /**
   * Campo obrigatorio e campo nulavel sao coisas diferentes, e o cliente precisa das duas.
   *
   * <p>{@code required} promete que a chave esta no JSON; o tipo diz o que cabe dentro dela. Sem a
   * lista, todo campo sai opcional no TypeScript e o front passa a tratar uma ausencia que nunca
   * acontece. Com ela e sem a nulabilidade, acontece o oposto e pior: o tipo promete {@code string}
   * onde a API manda {@code null}.
   */
  @Test
  @DisplayName("todo campo do perfil e required, e os que podem vir nulos dizem isso no tipo")
  void shouldDeclareRequiredAndNullableFields_forTheProfileSchema() {
    String body = fetchApiDocs();

    assertThat(json.from(body))
        .extractingJsonPathArrayValue("$.components.schemas.Profile.required")
        .containsExactlyInAnyOrder(
            "fullName",
            "headline",
            "bio",
            "location",
            "resumeUrl",
            "availableForWork",
            "socialLinks");

    assertThat(json.from(body))
        .extractingJsonPathArrayValue("$.components.schemas.Profile.properties.location.type")
        .containsExactly("string", "null");
    assertThat(json.from(body))
        .extractingJsonPathArrayValue("$.components.schemas.Profile.properties.resumeUrl.type")
        .containsExactly("string", "null");

    // Nao nulavel: string e string. A distincao so vale onde ha ausencia real.
    assertThat(json.from(body))
        .extractingJsonPathStringValue("$.components.schemas.Profile.properties.fullName.type")
        .isEqualTo("string");

    assertThat(json.from(body))
        .extractingJsonPathArrayValue("$.components.schemas.SocialLink.required")
        .containsExactlyInAnyOrder("platform", "url");
  }

  /** O enum e o que faz {@code platform} virar uniao literal no TypeScript, em vez de string. */
  @Test
  @DisplayName("platform publica o conjunto fechado de plataformas")
  void shouldPublishThePlatformEnum() {
    String body = fetchApiDocs();

    assertThat(json.from(body))
        .extractingJsonPathArrayValue("$.components.schemas.SocialLink.properties.platform.enum")
        .containsExactly("github", "linkedin", "email");
  }

  private String fetchApiDocs() {
    ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response.getBody();
  }
}
