package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.crystofer.portfolio.profile.domain.model.Proficiency;
import dev.crystofer.portfolio.profile.domain.model.Skill;
import dev.crystofer.portfolio.profile.domain.model.SkillCategory;
import dev.crystofer.portfolio.profile.domain.port.in.ListSkillsUseCase;

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

  @Autowired ListSkillsUseCase listSkillsUseCase;

  @AfterEach
  void limparAsTabelas() {
    jdbcTemplate.update("DELETE FROM skill_category");
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
