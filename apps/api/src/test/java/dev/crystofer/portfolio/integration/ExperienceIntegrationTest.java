package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.http.HttpStatus;

import dev.crystofer.portfolio.profile.domain.model.Experience;
import dev.crystofer.portfolio.profile.domain.port.in.ListExperiencesUseCase;
import dev.crystofer.portfolio.profile.domain.port.out.LoadExperiencePort;
import dev.crystofer.portfolio.support.fixtures.ExperienceFixtures;

/**
 * O caminho do banco ate o dominio, com Postgres de verdade.
 *
 * <p>Existe porque duas promessas deste commit nao sao verificaveis em nenhuma outra camada.
 *
 * <p>A primeira e a leitura do {@code jsonb}. O {@code ddl-auto: validate} confere que a coluna
 * existe e tem o tipo esperado, e para por ai - ele nao le uma linha. O teste do mapper monta a
 * entidade a mao, entao tambem nao exercita o Hibernate convertendo {@code jsonb} em {@code
 * List<String>}. Sem este teste, um mapeamento errado passaria por toda a suite e falharia na
 * primeira leitura real.
 *
 * <p>A segunda e a ordenacao ponta a ponta, com a consulta de verdade no meio.
 *
 * <p>SQL cru para preparar o cenario, e nao os repositorios: montar o estado com o mesmo codigo que
 * esta sob teste torna o teste circular.
 */
class ExperienceIntegrationTest extends AbstractIntegrationTest {

  private static final String INSERIR =
      """
      INSERT INTO experience (company, role, start_date, end_date, description, highlights)
      VALUES (?, ?, ?::date, ?::date, ?, ?::jsonb)
      """;

  private final BasicJsonTester json = new BasicJsonTester(getClass());

  @Autowired LoadExperiencePort loadExperiencePort;

  @Autowired ListExperiencesUseCase listExperiencesUseCase;

  @Autowired DataSource dataSource;

  /**
   * Tabela vazia antes, seed de producao depois.
   *
   * <p>Desde que o {@code R__seed_experience} entrou, o container chega aqui com as duas posicoes
   * reais aplicadas pelo Flyway. Sem esvaziar antes, cada assercao abaixo mediria o conteudo
   * publicado em vez das linhas que o proprio teste escreve - e uma correcao de texto no seed
   * viraria build vermelho.
   */
  @BeforeEach
  void esvaziarATabela() {
    ExperienceFixtures.empty(jdbcTemplate);
  }

  @AfterEach
  void devolverOBancoAoSeed() {
    ExperienceFixtures.reapplySeed(dataSource);
  }

  /**
   * A acentuacao vai junto de proposito.
   *
   * <p>O caminho do {@code jsonb} passa por driver, serializador e banco, e cada um deles tem uma
   * chance de trocar a codificacao. O perfil ja tem uma assercao assim pelo mesmo motivo.
   */
  @Test
  @DisplayName("deve ler os destaques do jsonb como lista, com acentuacao intacta")
  void shouldReadHighlightsFromJsonb_whenRowHasThem() {
    // given
    inserir(
        "Acme",
        "Dev Backend",
        "2022-03-01",
        null,
        "[\"Reduziu a latência em 40%\", \"Migrou o deploy para contêiner\"]");

    // when
    var experiences = loadExperiencePort.loadExperiences();

    // then
    assertThat(experiences).hasSize(1);
    assertThat(experiences.getFirst().highlights())
        .containsExactly("Reduziu a latência em 40%", "Migrou o deploy para contêiner");
  }

  @Test
  @DisplayName("deve ler destaques vazios como lista vazia, e nao como nulo")
  void shouldReadEmptyHighlightsAsEmptyList_whenColumnHasDefault() {
    // given
    jdbcTemplate.update(
        """
        INSERT INTO experience (company, role, start_date, description)
        VALUES ('Acme', 'Dev', '2022-03-01'::date, 'Descricao')
        """);

    // when
    var experiences = loadExperiencePort.loadExperiences();

    // then
    assertThat(experiences.getFirst().highlights()).isEmpty();
  }

  @Test
  @DisplayName("deve traduzir data de saida nula em cargo atual")
  void shouldReadNullEndDateAsCurrent_whenPositionIsOngoing() {
    // given
    inserir("Acme", "Dev", "2024-01-01", null, "[]");
    inserir("Antiga", "Dev", "2018-01-01", "2020-12-31", "[]");

    // when
    var timeline = listExperiencesUseCase.listExperiences();

    // then
    assertThat(timeline.findCurrent()).map(Experience::company).contains("Acme");
    assertThat(timeline.experiences().getLast().findEndDate()).contains(LocalDate.of(2020, 12, 31));
  }

  /**
   * A ordem correta com a consulta real no caminho.
   *
   * <p>As linhas entram fora de ordem, e a insercao no Postgres nao promete ordem de leitura. O que
   * este teste afirma e o resultado; <strong>qual das duas pecas o produz nao esta em disputa
   * aqui</strong>, e essa distincao e o que a secao 4.20 registrou: quem garante e o dominio, e o
   * {@code ORDER BY} do repositorio existe para o indice evitar o sort. Remover a clausula nao deve
   * mudar esta assercao - se mudar, a garantia esta no lugar errado.
   */
  @Test
  @DisplayName("deve devolver a timeline em ordem decrescente, com a consulta real no caminho")
  void shouldReturnOrderedTimeline_whenRowsAreInsertedOutOfOrder() {
    // given
    inserir("Meio", "Dev", "2021-06-01", "2023-05-31", "[]");
    inserir("Antiga", "Dev", "2018-01-01", "2020-12-31", "[]");
    inserir("Recente", "Dev", "2024-02-01", null, "[]");

    // when
    var timeline = listExperiencesUseCase.listExperiences();

    // then
    assertThat(timeline.experiences())
        .extracting(Experience::company)
        .containsExactly("Recente", "Meio", "Antiga");
  }

  @Test
  @DisplayName("deve devolver timeline vazia quando a tabela esta vazia")
  void shouldReturnEmptyTimeline_whenTableIsEmpty() {
    // when
    var timeline = listExperiencesUseCase.listExperiences();

    // then
    assertThat(timeline.isEmpty()).isTrue();
  }

  /**
   * O endpoint inteiro, por HTTP de verdade e com o banco no caminho.
   *
   * <p>A fatia {@code @WebMvcTest} usa duble de caso de uso e simula o container servlet, entao ela
   * nao alcanca nem a consulta nem a codificacao dos bytes na rede. Aqui a requisicao sai pela rede
   * local, atravessa Tomcat, filtro de chave, controlador, caso de uso, adaptador, Hibernate,
   * driver e Postgres, e volta.
   *
   * <p>A resposta e lida como bytes e decodificada em UTF-8 a mao, pelo mesmo motivo do perfil:
   * assim o que se afirma e que os bytes na rede sao UTF-8, em vez de deixar o cliente adivinhar o
   * charset e esconder justamente o defeito procurado.
   */
  @Test
  @DisplayName("deve servir a timeline por http, com acentuacao e ordem intactas")
  void shouldServeTimelineOverHttp_whenRowsExist() {
    // given
    inserir("Empresa Antiga", "Desenvolvedor Junior", "2019-02-01", "2021-07-31", "[]");
    inserir(
        "Empresa Atual",
        "Desenvolvedor Backend",
        "2022-08-01",
        null,
        "[\"Migrou o deploy para contêiner\"]");

    // when
    var response = getComChave("/api/v1/experiences", byte[].class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getCacheControl())
        .isEqualTo("max-age=300, public, stale-while-revalidate=3600");

    var corpo = new String(response.getBody(), StandardCharsets.UTF_8);
    assertThat(json.from(corpo))
        .extractingJsonPathStringValue("$[0].company")
        .isEqualTo("Empresa Atual");
    assertThat(json.from(corpo))
        .extractingJsonPathStringValue("$[1].company")
        .isEqualTo("Empresa Antiga");
    assertThat(json.from(corpo))
        .extractingJsonPathStringValue("$[0].highlights[0]")
        .isEqualTo("Migrou o deploy para contêiner");

    // Nulo explicito, e nao chave omitida - ver ExperienceControllerTest.
    assertThat(corpo).contains("\"endDate\":null");
  }

  @Test
  @DisplayName("deve responder 200 com array vazio, e nao 404, quando nao ha passagens")
  void shouldRespondEmptyArray_whenThereAreNoRows() {
    // when
    var response = getComChave("/api/v1/experiences", String.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("[]");
  }

  /** O filtro de chave cobre {@code /api/*} por prefixo, entao o endpoint novo entra protegido. */
  @Test
  @DisplayName("deve recusar a timeline sem a chave de servico")
  void shouldReject_whenServiceKeyIsMissing() {
    // when
    var response = restTemplate.getForEntity("/api/v1/experiences", String.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  /**
   * A idempotencia que o {@code R__seed_experience.sql} promete no proprio cabecalho.
   *
   * <p>Nenhum outro teste cobre isso, porque o Flyway so reexecuta a repetivel quando o checksum do
   * arquivo muda - em condicoes normais ela roda uma vez e nunca mais. Se o arquivo deixasse de ser
   * idempotente, o efeito apareceria em producao, no deploy seguinte a uma correcao de texto.
   *
   * <p><strong>O snapshot inclui os ids de proposito.</strong> E o que distingue upsert de {@code
   * DELETE} seguido de {@code INSERT}: os dois deixam o mesmo conteudo, mas o segundo troca as
   * chaves a cada execucao. Aqui isso ainda nao quebraria nada, porque nenhuma tabela referencia
   * {@code experience} - a assercao esta posta para o dia em que alguma referenciar.
   *
   * <p>{@code created_at} e {@code updated_at} ficam fora do snapshot, e nao por conveniencia: o
   * seed grava {@code now()} no {@code ON CONFLICT}, entao {@code updated_at} muda de propria
   * vontade a cada execucao. Compara-lo afirmaria que o seed nao roda, e nao que ele e idempotente.
   */
  @Test
  @DisplayName("o seed e idempotente: rodar duas vezes deixa o banco identico, ids inclusive")
  void shouldBeIdempotent_whenSeedRunsTwice() {
    // given
    ExperienceFixtures.reapplySeed(dataSource);
    List<Map<String, Object>> depoisDaPrimeira = snapshot();

    // when
    ExperienceFixtures.reapplySeed(dataSource);

    // then
    assertThat(snapshot()).isEqualTo(depoisDaPrimeira);
  }

  /**
   * A lista do seed e fonte de verdade nos dois sentidos.
   *
   * <p>Sem o {@code DELETE} da CTE, o seed saberia acrescentar e corrigir mas nunca tirar - e
   * despublicar uma posicao, que e o caso em que mais importa, deixaria de funcionar em silencio.
   */
  @Test
  @DisplayName("o seed remove passagem que nao esta mais na lista")
  void shouldRemoveExperience_whenItIsNoLongerInTheSeedList() {
    // given
    inserir("Empresa Que Nao Existe", "Cargo Inventado", "2010-01-01", null, "[]");

    // when
    ExperienceFixtures.reapplySeed(dataSource);

    // then
    assertThat(listarEmpresas()).doesNotContain("Empresa Que Nao Existe").hasSize(2);
  }

  private List<Map<String, Object>> snapshot() {
    return jdbcTemplate.queryForList(
        """
        SELECT id, company, role, start_date, end_date, description, highlights::text
        FROM experience
        ORDER BY id
        """);
  }

  private List<String> listarEmpresas() {
    return jdbcTemplate.queryForList("SELECT company FROM experience", String.class);
  }

  private void inserir(
      String company, String role, String start, String end, String highlightsJson) {
    jdbcTemplate.update(INSERIR, company, role, start, end, "Descricao", highlightsJson);
  }
}
