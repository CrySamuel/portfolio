package dev.crystofer.portfolio.profile.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;

import dev.crystofer.portfolio.profile.adapter.out.persistence.entity.SkillCategoryEntity;
import dev.crystofer.portfolio.profile.adapter.out.persistence.entity.SkillEntity;
import dev.crystofer.portfolio.profile.domain.model.Proficiency;

class SkillPersistenceMapperTest {

  private final SkillPersistenceMapper mapper = Mappers.getMapper(SkillPersistenceMapper.class);

  @Test
  @DisplayName("deve converter a categoria inteira, com as competencias")
  void shouldMapCategoryWithSkills() {
    // given
    var entity =
        new SkillCategoryEntity(
            1L,
            "Linguagens",
            (short) 0,
            List.of(
                new SkillEntity(1L, "Java", "advanced", (short) 3),
                new SkillEntity(2L, "Python", "intermediate", null)));

    // when
    var categoria = mapper.toDomain(entity);

    // then
    assertThat(categoria.name()).isEqualTo("Linguagens");
    assertThat(categoria.displayOrder()).isZero();
    assertThat(categoria.skills()).hasSize(2);
    assertThat(categoria.skills().getFirst().proficiency()).isEqualTo(Proficiency.ADVANCED);
    assertThat(categoria.skills().getFirst().findYearsOfExperience()).contains(3);
    assertThat(categoria.skills().getLast().findYearsOfExperience()).isEmpty();
  }

  @ParameterizedTest
  @DisplayName("deve traduzir o codigo da coluna para a constante do dominio")
  @CsvSource({"basic,BASIC", "intermediate,INTERMEDIATE", "advanced,ADVANCED"})
  void shouldMapProficiencyCode(String codigo, Proficiency esperado) {
    // when
    var skill = mapper.toDomain(new SkillEntity(1L, "Java", codigo, null));

    // then
    assertThat(skill.proficiency()).isEqualTo(esperado);
  }

  /**
   * Falha alto, e nao em silencio.
   *
   * <p>Nivel desconhecido no banco significa migracao que entrou sem o enum correspondente.
   * Devolver null ou pular a linha esconderia o problema, e a tela mostraria uma competencia a
   * menos sem nada dizendo por que.
   */
  @ParameterizedTest
  @DisplayName("deve falhar alto diante de nivel desconhecido")
  @ValueSource(strings = {"expert", "avancado", "  "})
  void shouldThrow_whenProficiencyIsUnknown(String desconhecido) {
    // when
    var thrown =
        catchThrowable(() -> mapper.toDomain(new SkillEntity(1L, "Java", desconhecido, null)));

    // then
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
  }

  /** O id da entidade nao tem componente correspondente no dominio - a fronteira funcionando. */
  @Test
  @DisplayName("deve deixar o id da entidade para tras")
  void shouldNotLeakId_whenMappingToDomain() {
    // when
    var skill = mapper.toDomain(new SkillEntity(42L, "Java", "advanced", (short) 3));

    // then
    assertThat(skill.toString()).doesNotContain("42");
  }
}
