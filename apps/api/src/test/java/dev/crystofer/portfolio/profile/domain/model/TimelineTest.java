package dev.crystofer.portfolio.profile.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A ordem da timeline, que e o objetivo deste commit.
 *
 * <p>Todo caso entrega a lista <strong>fora de ordem</strong> de proposito. Montar o cenario ja
 * ordenado tornaria o teste incapaz de distinguir "o tipo ordena" de "a lista chegou ordenada" - e
 * essa e exatamente a suposicao que, no modulo de perfil, se provou errada quando foi medida.
 */
class TimelineTest {

  @Test
  @DisplayName("deve ordenar da passagem mais recente para a mais antiga")
  void shouldOrderByStartDateDescending_whenInputIsShuffled() {
    // given
    var antiga = umaExperiencia("Antiga", "Dev", 2018, null);
    var meio = umaExperiencia("Meio", "Dev", 2021, null);
    var recente = umaExperiencia("Recente", "Dev", 2024, null);

    // when
    var timeline = new Timeline(List.of(meio, antiga, recente));

    // then
    assertThat(timeline.experiences()).containsExactly(recente, meio, antiga);
  }

  /**
   * O desempate que impede a pagina de mudar de aparencia entre dois deploys.
   *
   * <p>Sem ele a ordem entre passagens iniciadas no mesmo mes seria a que a origem devolvesse, e a
   * origem nao promete nenhuma.
   */
  @Test
  @DisplayName("deve por o cargo atual antes do encerrado quando as datas de inicio empatam")
  void shouldPreferCurrent_whenStartDatesTie() {
    // given
    var encerrada = umaExperiencia("Acme", "Dev", 2022, 2023);
    var atual = umaExperiencia("Acme", "Tech Lead", 2022, null);

    // when
    var timeline = new Timeline(List.of(encerrada, atual));

    // then
    assertThat(timeline.experiences()).containsExactly(atual, encerrada);
  }

  @Test
  @DisplayName("deve desempatar por empresa e cargo quando data e situacao coincidem")
  void shouldFallBackToAlphabetical_whenDateAndCurrencyTie() {
    // given
    var beta = umaExperiencia("Beta", "Dev", 2022, 2023);
    var alfa = umaExperiencia("Alfa", "Dev", 2022, 2023);
    var alfaOutroCargo = umaExperiencia("Alfa", "Analista", 2022, 2023);

    // when
    var timeline = new Timeline(List.of(beta, alfa, alfaOutroCargo));

    // then
    assertThat(timeline.experiences()).containsExactly(alfaOutroCargo, alfa, beta);
  }

  @Test
  @DisplayName("deve recusar a mesma passagem duas vezes")
  void shouldReject_whenPassageIsRepeated() {
    // given
    var passagem = umaExperiencia("Acme", "Dev", 2022, null);

    // when
    var thrown = catchThrowable(() -> new Timeline(List.of(passagem, passagem)));

    // then
    assertThat(thrown).hasMessageContaining("Passagem repetida");
  }

  @Test
  @DisplayName("deve aceitar o retorno a mesma empresa e cargo em outra data")
  void shouldAccept_whenSameCompanyAndRoleStartOnAnotherDate() {
    // given
    var primeira = umaExperiencia("Acme", "Dev", 2018, 2020);
    var retorno = umaExperiencia("Acme", "Dev", 2023, null);

    // when
    var timeline = new Timeline(List.of(primeira, retorno));

    // then
    assertThat(timeline.experiences()).containsExactly(retorno, primeira);
  }

  @Test
  @DisplayName("deve recusar lista ausente")
  void shouldReject_whenListIsNull() {
    // when
    var thrown = catchThrowable(() -> new Timeline(null));

    // then
    assertThat(thrown).hasMessageContaining("Lista de experiencias e obrigatoria");
  }

  @Test
  @DisplayName("deve tratar timeline sem passagens como estado legitimo")
  void shouldBeEmpty_whenThereIsNoPassage() {
    // when
    var timeline = Timeline.empty();

    // then
    assertThat(timeline.isEmpty()).isTrue();
    assertThat(timeline.experiences()).isEmpty();
    assertThat(timeline.findCurrent()).isEmpty();
  }

  @Test
  @DisplayName("deve encontrar o cargo atual quando existe")
  void shouldFindCurrent_whenOnePassageHasNoEndDate() {
    // given
    var encerrada = umaExperiencia("Antiga", "Dev", 2018, 2020);
    var atual = umaExperiencia("Atual", "Dev", 2021, null);

    // when
    var timeline = new Timeline(List.of(encerrada, atual));

    // then
    assertThat(timeline.findCurrent()).contains(atual);
  }

  @Test
  @DisplayName("nao deve encontrar cargo atual quando todas as passagens terminaram")
  void shouldNotFindCurrent_whenEveryPassageHasEnded() {
    // given
    var uma = umaExperiencia("Uma", "Dev", 2018, 2020);
    var outra = umaExperiencia("Outra", "Dev", 2021, 2023);

    // when
    var timeline = new Timeline(List.of(uma, outra));

    // then
    assertThat(timeline.findCurrent()).isEmpty();
  }

  @Test
  @DisplayName("deve isolar-se de alteracoes na lista recebida")
  void shouldCopy_whenSourceListChangesLater() {
    // given
    var origem = new ArrayList<>(List.of(umaExperiencia("Acme", "Dev", 2022, null)));
    var timeline = new Timeline(origem);

    // when
    origem.add(umaExperiencia("Outra", "Dev", 2023, null));

    // then
    assertThat(timeline.experiences()).hasSize(1);
  }

  private static Experience umaExperiencia(
      String company, String role, int anoInicio, Integer anoFim) {
    return new Experience(
        company,
        role,
        LocalDate.of(anoInicio, 1, 1),
        anoFim == null ? null : LocalDate.of(anoFim, 1, 1),
        "Descricao",
        List.of());
  }
}
