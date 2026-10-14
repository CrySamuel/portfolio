package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import dev.crystofer.portfolio.support.fixtures.ExperienceFixtures;
import dev.crystofer.portfolio.support.fixtures.ProfileFixtures;
import dev.crystofer.portfolio.support.fixtures.ProjectFixtures;
import dev.crystofer.portfolio.support.fixtures.SkillFixtures;

/**
 * As promessas da {@code V6__create_translation_tables} contra o Postgres 16 de verdade.
 *
 * <p>Existe pela razao de sempre: ate o commit que le estas tabelas, nada na aplicacao grava nelas,
 * entao remover um {@code CHECK} daqui deixaria a suite inteira verde. E as regras sao as mesmas
 * nas seis tabelas, o que torna o esquecimento numa delas o defeito mais provavel - por isso cada
 * regra e provada contra todas, a partir de um {@link Tabela} que descreve cada uma.
 *
 * <p>Os pais sao as linhas do seed, e nao linhas criadas aqui: o perfil e unico por construcao, e
 * nao ha como criar um segundo so para o teste. O teste de cascata apaga o pai de verdade, e o
 * {@code @AfterEach} devolve os quatro seeds.
 *
 * <p>Cada recusa exige o nome da restricao, como nos demais testes de schema: um {@code INSERT}
 * pode falhar por muitos motivos, e teste que aceita qualquer falha passa pelo motivo errado.
 */
class TranslationSchemaTest extends AbstractIntegrationTest {

  /** As seis tabelas, cada uma com o que o teste precisa para escrever nela e no pai. */
  enum Tabela {
    PROFILE(
        "profile_translation",
        "profile",
        """
        INSERT INTO profile_translation (profile_id, locale, headline, bio, location)
        VALUES (?, ?, ?, 'Bio.', NULL)
        """),
    EXPERIENCE(
        "experience_translation",
        "experience",
        """
        INSERT INTO experience_translation (experience_id, locale, role, description, highlights)
        VALUES (?, ?, ?, 'Description.', '["Highlight"]'::jsonb)
        """),
    SKILL_CATEGORY(
        "skill_category_translation",
        "skill_category",
        "INSERT INTO skill_category_translation (category_id, locale, name) VALUES (?, ?, ?)"),
    SKILL(
        "skill_translation",
        "skill",
        "INSERT INTO skill_translation (skill_id, locale, name) VALUES (?, ?, ?)"),
    PROJECT(
        "project_translation",
        "project",
        """
        INSERT INTO project_translation (project_id, locale, title, summary, problem, solution,
                                         outcome)
        VALUES (?, ?, ?, 'Summary.', 'Problem.', 'Solution.', 'Outcome.')
        """),
    PROJECT_METRIC(
        "project_metric_translation",
        "project_metric",
        "INSERT INTO project_metric_translation (metric_id, locale, label, value) VALUES (?, ?, ?, '24/7')");

    final String nome;
    final String pai;

    /**
     * Insere uma linha valida com tres parametros: o pai, o idioma e o primeiro texto obrigatorio
     * da tabela. Passar nulo no terceiro e como o teste de {@code NOT NULL} se escreve uma vez so.
     */
    final String inserir;

    Tabela(String nome, String pai, String inserir) {
      this.nome = nome;
      this.pai = pai;
      this.inserir = inserir;
    }
  }

  @Autowired DataSource dataSource;

  /**
   * Comeca sem traducao nenhuma. Hoje as tabelas nascem vazias; quando os seeds passarem a trazer o
   * ingles, sem isto as contagens e as chaves duplicadas abaixo mediriam o seed.
   */
  @BeforeEach
  void esvaziarAsTraducoes() {
    for (Tabela tabela : Tabela.values()) {
      jdbcTemplate.update("DELETE FROM " + tabela.nome);
    }
  }

  @AfterEach
  void devolverOBancoAosSeeds() {
    ProfileFixtures.reapplySeed(dataSource);
    ExperienceFixtures.reapplySeed(dataSource);
    SkillFixtures.reapplySeed(dataSource);
    ProjectFixtures.reapplySeed(dataSource);
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(Tabela.class)
  @DisplayName("deve aceitar traducao completa em en-US")
  void shouldAcceptTranslation_whenLocaleIsEnUs(Tabela tabela) {
    // when
    var thrown = catchThrowable(() -> inserir(tabela, paiDe(tabela), "en-US", "Text"));

    // then
    assertThat(thrown).isNull();
    assertThat(contar(tabela)).isEqualTo(1);
  }

  /**
   * Os idiomas que o {@code CHECK} recusa, e o que cada um representa.
   *
   * <p>{@code pt-BR} e o caso que mais importa: e o idioma das tabelas originais, e uma linha dele
   * aqui seria uma segunda fonte para o mesmo texto. {@code en} e a tag sem regiao, que o cabecalho
   * {@code Accept-Language} aceita e o banco nao. {@code EN-US} e a caixa errada, que um {@code
   * CHECK} com {@code ILIKE} deixaria passar. {@code fr-FR} e o idioma que ninguem decidiu
   * acrescentar.
   */
  @ParameterizedTest(name = "{0} com {1}")
  @MethodSource("tabelasComIdiomaInvalido")
  @DisplayName("deve recusar idioma que nao e uma traducao prevista")
  void shouldRejectLocale_whenItIsNotATranslation(Tabela tabela, String idioma) {
    // when
    var thrown = catchThrowable(() -> inserir(tabela, paiDe(tabela), idioma, "Text"));

    // then
    assertThat(thrown).hasMessageContaining(tabela.nome + "_locale_ck");
  }

  static Stream<Arguments> tabelasComIdiomaInvalido() {
    return Stream.of(Tabela.values())
        .flatMap(
            tabela ->
                Stream.of("pt-BR", "en", "EN-US", "fr-FR")
                    .map(idioma -> Arguments.of(tabela, idioma)));
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(Tabela.class)
  @DisplayName("deve recusar duas traducoes da mesma linha para o mesmo idioma")
  void shouldRejectTranslation_whenSameRowAndLocaleRepeat(Tabela tabela) {
    // given
    long pai = paiDe(tabela);
    inserir(tabela, pai, "en-US", "First");

    // when
    var thrown = catchThrowable(() -> inserir(tabela, pai, "en-US", "Second"));

    // then
    assertThat(thrown).hasMessageContaining(tabela.nome + "_pk");
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(Tabela.class)
  @DisplayName("deve recusar traducao sem o texto principal")
  void shouldRejectTranslation_whenMainTextIsMissing(Tabela tabela) {
    // when
    var thrown = catchThrowable(() -> inserir(tabela, paiDe(tabela), "en-US", null));

    // then
    assertThat(thrown).hasMessageContaining("violates not-null constraint");
  }

  /**
   * Traducao nao existe sem o original.
   *
   * <p>Apaga o pai de verdade - uma linha do seed -, e e por isso que o {@code @AfterEach} devolve
   * os seeds em vez de apagar o que o teste escreveu.
   */
  @ParameterizedTest(name = "{0}")
  @EnumSource(Tabela.class)
  @DisplayName("deve apagar a traducao junto com o original")
  void shouldCascade_whenOriginalIsDeleted(Tabela tabela) {
    // given
    long pai = paiDe(tabela);
    inserir(tabela, pai, "en-US", "Text");

    // when
    jdbcTemplate.update("DELETE FROM " + tabela.pai + " WHERE id = ?", pai);

    // then
    assertThat(contar(tabela)).isZero();
  }

  /** O mesmo {@code CHECK} do original: o {@code jsonb} aceitaria um objeto sem reclamar. */
  @Test
  @DisplayName("deve recusar destaques que nao sejam uma lista")
  void shouldRejectHighlights_whenTheyAreNotAnArray() {
    // when
    var thrown =
        catchThrowable(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO experience_translation
                        (experience_id, locale, role, description, highlights)
                    VALUES (?, 'en-US', 'Role', 'Description.', '{"a": 1}'::jsonb)
                    """,
                    paiDe(Tabela.EXPERIENCE)));

    // then
    assertThat(thrown).hasMessageContaining("experience_translation_highlights_is_array_ck");
  }

  /** Duas categorias com o mesmo nome traduzido seriam o mesmo cabecalho duas vezes na pagina. */
  @Test
  @DisplayName("deve recusar duas categorias com o mesmo nome no mesmo idioma")
  void shouldRejectCategoryName_whenItRepeatsWithinTheLocale() {
    // given
    var categorias =
        jdbcTemplate.queryForList("SELECT id FROM skill_category ORDER BY id LIMIT 2", Long.class);
    inserir(Tabela.SKILL_CATEGORY, categorias.get(0), "en-US", "Databases");

    // when
    var thrown =
        catchThrowable(
            () -> inserir(Tabela.SKILL_CATEGORY, categorias.get(1), "en-US", "Databases"));

    // then
    assertThat(thrown).hasMessageContaining("skill_category_translation_locale_name_uk");
  }

  /**
   * O resumo traduzido tem o limite do original, e o motivo e o mesmo: ele cabe no cartao de
   * compartilhamento. Foi isso que recusou o {@code jsonb} na propria linha (ADR-0013).
   */
  @Test
  @DisplayName("deve recusar resumo traduzido acima de 280 caracteres")
  void shouldRejectSummary_whenLongerThanTheOriginalLimit() {
    // when
    var thrown =
        catchThrowable(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO project_translation
                        (project_id, locale, title, summary, problem, solution, outcome)
                    VALUES (?, 'en-US', 'Title', ?, 'Problem.', 'Solution.', 'Outcome.')
                    """,
                    paiDe(Tabela.PROJECT),
                    "x".repeat(281)));

    // then
    assertThat(thrown).hasMessageContaining("character varying(280)");
  }

  private long paiDe(Tabela tabela) {
    Long id = jdbcTemplate.queryForObject("SELECT min(id) FROM " + tabela.pai, Long.class);
    assertThat(id).as("o seed precisa ter ao menos uma linha em %s", tabela.pai).isNotNull();
    return id;
  }

  private void inserir(Tabela tabela, long pai, String idioma, String texto) {
    jdbcTemplate.update(tabela.inserir, pai, idioma, texto);
  }

  private int contar(Tabela tabela) {
    Integer total =
        jdbcTemplate.queryForObject("SELECT count(*) FROM " + tabela.nome, Integer.class);
    return total == null ? 0 : total;
  }
}
