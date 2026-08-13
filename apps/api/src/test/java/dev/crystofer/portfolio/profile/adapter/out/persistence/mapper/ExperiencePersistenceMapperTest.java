package dev.crystofer.portfolio.profile.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import dev.crystofer.portfolio.profile.adapter.out.persistence.entity.ExperienceEntity;

class ExperiencePersistenceMapperTest {

  private final ExperiencePersistenceMapper mapper =
      Mappers.getMapper(ExperiencePersistenceMapper.class);

  @Test
  @DisplayName("deve converter a entidade inteira para o modelo de dominio")
  void shouldMapAllFields_whenEntityIsComplete() {
    // given
    var entity =
        new ExperienceEntity(
            1L,
            "Acme",
            "Dev Backend",
            LocalDate.of(2022, 3, 1),
            LocalDate.of(2024, 1, 31),
            "Descricao da passagem",
            List.of("Reduziu o p95 em 40%", "Migrou o deploy para container"));

    // when
    var experience = mapper.toDomain(entity);

    // then
    assertThat(experience.company()).isEqualTo("Acme");
    assertThat(experience.role()).isEqualTo("Dev Backend");
    assertThat(experience.startDate()).isEqualTo(LocalDate.of(2022, 3, 1));
    assertThat(experience.findEndDate()).contains(LocalDate.of(2024, 1, 31));
    assertThat(experience.description()).isEqualTo("Descricao da passagem");
    assertThat(experience.highlights())
        .containsExactly("Reduziu o p95 em 40%", "Migrou o deploy para container");
    assertThat(experience.isCurrent()).isFalse();
  }

  /**
   * O {@code id} da entidade nao tem componente correspondente no dominio, e essa e a fronteira
   * funcionando: chave tecnica de persistencia nao e informacao de negocio.
   */
  @Test
  @DisplayName("deve deixar o id da entidade para tras")
  void shouldNotLeakId_whenMappingToDomain() {
    // given
    var entity =
        new ExperienceEntity(
            42L, "Acme", "Dev", LocalDate.of(2022, 1, 1), null, "Descricao", List.of());

    // when
    var experience = mapper.toDomain(entity);

    // then
    assertThat(experience.toString()).doesNotContain("42");
  }

  @Test
  @DisplayName("deve preservar a ausencia de data de saida como cargo atual")
  void shouldKeepCurrent_whenEndDateIsNull() {
    // given
    var entity =
        new ExperienceEntity(
            1L, "Acme", "Dev", LocalDate.of(2024, 6, 1), null, "Descricao", List.of());

    // when
    var experience = mapper.toDomain(entity);

    // then
    assertThat(experience.isCurrent()).isTrue();
    assertThat(experience.findEndDate()).isEmpty();
  }

  /**
   * Lista vazia atravessa como lista vazia, e nao como {@code null}.
   *
   * <p>A coluna tem {@code DEFAULT '[]'} justamente para que nao existam dois jeitos de dizer "sem
   * destaques". Se o mapeamento transformasse um no outro, o default perderia a razao de existir.
   */
  @Test
  @DisplayName("deve manter lista de destaques vazia como vazia")
  void shouldKeepHighlightsEmpty_whenEntityHasNone() {
    // given
    var entity =
        new ExperienceEntity(
            1L, "Acme", "Dev", LocalDate.of(2024, 6, 1), null, "Descricao", List.of());

    // when
    var experience = mapper.toDomain(entity);

    // then
    assertThat(experience.highlights()).isEmpty();
  }
}
