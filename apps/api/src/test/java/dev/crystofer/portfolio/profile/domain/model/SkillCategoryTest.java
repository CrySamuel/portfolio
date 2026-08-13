package dev.crystofer.portfolio.profile.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A ordem <em>dentro</em> da categoria.
 *
 * <p>Todo caso entrega a lista fora de ordem de proposito - montar o cenario ja ordenado tornaria o
 * teste incapaz de distinguir "o tipo ordena" de "a lista chegou ordenada".
 */
class SkillCategoryTest {

  @Test
  @DisplayName("deve por o maior nivel primeiro")
  void shouldOrderByProficiencyDescending_whenInputIsShuffled() {
    // given
    var basico = new Skill("Rust", Proficiency.BASIC, null);
    var avancado = new Skill("Java", Proficiency.ADVANCED, 3);
    var intermediario = new Skill("Python", Proficiency.INTERMEDIATE, 2);

    // when
    var categoria = new SkillCategory("Linguagens", 0, List.of(basico, intermediario, avancado));

    // then
    assertThat(categoria.skills()).containsExactly(avancado, intermediario, basico);
  }

  /**
   * O desempate que impede a pagina de mudar de aparencia entre dois deploys.
   *
   * <p>Sem ele, competencias de mesmo nivel sairiam na ordem que a origem devolvesse - e a origem
   * nao promete nenhuma.
   */
  @Test
  @DisplayName("deve desempatar por nome quando o nivel coincide")
  void shouldFallBackToName_whenProficiencyTies() {
    // given
    var maven = new Skill("Maven", Proficiency.INTERMEDIATE, null);
    var docker = new Skill("Docker", Proficiency.INTERMEDIATE, null);
    var linux = new Skill("Linux", Proficiency.INTERMEDIATE, null);

    // when
    var categoria = new SkillCategory("Infraestrutura", 0, List.of(maven, linux, docker));

    // then
    assertThat(categoria.skills()).containsExactly(docker, linux, maven);
  }

  @Test
  @DisplayName("deve recusar a mesma competencia duas vezes na categoria")
  void shouldReject_whenSkillNameRepeats() {
    // given
    var uma = new Skill("Java", Proficiency.ADVANCED, 3);
    var outra = new Skill("Java", Proficiency.BASIC, 1);

    // when
    var thrown = catchThrowable(() -> new SkillCategory("Linguagens", 0, List.of(uma, outra)));

    // then
    assertThat(thrown).hasMessageContaining("Competencia repetida");
  }

  @Test
  @DisplayName("deve recusar nome de categoria ausente")
  void shouldRejectName_whenMissing() {
    // when
    var thrown = catchThrowable(() -> new SkillCategory("  ", 0, List.of()));

    // then
    assertThat(thrown).hasMessage("Nome da categoria e obrigatorio");
  }

  @Test
  @DisplayName("deve recusar lista de competencias ausente")
  void shouldReject_whenSkillListIsNull() {
    // when
    var thrown = catchThrowable(() -> new SkillCategory("Linguagens", 0, null));

    // then
    assertThat(thrown).hasMessageContaining("Lista de competencias e obrigatoria");
  }

  @Test
  @DisplayName("deve reconhecer categoria sem competencias")
  void shouldBeEmpty_whenThereAreNoSkills() {
    // when
    var categoria = new SkillCategory("Vazia", 0, List.of());

    // then
    assertThat(categoria.isEmpty()).isTrue();
  }

  @Test
  @DisplayName("deve isolar-se de alteracoes na lista recebida")
  void shouldCopy_whenSourceListChangesLater() {
    // given
    var origem = new ArrayList<>(List.of(new Skill("Java", Proficiency.ADVANCED, 3)));
    var categoria = new SkillCategory("Linguagens", 0, origem);

    // when
    origem.add(new Skill("Python", Proficiency.BASIC, null));

    // then
    assertThat(categoria.skills()).hasSize(1);
  }
}
