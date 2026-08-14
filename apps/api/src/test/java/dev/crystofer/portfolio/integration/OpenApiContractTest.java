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

  @Test
  @DisplayName("a resposta da timeline e publicada como application/json, e nao como coringa")
  void shouldPublishJsonMediaType_forTheExperienceResponse() {
    String body = fetchApiDocs();

    assertThat(json.from(body))
        .extractingJsonPathMapValue("$.paths./api/v1/experiences.get.responses.200.content")
        .containsOnlyKeys("application/json");
  }

  /** O endpoint devolve um array puro, e o contrato precisa dizer isso ao cliente gerado. */
  @Test
  @DisplayName("a timeline e publicada como array de Experience")
  void shouldPublishAnArrayOfExperience_forTheTimeline() {
    String body = fetchApiDocs();

    var schema = "$.paths./api/v1/experiences.get.responses.200.content.application/json.schema";
    assertThat(json.from(body)).extractingJsonPathStringValue(schema + ".type").isEqualTo("array");
    assertThat(json.from(body))
        .extractingJsonPathStringValue(schema + ".items.$ref")
        .isEqualTo("#/components/schemas/Experience");
  }

  /**
   * O campo nulavel da timeline carrega significado, e nao apenas ausencia.
   *
   * <p>{@code endDate} nulo <em>e</em> o que define cargo atual. Publicado como {@code string}
   * simples, o TypeScript prometeria uma data que nunca chega, e o componente do badge "Atual"
   * quebraria na posicao mais importante da timeline. Nada disto tem efeito no Java.
   */
  @Test
  @DisplayName("todo campo da experiencia e required, e endDate diz no tipo que pode vir nulo")
  void shouldDeclareRequiredAndNullableFields_forTheExperienceSchema() {
    String body = fetchApiDocs();

    assertThat(json.from(body))
        .extractingJsonPathArrayValue("$.components.schemas.Experience.required")
        .containsExactlyInAnyOrder(
            "company", "role", "startDate", "endDate", "description", "highlights");

    assertThat(json.from(body))
        .extractingJsonPathArrayValue("$.components.schemas.Experience.properties.endDate.type")
        .containsExactly("string", "null");

    // Nao nulavel: a entrada sempre existe. A distincao so vale onde ha ausencia real.
    assertThat(json.from(body))
        .extractingJsonPathStringValue("$.components.schemas.Experience.properties.startDate.type")
        .isEqualTo("string");
  }

  /**
   * O {@code format} sobrevive ao tipo composto - e isso foi medido, nao suposto.
   *
   * <p>A suposicao ao escrever o DTO era a oposta: que declarar {@code types = {"string","null"}}
   * substituiria o {@code type} inferido de {@code LocalDate} e levaria junto o {@code format:
   * date}, deixando o campo como texto livre no contrato. O DTO chegou a trazer {@code format =
   * "date"} escrito a mao por causa disso. Removida a declaracao, o documento continuou publicando
   * {@code format: date} nos dois campos - o springdoc infere a partir do tipo Java
   * independentemente do {@code types}. A anotacao redundante saiu.
   *
   * <p>O teste fica, com outro papel: ele passa a vigiar essa inferencia. Uma versao futura do
   * springdoc que deixasse de aplica-la publicaria data como texto livre, e nada do lado Java
   * mudaria de comportamento para avisar.
   */
  @Test
  @DisplayName("as duas datas publicam format date, inclusive a nulavel")
  void shouldKeepDateFormat_forBothDateFields() {
    String body = fetchApiDocs();

    assertThat(json.from(body))
        .extractingJsonPathStringValue(
            "$.components.schemas.Experience.properties.startDate.format")
        .isEqualTo("date");
    assertThat(json.from(body))
        .extractingJsonPathStringValue("$.components.schemas.Experience.properties.endDate.format")
        .isEqualTo("date");
  }

  /** Lista de texto, e nao {@code array} sem item declarado - que viraria {@code unknown[]}. */
  @Test
  @DisplayName("highlights publica o tipo dos itens")
  void shouldPublishItemTypeForHighlights() {
    String body = fetchApiDocs();

    assertThat(json.from(body))
        .extractingJsonPathStringValue("$.components.schemas.Experience.properties.highlights.type")
        .isEqualTo("array");
    assertThat(json.from(body))
        .extractingJsonPathStringValue(
            "$.components.schemas.Experience.properties.highlights.items.type")
        .isEqualTo("string");
  }

  @Test
  @DisplayName("as competencias sao publicadas como array de SkillCategory, em application/json")
  void shouldPublishGroupedSkills_forTheSkillsEndpoint() {
    String body = fetchApiDocs();

    assertThat(json.from(body))
        .extractingJsonPathMapValue("$.paths./api/v1/skills.get.responses.200.content")
        .containsOnlyKeys("application/json");

    var schema = "$.paths./api/v1/skills.get.responses.200.content.application/json.schema";
    assertThat(json.from(body)).extractingJsonPathStringValue(schema + ".type").isEqualTo("array");
    assertThat(json.from(body))
        .extractingJsonPathStringValue(schema + ".items.$ref")
        .isEqualTo("#/components/schemas/SkillCategory");
  }

  /**
   * O campo nulavel aqui e numerico, e o tipo composto precisa dizer isso.
   *
   * <p>{@code yearsOfExperience} nulo significa "sem numero declarado", que e diferente de zero -
   * zero e de quem comecou agora. Publicado como {@code integer} simples, o TypeScript prometeria
   * um numero que as vezes nao vem, e a tela quebraria ao formata-lo.
   */
  @Test
  @DisplayName("todo campo da competencia e required, e yearsOfExperience diz que pode vir nulo")
  void shouldDeclareRequiredAndNullableFields_forTheSkillSchema() {
    String body = fetchApiDocs();

    assertThat(json.from(body))
        .extractingJsonPathArrayValue("$.components.schemas.SkillCategory.required")
        .containsExactlyInAnyOrder("name", "skills");

    assertThat(json.from(body))
        .extractingJsonPathArrayValue("$.components.schemas.Skill.required")
        .containsExactlyInAnyOrder("name", "proficiency", "yearsOfExperience");

    assertThat(json.from(body))
        .extractingJsonPathArrayValue(
            "$.components.schemas.Skill.properties.yearsOfExperience.type")
        .containsExactly("integer", "null");

    // Nao nulavel: string e string. A distincao so vale onde ha ausencia real.
    assertThat(json.from(body))
        .extractingJsonPathStringValue("$.components.schemas.Skill.properties.name.type")
        .isEqualTo("string");
  }

  /** O enum e o que faz {@code proficiency} virar uniao literal no TypeScript, e nao string. */
  @Test
  @DisplayName("proficiency publica a escala fechada de niveis")
  void shouldPublishTheProficiencyEnum() {
    String body = fetchApiDocs();

    assertThat(json.from(body))
        .extractingJsonPathArrayValue("$.components.schemas.Skill.properties.proficiency.enum")
        .containsExactly("basic", "intermediate", "advanced");
  }

  @Test
  @DisplayName("as duas rotas de projeto sao publicadas como application/json")
  void shouldPublishJsonMediaType_forTheProjectRoutes() {
    String body = fetchApiDocs();

    assertThat(json.from(body))
        .extractingJsonPathMapValue("$.paths./api/v1/projects.get.responses.200.content")
        .containsOnlyKeys("application/json");
    assertThat(json.from(body))
        .extractingJsonPathMapValue("$.paths./api/v1/projects/{slug}.get.responses.200.content")
        .containsOnlyKeys("application/json");
  }

  /**
   * A listagem publica o resumo, e o detalhe publica o detalhe - sao schemas distintos.
   *
   * <p>Se as duas rotas apontassem para o mesmo schema, o cliente TypeScript prometeria a narrativa
   * completa nos cards. O componente compilaria, leria {@code problem} e receberia {@code
   * undefined} em producao.
   */
  @Test
  @DisplayName("a listagem publica array de ProjectSummary e o detalhe publica ProjectDetail")
  void shouldPublishDistinctSchemas_forListingAndDetail() {
    String body = fetchApiDocs();

    assertThat(json.from(body))
        .extractingJsonPathStringValue(
            "$.paths./api/v1/projects.get.responses.200.content.application/json.schema.type")
        .isEqualTo("array");
    assertThat(json.from(body))
        .extractingJsonPathStringValue(
            "$.paths./api/v1/projects.get.responses.200.content.application/json.schema.items.$ref")
        .isEqualTo("#/components/schemas/ProjectSummary");
    assertThat(json.from(body))
        .extractingJsonPathStringValue(
            "$.paths./api/v1/projects/{slug}.get.responses.200.content.application/json.schema.$ref")
        .isEqualTo("#/components/schemas/ProjectDetail");
  }

  /**
   * Os quatro nulaveis do detalhe, que sao os que mais pesam neste commit.
   *
   * <p>{@code repoUrl} e {@code liveUrl} decidem se o botao existe; publicados como {@code string}
   * simples, o TypeScript prometeria endereco onde chega {@code null} e o componente montaria um
   * link para lugar nenhum.
   */
  @Test
  @DisplayName("todo campo do detalhe e required, e os quatro nulaveis dizem isso no tipo")
  void shouldDeclareRequiredAndNullableFields_forTheProjectDetailSchema() {
    String body = fetchApiDocs();

    assertThat(json.from(body))
        .extractingJsonPathArrayValue("$.components.schemas.ProjectDetail.required")
        .containsExactlyInAnyOrder(
            "slug",
            "title",
            "summary",
            "problem",
            "solution",
            "outcome",
            "repoUrl",
            "liveUrl",
            "coverImage",
            "featured",
            "publishedAt",
            "technologies",
            "metrics");

    for (String nulavel : new String[] {"repoUrl", "liveUrl", "coverImage", "publishedAt"}) {
      assertThat(json.from(body))
          .extractingJsonPathArrayValue(
              "$.components.schemas.ProjectDetail.properties." + nulavel + ".type")
          .as("o nulavel %s precisa publicar a uniao com null", nulavel)
          .containsExactly("string", "null");
    }

    // Nao nulavel: sempre existe. A distincao so vale onde ha ausencia real.
    assertThat(json.from(body))
        .extractingJsonPathStringValue("$.components.schemas.ProjectDetail.properties.slug.type")
        .isEqualTo("string");
  }

  /**
   * O resumo omite a narrativa e os enderecos, e o contrato registra essa omissao.
   *
   * <p>O {@code unmappedTargetPolicy} do MapStruct guarda o sentido contrario - campo do DTO sem
   * origem no dominio -, mas nao tem como distinguir omissao deliberada de esquecimento. Esta e a
   * unica guarda sobre a forma reduzida do card.
   */
  @Test
  @DisplayName("o resumo nao publica narrativa, enderecos nem metricas")
  void shouldKeepTheSummaryLean() {
    String body = fetchApiDocs();

    assertThat(json.from(body))
        .extractingJsonPathArrayValue("$.components.schemas.ProjectSummary.required")
        .containsExactlyInAnyOrder(
            "slug", "title", "summary", "coverImage", "featured", "publishedAt", "technologies")
        .doesNotContain("problem", "solution", "outcome", "repoUrl", "liveUrl", "metrics");

    // Os dois nulaveis do resumo precisam da mesma uniao que os do detalhe. Esta
    // metade faltava na primeira versao do teste, e a falta so apareceu ao montar
    // a quebra proposital - o que e, por si, a razao de quebrar.
    for (String nulavel : new String[] {"coverImage", "publishedAt"}) {
      assertThat(json.from(body))
          .extractingJsonPathArrayValue(
              "$.components.schemas.ProjectSummary.properties." + nulavel + ".type")
          .as("o nulavel %s do resumo precisa publicar a uniao com null", nulavel)
          .containsExactly("string", "null");
    }
  }

  /** O enum e o que faz {@code category} virar uniao literal no TypeScript, e nao string. */
  @Test
  @DisplayName("category publica o conjunto fechado de familias")
  void shouldPublishTheTechnologyCategoryEnum() {
    String body = fetchApiDocs();

    assertThat(json.from(body))
        .extractingJsonPathArrayValue("$.components.schemas.Technology.properties.category.enum")
        .containsExactlyInAnyOrder("language", "framework", "database", "infrastructure", "tool");
  }

  private String fetchApiDocs() {
    ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response.getBody();
  }
}
