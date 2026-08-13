package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * As promessas da {@code V3__create_skill_tables} contra o Postgres 16 de verdade.
 *
 * <p>Existe pela mesma razao do {@link ExperienceSchemaTest}: ate o commit 30 nao ha entidade nem
 * adaptador que tente gravar errado, entao remover um {@code CHECK} daqui deixaria a suite inteira
 * verde. O defeito so apareceria como dado impossivel em producao.
 *
 * <p>Cada assercao exige o nome da restricao. Um {@code INSERT} pode ser recusado por muitos
 * motivos, e teste que aceita qualquer falha como prova passa pelo motivo errado.
 */
class SkillSchemaTest extends AbstractIntegrationTest {

  private static final String INSERIR_SKILL =
      """
      INSERT INTO skill (category_id, name, proficiency, years_of_experience)
      VALUES (?, ?, ?, ?)
      """;

  /**
   * As duas tabelas nascem vazias e assim devem terminar.
   *
   * <p>Nao ha seed a restaurar - o conteudo das skills depende do dono, que ainda nao informou os
   * niveis. Quando houver um {@code R__seed_skills}, este metodo vira o par esvaziar/restaurar que
   * {@code ExperienceFixtures} ja implementa.
   *
   * <p>Apagar so {@code skill_category}: as competencias vao junto pelo {@code ON DELETE CASCADE}.
   * E mais que limpeza - se o cascade sumir numa migracao futura, sobra linha orfa e a contagem
   * denuncia.
   */
  @AfterEach
  void limparAsTabelas() {
    jdbcTemplate.update("DELETE FROM skill_category");
  }

  @Test
  @DisplayName("deve aceitar competencia completa dentro de uma categoria")
  void shouldAcceptSkill_whenEverythingIsValid() {
    // given
    long categoria = criarCategoria("Linguagens", 0);

    // when
    var thrown = catchThrowable(() -> inserirSkill(categoria, "Java", "advanced", 3));

    // then
    assertThat(thrown).isNull();
    assertThat(contarSkills()).isEqualTo(1);
  }

  @ParameterizedTest
  @DisplayName("deve aceitar os tres niveis previstos")
  @ValueSource(strings = {"basic", "intermediate", "advanced"})
  void shouldAcceptProficiency_whenValueIsOneOfTheThree(String nivel) {
    // given
    long categoria = criarCategoria("Linguagens", 0);

    // when
    var thrown = catchThrowable(() -> inserirSkill(categoria, "Java " + nivel, nivel, null));

    // then
    assertThat(thrown).isNull();
  }

  /**
   * O que o {@code CHECK} recusa, e por que cada caso importa.
   *
   * <p>{@code expert} e o nivel plausivel que a escala nao tem - e o erro de quem supoe outra
   * escala. {@code ADVANCED} em maiuscula e o erro de quem gravou o nome da constante Java em vez
   * do codigo. {@code avancado} e o erro de quem traduziu. Vazio e o erro de quem esqueceu o campo.
   */
  @ParameterizedTest
  @DisplayName("deve recusar nivel fora da escala")
  @ValueSource(strings = {"expert", "ADVANCED", "avancado", "", "beginner"})
  void shouldRejectProficiency_whenValueIsOutsideTheScale(String invalido) {
    // given
    long categoria = criarCategoria("Linguagens", 0);

    // when
    var thrown = catchThrowable(() -> inserirSkill(categoria, "Java", invalido, null));

    // then
    assertThat(thrown).hasMessageContaining("skill_proficiency_ck");
  }

  @Test
  @DisplayName("deve recusar tempo de experiencia negativo")
  void shouldRejectYears_whenNegative() {
    // given
    long categoria = criarCategoria("Linguagens", 0);

    // when
    var thrown = catchThrowable(() -> inserirSkill(categoria, "Java", "advanced", -1));

    // then
    assertThat(thrown).hasMessageContaining("skill_years_ck");
  }

  /** O limite, e nao apenas um caso claramente valido: quem comecou agora tem zero. */
  @Test
  @DisplayName("deve aceitar zero ano de experiencia")
  void shouldAcceptYears_whenZero() {
    // given
    long categoria = criarCategoria("Linguagens", 0);

    // when
    var thrown = catchThrowable(() -> inserirSkill(categoria, "Rust", "basic", 0));

    // then
    assertThat(thrown).isNull();
  }

  @Test
  @DisplayName("deve aceitar competencia sem tempo declarado")
  void shouldAcceptYears_whenAbsent() {
    // given
    long categoria = criarCategoria("Linguagens", 0);

    // when
    var thrown = catchThrowable(() -> inserirSkill(categoria, "SQL", "intermediate", null));

    // then
    assertThat(thrown).isNull();
  }

  @Test
  @DisplayName("deve recusar a mesma competencia duas vezes na mesma categoria")
  void shouldRejectSkill_whenNameRepeatsInTheSameCategory() {
    // given
    long categoria = criarCategoria("Linguagens", 0);
    inserirSkill(categoria, "Java", "advanced", 3);

    // when
    var thrown = catchThrowable(() -> inserirSkill(categoria, "Java", "basic", 1));

    // then
    assertThat(thrown).hasMessageContaining("skill_category_id_name_uk");
  }

  /**
   * A mesma competencia em duas categorias e permitida, e isso e uma decisao.
   *
   * <p>"SQL" cabe em Bancos de Dados e em Linguagens. Uma chave unica so pelo nome forcaria o
   * schema a opinar sobre taxonomia, que e assunto de quem escreve o conteudo.
   */
  @Test
  @DisplayName("deve aceitar a mesma competencia em categorias diferentes")
  void shouldAcceptSkill_whenSameNameLivesInAnotherCategory() {
    // given
    long linguagens = criarCategoria("Linguagens", 0);
    long bancos = criarCategoria("Bancos de Dados", 1);
    inserirSkill(linguagens, "SQL", "intermediate", null);

    // when
    var thrown = catchThrowable(() -> inserirSkill(bancos, "SQL", "intermediate", null));

    // then
    assertThat(thrown).isNull();
    assertThat(contarSkills()).isEqualTo(2);
  }

  @Test
  @DisplayName("deve recusar categoria com nome repetido")
  void shouldRejectCategory_whenNameRepeats() {
    // given
    criarCategoria("Linguagens", 0);

    // when
    var thrown = catchThrowable(() -> criarCategoria("Linguagens", 1));

    // then
    assertThat(thrown).hasMessageContaining("skill_category_name_uk");
  }

  /**
   * O cascade, exercitado em vez de suposto.
   *
   * <p>A FK promete que competencia nao sobrevive a categoria. Sem esta assercao, trocar o {@code
   * ON DELETE CASCADE} por {@code RESTRICT} numa migracao futura passaria despercebido ate alguem
   * tentar remover uma categoria em producao.
   */
  @Test
  @DisplayName("deve apagar as competencias junto com a categoria")
  void shouldCascade_whenCategoryIsDeleted() {
    // given
    long categoria = criarCategoria("Linguagens", 0);
    inserirSkill(categoria, "Java", "advanced", 3);
    inserirSkill(categoria, "Python", "intermediate", 2);

    // when
    jdbcTemplate.update("DELETE FROM skill_category WHERE id = ?", categoria);

    // then
    assertThat(contarSkills()).isZero();
  }

  @Test
  @DisplayName("deve recusar competencia sem categoria existente")
  void shouldRejectSkill_whenCategoryDoesNotExist() {
    // when
    var thrown = catchThrowable(() -> inserirSkill(9_999L, "Java", "advanced", null));

    // then
    assertThat(thrown).hasMessageContaining("skill_category_id_fkey");
  }

  /** A identidade e sempre gerada nas duas tabelas - ver {@link ExperienceSchemaTest}. */
  @Test
  @DisplayName("deve recusar id explicito na categoria")
  void shouldRejectExplicitId_whenIdentityIsGeneratedAlways() {
    // when
    var thrown =
        catchThrowable(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO skill_category (id, name) VALUES (7, 'Linguagens')"));

    // then
    assertThat(thrown)
        .rootCause()
        .hasMessageContaining("cannot insert a non-DEFAULT value into column");
  }

  private long criarCategoria(String nome, int ordem) {
    Long id =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO skill_category (name, display_order) VALUES (?, ?) RETURNING id
            """,
            Long.class,
            nome,
            ordem);
    return id;
  }

  private void inserirSkill(long categoria, String nome, String nivel, Integer anos) {
    jdbcTemplate.update(INSERIR_SKILL, categoria, nome, nivel, anos);
  }

  private Integer contarSkills() {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM skill", Integer.class);
  }
}
