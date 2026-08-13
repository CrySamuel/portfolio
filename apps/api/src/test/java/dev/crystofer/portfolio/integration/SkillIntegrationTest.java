package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.http.HttpStatus;

import dev.crystofer.portfolio.profile.domain.model.Proficiency;
import dev.crystofer.portfolio.profile.domain.model.Skill;
import dev.crystofer.portfolio.profile.domain.model.SkillCategory;
import dev.crystofer.portfolio.profile.domain.port.in.ListSkillsUseCase;
import dev.crystofer.portfolio.support.fixtures.SkillFixtures;

/**
 * O caminho do banco ate o dominio, com Postgres de verdade.
 *
 * <p>Cobre o que nenhuma outra camada alcanca: a traducao do codigo de nivel na leitura real, o
 * agrupamento pela chave estrangeira e a ordenacao ponta a ponta com a consulta no meio.
 *
 * <p>SQL cru para preparar o cenario - montar o estado com o codigo sob teste torna o teste
 * circular.
 */
class SkillIntegrationTest extends AbstractIntegrationTest {

  private final BasicJsonTester json = new BasicJsonTester(getClass());

  @Autowired ListSkillsUseCase listSkillsUseCase;

  @Autowired DataSource dataSource;

  /**
   * Tabelas vazias antes, seed de producao depois.
   *
   * <p>Desde que o {@code R__seed_skills} entrou, o container chega aqui com as quatro categorias
   * reais. Sem esvaziar antes, cada assercao mediria o conteudo publicado - e um ajuste de nivel
   * feito pelo dono viraria build vermelho.
   */
  @BeforeEach
  void esvaziarAsTabelas() {
    SkillFixtures.empty(jdbcTemplate);
  }

  @AfterEach
  void devolverOBancoAoSeed() {
    SkillFixtures.reapplySeed(dataSource);
  }

  @Test
  @DisplayName("deve agrupar as competencias por categoria, na ordem editorial")
  void shouldGroupSkillsByCategory_whenRowsExist() {
    // given - categorias inseridas fora de ordem, de proposito
    long infra = criarCategoria("Infraestrutura", 2);
    long linguagens = criarCategoria("Linguagens", 0);
    inserirSkill(infra, "Docker", "intermediate", 2);
    inserirSkill(linguagens, "Java", "advanced", 3);
    inserirSkill(linguagens, "Python", "intermediate", null);

    // when
    var catalogo = listSkillsUseCase.listSkills();

    // then
    assertThat(catalogo.categories())
        .extracting(SkillCategory::name)
        .containsExactly("Linguagens", "Infraestrutura");
    assertThat(catalogo.totalSkills()).isEqualTo(3);
    assertThat(catalogo.categories().getFirst().skills())
        .extracting(Skill::name)
        .containsExactly("Java", "Python");
  }

  @Test
  @DisplayName("deve traduzir o codigo do banco para o enum de dominio")
  void shouldTranslateProficiencyCode_whenReadingFromDatabase() {
    // given
    long categoria = criarCategoria("Linguagens", 0);
    inserirSkill(categoria, "Java", "advanced", 3);

    // when
    var catalogo = listSkillsUseCase.listSkills();

    // then
    assertThat(catalogo.categories().getFirst().skills().getFirst().proficiency())
        .isEqualTo(Proficiency.ADVANCED);
  }

  @Test
  @DisplayName("deve ler tempo ausente como ausencia, e nao como zero")
  void shouldReadNullYearsAsAbsent() {
    // given
    long categoria = criarCategoria("Ferramentas", 0);
    inserirSkill(categoria, "Git", "advanced", null);

    // when
    var catalogo = listSkillsUseCase.listSkills();

    // then
    assertThat(catalogo.categories().getFirst().skills().getFirst().findYearsOfExperience())
        .isEmpty();
  }

  /** Cabecalho sem competencia abaixo nao chega a tela - a decisao mora no catalogo. */
  @Test
  @DisplayName("deve descartar categoria sem competencias")
  void shouldDropEmptyCategory_whenItHasNoSkills() {
    // given
    long comSkills = criarCategoria("Linguagens", 0);
    criarCategoria("Categoria Vazia", 1);
    inserirSkill(comSkills, "Java", "advanced", 3);

    // when
    var catalogo = listSkillsUseCase.listSkills();

    // then
    assertThat(catalogo.categories()).extracting(SkillCategory::name).containsExactly("Linguagens");
  }

  @Test
  @DisplayName("deve devolver catalogo vazio quando as tabelas estao vazias")
  void shouldReturnEmptyCatalog_whenTablesAreEmpty() {
    // when
    var catalogo = listSkillsUseCase.listSkills();

    // then
    assertThat(catalogo.isEmpty()).isTrue();
  }

  /**
   * Acentuacao ponta a ponta, pelo mesmo motivo do perfil e da timeline.
   *
   * <p>Driver, Hibernate e banco tem uma chance cada de trocar a codificacao, e mojibake nao quebra
   * nada - so aparece na tela do visitante.
   */
  @Test
  @DisplayName("deve preservar a acentuacao dos nomes")
  void shouldPreserveAccents_whenReadingNames() {
    // given
    long categoria = criarCategoria("Infraestrutura & Versionamento", 0);
    inserirSkill(categoria, "Integração Contínua", "intermediate", null);

    // when
    var catalogo = listSkillsUseCase.listSkills();

    // then
    assertThat(catalogo.categories().getFirst().name()).isEqualTo("Infraestrutura & Versionamento");
    assertThat(catalogo.categories().getFirst().skills().getFirst().name())
        .isEqualTo("Integração Contínua");
  }

  /**
   * O endpoint inteiro, por HTTP de verdade e com o banco no caminho.
   *
   * <p>A fatia {@code @WebMvcTest} usa duble de caso de uso e simula o container servlet, entao nao
   * alcanca nem a consulta nem a codificacao dos bytes na rede.
   */
  @Test
  @DisplayName("deve servir as competencias agrupadas por http, com acentuacao intacta")
  void shouldServeGroupedSkillsOverHttp_whenRowsExist() {
    // given
    long infra = criarCategoria("Infraestrutura", 1);
    long linguagens = criarCategoria("Linguagens", 0);
    inserirSkill(infra, "Integração Contínua", "intermediate", null);
    inserirSkill(linguagens, "Java", "advanced", 3);

    // when
    var response = getComChave("/api/v1/skills", byte[].class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getCacheControl())
        .isEqualTo("max-age=300, public, stale-while-revalidate=3600");

    var corpo = new String(response.getBody(), StandardCharsets.UTF_8);
    assertThat(json.from(corpo)).extractingJsonPathStringValue("$[0].name").isEqualTo("Linguagens");
    assertThat(json.from(corpo))
        .extractingJsonPathStringValue("$[0].skills[0].proficiency")
        .isEqualTo("advanced");
    assertThat(json.from(corpo))
        .extractingJsonPathStringValue("$[1].skills[0].name")
        .isEqualTo("Integração Contínua");

    // Nulo explicito, e nao chave omitida - ver SkillControllerTest.
    assertThat(corpo).contains("\"yearsOfExperience\":null");
  }

  @Test
  @DisplayName("deve responder 200 com array vazio, e nao 404, quando nao ha competencias")
  void shouldRespondEmptyArray_whenThereAreNoRows() {
    // when
    var response = getComChave("/api/v1/skills", String.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("[]");
  }

  /** O filtro de chave cobre {@code /api/*} por prefixo, entao o endpoint novo nasce protegido. */
  @Test
  @DisplayName("deve recusar as competencias sem a chave de servico")
  void shouldReject_whenServiceKeyIsMissing() {
    // when
    var response = restTemplate.getForEntity("/api/v1/skills", String.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  /**
   * A idempotencia que o {@code R__seed_skills.sql} promete no proprio cabecalho.
   *
   * <p>O Flyway so reexecuta a repetivel quando o checksum do arquivo muda, entao em condicoes
   * normais ela roda uma vez e nunca mais. Se deixasse de ser idempotente, o efeito apareceria em
   * producao, no deploy seguinte a um ajuste de nivel.
   *
   * <p>O snapshot inclui os ids: e o que distingue upsert de {@code DELETE} seguido de {@code
   * INSERT}. Aqui isso importa de verdade, e nao so por principio - {@code skill} referencia {@code
   * skill_category} por chave estrangeira, entao recriar as categorias arrastaria as competencias
   * pelo cascade a cada execucao.
   */
  @Test
  @DisplayName("o seed e idempotente: rodar duas vezes deixa o banco identico, ids inclusive")
  void shouldBeIdempotent_whenSeedRunsTwice() {
    // given
    SkillFixtures.reapplySeed(dataSource);
    var depoisDaPrimeira = snapshot();

    // when
    SkillFixtures.reapplySeed(dataSource);

    // then
    assertThat(snapshot()).isEqualTo(depoisDaPrimeira);
  }

  /**
   * A lista do seed e fonte de verdade nos dois sentidos.
   *
   * <p>Sem os {@code DELETE} das CTEs, o seed saberia acrescentar e corrigir mas nunca tirar - e
   * despublicar uma competencia, que e o caso em que mais importa, deixaria de funcionar em
   * silencio.
   */
  @Test
  @DisplayName("o seed remove competencia e categoria que nao estao mais na lista")
  void shouldRemoveWhatIsNoLongerInTheSeedList() {
    // given
    SkillFixtures.reapplySeed(dataSource);
    long inventada = criarCategoria("Categoria Inventada", 9);
    inserirSkill(inventada, "Competencia Inventada", "advanced", null);
    long real =
        jdbcTemplate.queryForObject(
            "SELECT id FROM skill_category WHERE name = 'Bancos de Dados'", Long.class);
    inserirSkill(real, "Competencia Intrusa", "advanced", null);

    // when
    SkillFixtures.reapplySeed(dataSource);

    // then
    assertThat(listarNomes("SELECT name FROM skill_category"))
        .doesNotContain("Categoria Inventada");
    assertThat(listarNomes("SELECT name FROM skill"))
        .doesNotContain("Competencia Inventada", "Competencia Intrusa");
  }

  private java.util.List<java.util.Map<String, Object>> snapshot() {
    return jdbcTemplate.queryForList(
        """
        SELECT c.id, c.name, c.display_order, s.id, s.name, s.proficiency, s.years_of_experience
        FROM skill_category c
        LEFT JOIN skill s ON s.category_id = c.id
        ORDER BY c.id, s.id
        """);
  }

  private java.util.List<String> listarNomes(String sql) {
    return jdbcTemplate.queryForList(sql, String.class);
  }

  private long criarCategoria(String nome, int ordem) {
    Long id =
        jdbcTemplate.queryForObject(
            "INSERT INTO skill_category (name, display_order) VALUES (?, ?) RETURNING id",
            Long.class,
            nome,
            ordem);
    return id;
  }

  private void inserirSkill(long categoria, String nome, String nivel, Integer anos) {
    jdbcTemplate.update(
        """
        INSERT INTO skill (category_id, name, proficiency, years_of_experience)
        VALUES (?, ?, ?, ?)
        """,
        categoria,
        nome,
        nivel,
        anos);
  }
}
