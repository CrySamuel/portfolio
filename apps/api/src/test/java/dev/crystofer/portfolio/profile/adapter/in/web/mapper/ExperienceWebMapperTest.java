package dev.crystofer.portfolio.profile.adapter.in.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import dev.crystofer.portfolio.profile.adapter.in.web.dto.ExperienceResponse;
import dev.crystofer.portfolio.profile.domain.model.Experience;

class ExperienceWebMapperTest {

  private final ExperienceWebMapper mapper = Mappers.getMapper(ExperienceWebMapper.class);

  @Test
  @DisplayName("deve converter a passagem inteira para o corpo da resposta")
  void shouldMapAllFields_whenExperienceIsComplete() {
    // given
    var experience =
        new Experience(
            "Acme",
            "Dev Backend",
            LocalDate.of(2022, 3, 1),
            LocalDate.of(2024, 1, 31),
            "Descricao",
            List.of("um", "dois"));

    // when
    ExperienceResponse response = mapper.toResponse(experience);

    // then
    assertThat(response.company()).isEqualTo("Acme");
    assertThat(response.role()).isEqualTo("Dev Backend");
    assertThat(response.startDate()).isEqualTo(LocalDate.of(2022, 3, 1));
    assertThat(response.endDate()).isEqualTo(LocalDate.of(2024, 1, 31));
    assertThat(response.description()).isEqualTo("Descricao");
    assertThat(response.highlights()).containsExactly("um", "dois");
  }

  @Test
  @DisplayName("deve manter a data de saida nula do cargo atual")
  void shouldKeepNullEndDate_whenRoleIsCurrent() {
    // given
    var experience =
        new Experience("Acme", "Dev", LocalDate.of(2024, 1, 1), null, "Descricao", List.of());

    // when
    var response = mapper.toResponse(experience);

    // then
    assertThat(response.endDate()).isNull();
  }

  /**
   * A ordem recebida atravessa intacta.
   *
   * <p>E assim que a garantia do {@code Timeline} chega ao JSON. Se o mapper reordenasse, seria
   * mais um lugar decidindo a ordem - e a garantia deixaria de ter dono unico.
   */
  @Test
  @DisplayName("deve preservar a ordem da lista recebida")
  void shouldPreserveOrder_whenMappingTheList() {
    // given
    var primeira = umaExperiencia("Primeira", 2024);
    var segunda = umaExperiencia("Segunda", 2019);
    var terceira = umaExperiencia("Terceira", 2021);

    // when
    var responses = mapper.toResponse(List.of(primeira, segunda, terceira));

    // then
    assertThat(responses)
        .extracting(ExperienceResponse::company)
        .containsExactly("Primeira", "Segunda", "Terceira");
  }

  @Test
  @DisplayName("deve converter lista vazia em lista vazia")
  void shouldMapEmptyList_whenThereAreNoExperiences() {
    // when
    var responses = mapper.toResponse(List.<Experience>of());

    // then
    assertThat(responses).isEmpty();
  }

  private static Experience umaExperiencia(String company, int anoInicio) {
    return new Experience(
        company, "Dev", LocalDate.of(anoInicio, 1, 1), null, "Descricao", List.of());
  }
}
