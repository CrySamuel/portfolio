package dev.crystofer.portfolio.profile.adapter.in.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;

import dev.crystofer.portfolio.profile.adapter.in.web.dto.SkillCategoryResponse;
import dev.crystofer.portfolio.profile.domain.model.Proficiency;
import dev.crystofer.portfolio.profile.domain.model.Skill;
import dev.crystofer.portfolio.profile.domain.model.SkillCategory;

class SkillWebMapperTest {

  private final SkillWebMapper mapper = Mappers.getMapper(SkillWebMapper.class);

  @Test
  @DisplayName("deve converter a categoria inteira para o corpo da resposta")
  void shouldMapCategoryWithSkills() {
    // given
    var categoria =
        new SkillCategory(
            "Linguagens",
            0,
            List.of(
                new Skill("Java", Proficiency.ADVANCED, 3),
                new Skill("Python", Proficiency.INTERMEDIATE, null)));

    // when
    var response = mapper.toResponse(categoria);

    // then
    assertThat(response.name()).isEqualTo("Linguagens");
    assertThat(response.skills()).hasSize(2);
    assertThat(response.skills().getFirst().name()).isEqualTo("Java");
    assertThat(response.skills().getFirst().yearsOfExperience()).isEqualTo(3);
    assertThat(response.skills().getLast().yearsOfExperience()).isNull();
  }

  @ParameterizedTest
  @DisplayName("deve publicar o codigo do enum, e nao o nome da constante")
  @CsvSource({"BASIC,basic", "INTERMEDIATE,intermediate", "ADVANCED,advanced"})
  void shouldMapProficiencyToCode(Proficiency nivel, String esperado) {
    // when
    var response = mapper.toResponse(new Skill("Uma", nivel, null));

    // then
    assertThat(response.proficiency()).isEqualTo(esperado);
  }

  /**
   * A ordem recebida atravessa intacta, nos dois niveis.
   *
   * <p>E assim que a garantia do {@code SkillCatalog} chega ao JSON. Se o mapper reordenasse, seria
   * mais um lugar decidindo a ordem.
   */
  @Test
  @DisplayName("deve preservar a ordem das categorias e das competencias")
  void shouldPreserveOrder_whenMappingTheList() {
    // given
    var primeira =
        new SkillCategory(
            "Primeira",
            0,
            List.of(
                new Skill("Zeta", Proficiency.ADVANCED, null),
                new Skill("Alfa", Proficiency.BASIC, null)));
    var segunda =
        new SkillCategory("Segunda", 1, List.of(new Skill("Uma", Proficiency.BASIC, null)));

    // when
    var responses = mapper.toResponse(List.of(primeira, segunda));

    // then
    assertThat(responses)
        .extracting(SkillCategoryResponse::name)
        .containsExactly("Primeira", "Segunda");
    assertThat(responses.getFirst().skills())
        .extracting(SkillCategoryResponse.SkillResponse::name)
        .containsExactly("Zeta", "Alfa");
  }

  @Test
  @DisplayName("deve converter lista vazia em lista vazia")
  void shouldMapEmptyList_whenThereAreNoCategories() {
    // when
    var responses = mapper.toResponse(List.<SkillCategory>of());

    // then
    assertThat(responses).isEmpty();
  }
}
