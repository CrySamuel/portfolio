package dev.crystofer.portfolio.profile.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Invariantes da passagem, sem Spring e sem banco (secao 13.6). */
class ExperienceTest {

  private static final LocalDate INICIO = LocalDate.of(2022, 3, 1);

  @Test
  @DisplayName("deve normalizar espacos ao redor dos textos")
  void shouldTrim_whenTextsHavePadding() {
    // when
    var experience = umaExperiencia("  Acme  ", "  Dev Backend  ", INICIO, null);

    // then
    assertThat(experience.company()).isEqualTo("Acme");
    assertThat(experience.role()).isEqualTo("Dev Backend");
  }

  @ParameterizedTest
  @DisplayName("deve recusar empresa ausente ou em branco")
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t"})
  void shouldRejectCompany_whenMissingOrBlank(String invalid) {
    // when
    var thrown = catchThrowable(() -> umaExperiencia(invalid, "Dev", INICIO, null));

    // then
    assertThat(thrown).hasMessage("Empresa e obrigatorio");
  }

  @ParameterizedTest
  @DisplayName("deve recusar cargo ausente ou em branco")
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void shouldRejectRole_whenMissingOrBlank(String invalid) {
    // when
    var thrown = catchThrowable(() -> umaExperiencia("Acme", invalid, INICIO, null));

    // then
    assertThat(thrown).hasMessage("Cargo e obrigatorio");
  }

  @Test
  @DisplayName("deve recusar empresa acima do limite da coluna")
  void shouldRejectCompany_whenLongerThanColumn() {
    // when
    var thrown = catchThrowable(() -> umaExperiencia("a".repeat(121), "Dev", INICIO, null));

    // then
    assertThat(thrown).hasMessage("Empresa excede 120 caracteres: 121");
  }

  @Test
  @DisplayName("deve aceitar empresa exatamente no limite da coluna")
  void shouldAcceptCompany_whenExactlyAtColumnLimit() {
    // when
    var thrown = catchThrowable(() -> umaExperiencia("a".repeat(120), "Dev", INICIO, null));

    // then
    assertThat(thrown).isNull();
  }

  @Test
  @DisplayName("deve recusar data de inicio ausente")
  void shouldRejectStartDate_whenMissing() {
    // when
    var thrown = catchThrowable(() -> umaExperiencia("Acme", "Dev", null, null));

    // then
    assertThat(thrown).hasMessage("Data de inicio e obrigatoria");
  }

  @Test
  @DisplayName("deve recusar saida anterior a entrada")
  void shouldRejectPeriod_whenEndIsBeforeStart() {
    // when
    var thrown = catchThrowable(() -> umaExperiencia("Acme", "Dev", INICIO, INICIO.minusDays(1)));

    // then
    assertThat(thrown).hasMessageContaining("anterior a de inicio");
  }

  /** O limite do intervalo, e nao apenas um caso claramente valido - espelha o CHECK da V2. */
  @Test
  @DisplayName("deve aceitar entrada e saida no mesmo dia")
  void shouldAcceptPeriod_whenEndEqualsStart() {
    // when
    var thrown = catchThrowable(() -> umaExperiencia("Acme", "Dev", INICIO, INICIO));

    // then
    assertThat(thrown).isNull();
  }

  @Test
  @DisplayName("deve tratar ausencia de data de saida como cargo atual")
  void shouldBeCurrent_whenEndDateIsAbsent() {
    // when
    var experience = umaExperiencia("Acme", "Dev", INICIO, null);

    // then
    assertThat(experience.isCurrent()).isTrue();
    assertThat(experience.findEndDate()).isEmpty();
  }

  @Test
  @DisplayName("deve tratar posicao com data de saida como encerrada")
  void shouldNotBeCurrent_whenEndDateIsPresent() {
    // given
    var fim = INICIO.plusYears(2);

    // when
    var experience = umaExperiencia("Acme", "Dev", INICIO, fim);

    // then
    assertThat(experience.isCurrent()).isFalse();
    assertThat(experience.findEndDate()).contains(fim);
  }

  @Test
  @DisplayName("deve recusar destaque em branco em vez de descarta-lo em silencio")
  void shouldRejectHighlight_whenBlank() {
    // when
    var thrown =
        catchThrowable(
            () -> new Experience("Acme", "Dev", INICIO, null, "d", List.of("valido", "  ")));

    // then
    assertThat(thrown).hasMessage("Destaque em branco na lista");
  }

  @Test
  @DisplayName("deve recusar lista de destaques ausente")
  void shouldRejectHighlights_whenNull() {
    // when
    var thrown = catchThrowable(() -> new Experience("Acme", "Dev", INICIO, null, "d", null));

    // then
    assertThat(thrown).hasMessageContaining("Lista de destaques e obrigatoria");
  }

  @Test
  @DisplayName("deve normalizar espacos dos destaques")
  void shouldTrimHighlights_whenTheyHavePadding() {
    // when
    var experience = new Experience("Acme", "Dev", INICIO, null, "d", List.of("  um  ", "dois"));

    // then
    assertThat(experience.highlights()).containsExactly("um", "dois");
  }

  /**
   * Record com componente {@code List} nao e imovel de graca.
   *
   * <p>Sem a copia no construtor, quem passou a lista continuaria podendo altera-la depois, e o
   * "valor lido, nunca alterado" da documentacao seria falso.
   */
  @Test
  @DisplayName("deve isolar-se de alteracoes na lista recebida")
  void shouldCopyHighlights_whenSourceListChangesLater() {
    // given
    var origem = new ArrayList<>(List.of("um"));
    var experience = new Experience("Acme", "Dev", INICIO, null, "d", origem);

    // when
    origem.add("dois");

    // then
    assertThat(experience.highlights()).containsExactly("um");
  }

  @Test
  @DisplayName("deve expor a lista de destaques como imutavel")
  void shouldExposeImmutableHighlights_whenCallerTriesToChangeIt() {
    // given
    var experience = new Experience("Acme", "Dev", INICIO, null, "d", List.of("um"));

    // when
    var thrown = catchThrowable(() -> experience.highlights().add("dois"));

    // then
    assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
  }

  private static Experience umaExperiencia(
      String company, String role, LocalDate start, LocalDate end) {
    return new Experience(company, role, start, end, "Descricao", List.of());
  }
}
