package dev.crystofer.portfolio.profile.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Invariantes da competencia e do enum de nivel, sem Spring e sem banco. */
class SkillTest {

  @ParameterizedTest
  @DisplayName("deve recusar nome ausente ou em branco")
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void shouldRejectName_whenMissingOrBlank(String invalido) {
    // when
    var thrown = catchThrowable(() -> new Skill(invalido, Proficiency.BASIC, null));

    // then
    assertThat(thrown).hasMessage("Nome da competencia e obrigatorio");
  }

  @Test
  @DisplayName("deve normalizar espacos ao redor do nome")
  void shouldTrimName_whenItHasPadding() {
    // when
    var skill = new Skill("  Java  ", Proficiency.ADVANCED, 3);

    // then
    assertThat(skill.name()).isEqualTo("Java");
  }

  @Test
  @DisplayName("deve recusar nome acima do limite da coluna")
  void shouldRejectName_whenLongerThanColumn() {
    // when
    var thrown = catchThrowable(() -> new Skill("a".repeat(61), Proficiency.BASIC, null));

    // then
    assertThat(thrown).hasMessage("Nome da competencia excede 60 caracteres: 61");
  }

  @Test
  @DisplayName("deve recusar competencia sem nivel")
  void shouldRejectProficiency_whenMissing() {
    // when
    var thrown = catchThrowable(() -> new Skill("Java", null, 3));

    // then
    assertThat(thrown).hasMessageContaining("Nivel de proficiencia e obrigatorio");
  }

  @Test
  @DisplayName("deve recusar tempo de experiencia negativo")
  void shouldRejectYears_whenNegative() {
    // when
    var thrown = catchThrowable(() -> new Skill("Java", Proficiency.BASIC, -1));

    // then
    assertThat(thrown).hasMessageContaining("negativos");
  }

  /** O limite, e nao apenas um caso claramente valido - espelha o `skill_years_ck`. */
  @Test
  @DisplayName("deve aceitar zero ano de experiencia")
  void shouldAcceptYears_whenZero() {
    // when
    var skill = new Skill("Rust", Proficiency.BASIC, 0);

    // then
    assertThat(skill.findYearsOfExperience()).contains(0);
  }

  @Test
  @DisplayName("deve tratar tempo ausente como ausencia, e nao como zero")
  void shouldDistinguishAbsentYearsFromZero() {
    // when
    var semAnos = new Skill("Git", Proficiency.INTERMEDIATE, null);
    var zeroAnos = new Skill("Git", Proficiency.INTERMEDIATE, 0);

    // then
    assertThat(semAnos.findYearsOfExperience()).isEmpty();
    assertThat(zeroAnos.findYearsOfExperience()).contains(0);
  }

  /**
   * O codigo publicado nao e o nome da constante.
   *
   * <p>Sao coisas diferentes de proposito: renomear a constante e refatoracao, mudar o codigo e
   * quebra de contrato. Esta assercao e o que impede que uma vire a outra sem querer.
   */
  @Test
  @DisplayName("deve publicar o codigo em minusculo, igual ao da coluna")
  void shouldExposeLowercaseCode() {
    assertThat(Proficiency.BASIC.code()).isEqualTo("basic");
    assertThat(Proficiency.INTERMEDIATE.code()).isEqualTo("intermediate");
    assertThat(Proficiency.ADVANCED.code()).isEqualTo("advanced");
  }

  /**
   * A ordem de declaracao do enum e ordem de dominio, e nao acidente.
   *
   * <p>E dela que sai a ordenacao das competencias dentro de uma categoria. Trocar as constantes de
   * lugar mudaria silenciosamente a ordem da tela - esta assercao e o que transforma isso em build
   * vermelho.
   */
  @Test
  @DisplayName("deve manter a escala em ordem crescente de dominio")
  void shouldKeepScaleInAscendingOrder() {
    assertThat(Proficiency.BASIC)
        .isLessThan(Proficiency.INTERMEDIATE)
        .isLessThan(Proficiency.ADVANCED);
  }
}
