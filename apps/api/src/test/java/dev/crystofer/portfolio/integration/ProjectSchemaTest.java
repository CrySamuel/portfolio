package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import dev.crystofer.portfolio.support.fixtures.ProjectFixtures;

/**
 * As promessas da {@code V4__create_project_tables} contra o Postgres 16 de verdade.
 *
 * <p>Existe pela mesma razao do {@link ExperienceSchemaTest} e do {@link SkillSchemaTest}: ate o
 * commit 35 nao ha entidade nem adaptador que tente gravar errado, entao remover um {@code CHECK}
 * daqui deixaria a suite inteira verde e o defeito so apareceria como dado impossivel em producao.
 *
 * <p>Cada assercao exige o nome da restricao. Um {@code INSERT} pode ser recusado por muitos
 * motivos, e teste que aceita qualquer falha como prova passa pelo motivo errado.
 *
 * <p>As tabelas chegam a este teste com os dois projetos reais, porque o Flyway aplica o {@code
 * R__seed_projects} no container. Por isso o {@code @BeforeEach} esvazia e o {@code @AfterEach}
 * restaura - as contagens abaixo medem as linhas que o proprio teste escreve, e nao o conteudo
 * publicado.
 */
class ProjectSchemaTest extends AbstractIntegrationTest {

  @Autowired DataSource dataSource;

  private static final String INSERIR_PROJETO =
      """
      INSERT INTO project (slug, title, summary, problem, solution, outcome, repo_url, live_url)
      VALUES (?, ?, 'Resumo do card.', 'O problema.', 'A solucao.', 'O resultado.', ?, ?)
      RETURNING id
      """;

  /**
   * As quatro tabelas, na ordem que a integridade permite.
   *
   * <p>{@code project} primeiro: o {@code ON DELETE CASCADE} leva junto os vinculos e as metricas.
   * Se {@code technology} fosse apagada antes, o {@code RESTRICT} do vinculo recusaria - e a recusa
   * e exatamente o que um dos testes abaixo verifica.
   */
  @BeforeEach
  void partirDeTabelasVazias() {
    ProjectFixtures.empty(jdbcTemplate);
  }

  @AfterEach
  void devolverOBancoAoSeed() {
    ProjectFixtures.reapplySeed(dataSource);
  }

  @Test
  @DisplayName("deve aceitar projeto com narrativa completa")
  void shouldAcceptProject_whenEverythingIsValid() {
    // when
    var thrown =
        catchThrowable(
            () ->
                inserirProjeto(
                    "music-style-api", "Music Style API", "https://github.com/x/y", null));

    // then
    assertThat(thrown).isNull();
    assertThat(contar("project")).isEqualTo(1);
  }

  @Test
  @DisplayName("deve recusar dois projetos com o mesmo slug")
  void shouldRejectProject_whenSlugRepeats() {
    // given
    inserirProjeto("finai", "FinAI", null, null);

    // when
    var thrown = catchThrowable(() -> inserirProjeto("finai", "FinAI v2", null, null));

    // then
    assertThat(thrown).hasMessageContaining("project_slug_uk");
  }

  /**
   * O que o formato recusa, e por que cada caso importa.
   *
   * <p>{@code Music Style API} e o titulo copiado sem transformar. O espaco e a maiuscula viram
   * escape na URL e a rota deixa de casar. {@code music_style} e o erro de quem usou a convencao do
   * banco de dados numa URL, que a secao 3.8 fixa em kebab-case. {@code music--style} e {@code
   * music-} sao o resultado de gerar slug por substituicao ingenua, e produzem duas URLs diferentes
   * para o mesmo projeto. Vazio e o campo esquecido.
   */
  @ParameterizedTest
  @DisplayName("deve recusar slug fora do formato da URL")
  @ValueSource(strings = {"Music Style API", "music_style", "music--style", "music-", "-music", ""})
  void shouldRejectProject_whenSlugIsMalformed(String invalido) {
    // when
    var thrown = catchThrowable(() -> inserirProjeto(invalido, "Titulo", null, null));

    // then
    assertThat(thrown).hasMessageContaining("project_slug_format_ck");
  }

  @ParameterizedTest
  @DisplayName("deve aceitar slug em kebab-case")
  @ValueSource(strings = {"finai", "music-style-api", "api-v2", "projeto-com-tres-partes"})
  void shouldAcceptProject_whenSlugIsKebabCase(String valido) {
    // when
    var thrown = catchThrowable(() -> inserirProjeto(valido, "Titulo", null, null));

    // then
    assertThat(thrown).isNull();
  }

  @Test
  @DisplayName("deve recusar repositorio sem esquema https")
  void shouldRejectProject_whenRepoUrlHasNoScheme() {
    // when
    var thrown =
        catchThrowable(() -> inserirProjeto("finai", "FinAI", "github.com/CrySamuel/finai", null));

    // then
    assertThat(thrown).hasMessageContaining("project_repo_url_ck");
  }

  @Test
  @DisplayName("deve recusar site sem esquema https")
  void shouldRejectProject_whenLiveUrlHasNoScheme() {
    // when
    var thrown = catchThrowable(() -> inserirProjeto("finai", "FinAI", null, "http://finai.dev"));

    // then
    assertThat(thrown).hasMessageContaining("project_live_url_ck");
  }

  /**
   * Projeto sem nenhum endereco publico e aceito, e isso e uma decisao.
   *
   * <p>Trabalho sob acordo de confidencialidade nao tem repositorio para mostrar. Recusa-lo aqui
   * seria o schema decidindo uma politica de conteudo que ainda nao foi tomada - e desfazer um
   * {@code CHECK} de migracao ja aplicada custa outra migracao.
   */
  @Test
  @DisplayName("deve aceitar projeto sem repositorio e sem site")
  void shouldAcceptProject_whenBothUrlsAreAbsent() {
    // when
    var thrown = catchThrowable(() -> inserirProjeto("interno", "Projeto interno", null, null));

    // then
    assertThat(thrown).isNull();
  }

  /** A identidade e sempre gerada - ver {@link ExperienceSchemaTest}. */
  @Test
  @DisplayName("deve recusar id explicito no projeto")
  void shouldRejectExplicitId_whenIdentityIsGeneratedAlways() {
    // when
    var thrown =
        catchThrowable(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO project (id, slug, title, summary, problem, solution, outcome)
                    VALUES (7, 'finai', 'FinAI', 'r', 'p', 's', 'o')
                    """));

    // then
    assertThat(thrown)
        .rootCause()
        .hasMessageContaining("cannot insert a non-DEFAULT value into column");
  }

  @Test
  @DisplayName("deve recusar tecnologia com nome repetido")
  void shouldRejectTechnology_whenNameRepeats() {
    // given
    criarTecnologia("Java", "java", "language");

    // when
    var thrown = catchThrowable(() -> criarTecnologia("Java", "java-21", "language"));

    // then
    assertThat(thrown).hasMessageContaining("technology_name_uk");
  }

  @Test
  @DisplayName("deve recusar tecnologia com slug repetido")
  void shouldRejectTechnology_whenSlugRepeats() {
    // given
    criarTecnologia("Java", "java", "language");

    // when
    var thrown = catchThrowable(() -> criarTecnologia("Java 21", "java", "language"));

    // then
    assertThat(thrown).hasMessageContaining("technology_slug_uk");
  }

  @ParameterizedTest
  @DisplayName("deve recusar slug de tecnologia fora do formato da query string")
  @ValueSource(strings = {"Spring Boot", "spring_boot", "spring--boot", "boot-", ""})
  void shouldRejectTechnology_whenSlugIsMalformed(String invalido) {
    // when
    var thrown = catchThrowable(() -> criarTecnologia("Spring Boot", invalido, "framework"));

    // then
    assertThat(thrown).hasMessageContaining("technology_slug_format_ck");
  }

  @ParameterizedTest
  @DisplayName("deve aceitar as cinco categorias previstas")
  @ValueSource(strings = {"language", "framework", "database", "infrastructure", "tool"})
  void shouldAcceptCategory_whenValueIsOneOfTheFive(String categoria) {
    // when
    var thrown = catchThrowable(() -> criarTecnologia(categoria, categoria, categoria));

    // then
    assertThat(thrown).isNull();
  }

  /**
   * {@code Backend} e o erro de quem escreveu para a tela em vez de para o filtro; {@code library}
   * e a categoria plausivel que a lista nao tem; {@code Framework} e a mesma categoria com a
   * capitalizacao errada, que sem o {@code CHECK} viraria um segundo grupo no filtro.
   */
  @ParameterizedTest
  @DisplayName("deve recusar categoria fora da lista")
  @ValueSource(strings = {"Backend", "library", "Framework", "linguagem"})
  void shouldRejectCategory_whenValueIsOutsideTheList(String invalida) {
    // when
    var thrown = catchThrowable(() -> criarTecnologia("Alguma", "alguma", invalida));

    // then
    assertThat(thrown).hasMessageContaining("technology_category_ck");
  }

  @Test
  @DisplayName("deve recusar a mesma tecnologia duas vezes no mesmo projeto")
  void shouldRejectLink_whenPairRepeats() {
    // given
    long projeto = inserirProjeto("finai", "FinAI", null, null);
    long java = criarTecnologia("Java", "java", "language");
    vincular(projeto, java);

    // when
    var thrown = catchThrowable(() -> vincular(projeto, java));

    // then
    assertThat(thrown).hasMessageContaining("project_tech_pk");
  }

  @Test
  @DisplayName("deve apagar os vinculos junto com o projeto")
  void shouldCascade_whenProjectIsDeleted() {
    // given
    long projeto = inserirProjeto("finai", "FinAI", null, null);
    long java = criarTecnologia("Java", "java", "language");
    long docker = criarTecnologia("Docker", "docker", "infrastructure");
    vincular(projeto, java);
    vincular(projeto, docker);

    // when
    jdbcTemplate.update("DELETE FROM project WHERE id = ?", projeto);

    // then
    assertThat(contar("project_tech")).isZero();
    assertThat(contar("technology")).isEqualTo(2);
  }

  /**
   * A assimetria do vinculo, exercitada em vez de suposta.
   *
   * <p>Se o {@code RESTRICT} virasse {@code CASCADE} numa migracao futura, remover uma tecnologia
   * do catalogo apagaria em silencio o chip dela de todos os projetos - e a perda apareceria na
   * tela, sem nada no log.
   */
  @Test
  @DisplayName("deve recusar apagar tecnologia ainda vinculada a um projeto")
  void shouldRestrict_whenTechnologyIsStillLinked() {
    // given
    long projeto = inserirProjeto("finai", "FinAI", null, null);
    long java = criarTecnologia("Java", "java", "language");
    vincular(projeto, java);

    // when
    var thrown =
        catchThrowable(() -> jdbcTemplate.update("DELETE FROM technology WHERE id = ?", java));

    // then
    assertThat(thrown).hasMessageContaining("project_tech_technology_id_fkey");
    assertThat(contar("project_tech")).isEqualTo(1);
  }

  @Test
  @DisplayName("deve recusar duas metricas com o mesmo rotulo no mesmo projeto")
  void shouldRejectMetric_whenLabelRepeatsInTheSameProject() {
    // given
    long projeto = inserirProjeto("finai", "FinAI", null, null);
    criarMetrica(projeto, "p95", "80ms");

    // when
    var thrown = catchThrowable(() -> criarMetrica(projeto, "p95", "120ms"));

    // then
    assertThat(thrown).hasMessageContaining("project_metric_project_id_label_uk");
  }

  /** O mesmo rotulo em projetos diferentes e o caso normal: quase todo projeto tem um p95. */
  @Test
  @DisplayName("deve aceitar o mesmo rotulo de metrica em projetos diferentes")
  void shouldAcceptMetric_whenSameLabelLivesInAnotherProject() {
    // given
    long finai = inserirProjeto("finai", "FinAI", null, null);
    long music = inserirProjeto("music-style-api", "Music Style API", null, null);
    criarMetrica(finai, "p95", "80ms");

    // when
    var thrown = catchThrowable(() -> criarMetrica(music, "p95", "45ms"));

    // then
    assertThat(thrown).isNull();
    assertThat(contar("project_metric")).isEqualTo(2);
  }

  @Test
  @DisplayName("deve apagar as metricas junto com o projeto")
  void shouldCascadeMetrics_whenProjectIsDeleted() {
    // given
    long projeto = inserirProjeto("finai", "FinAI", null, null);
    criarMetrica(projeto, "p95", "80ms");
    criarMetrica(projeto, "cobertura", "84%");

    // when
    jdbcTemplate.update("DELETE FROM project WHERE id = ?", projeto);

    // then
    assertThat(contar("project_metric")).isZero();
  }

  @Test
  @DisplayName("deve recusar metrica sem projeto existente")
  void shouldRejectMetric_whenProjectDoesNotExist() {
    // when
    var thrown = catchThrowable(() -> criarMetrica(9_999L, "p95", "80ms"));

    // then
    assertThat(thrown).hasMessageContaining("project_metric_project_id_fkey");
  }

  private long inserirProjeto(String slug, String titulo, String repoUrl, String liveUrl) {
    Long id =
        jdbcTemplate.queryForObject(INSERIR_PROJETO, Long.class, slug, titulo, repoUrl, liveUrl);
    return id;
  }

  private long criarTecnologia(String nome, String slug, String categoria) {
    Long id =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO technology (name, slug, category) VALUES (?, ?, ?) RETURNING id
            """,
            Long.class,
            nome,
            slug,
            categoria);
    return id;
  }

  private void vincular(long projeto, long tecnologia) {
    jdbcTemplate.update(
        "INSERT INTO project_tech (project_id, technology_id) VALUES (?, ?)", projeto, tecnologia);
  }

  private void criarMetrica(long projeto, String rotulo, String valor) {
    jdbcTemplate.update(
        "INSERT INTO project_metric (project_id, label, value) VALUES (?, ?, ?)",
        projeto,
        rotulo,
        valor);
  }

  private Integer contar(String tabela) {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM " + tabela, Integer.class);
  }
}
