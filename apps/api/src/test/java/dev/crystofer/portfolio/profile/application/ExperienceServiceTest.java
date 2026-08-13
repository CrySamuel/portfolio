package dev.crystofer.portfolio.profile.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.crystofer.portfolio.profile.domain.model.Experience;
import dev.crystofer.portfolio.profile.domain.port.out.LoadExperiencePort;

/** Sem Spring: a porta e um duble, entao o caso de uso roda em milissegundos (secao 13.6). */
@ExtendWith(MockitoExtension.class)
class ExperienceServiceTest {

  @Mock LoadExperiencePort loadExperiencePort;

  /**
   * A porta devolve fora de ordem de proposito.
   *
   * <p>E o que prova que a garantia nao depende de a origem colaborar - trocar o adaptador, o banco
   * ou a consulta nao pode mudar o que sai daqui.
   */
  @Test
  @DisplayName("deve devolver a timeline ordenada mesmo com a origem fora de ordem")
  void shouldReturnOrderedTimeline_whenSourceIsUnordered() {
    // given
    var antiga = umaExperiencia("Antiga", 2018);
    var recente = umaExperiencia("Recente", 2024);
    given(loadExperiencePort.loadExperiences()).willReturn(List.of(antiga, recente));
    var service = new ExperienceService(loadExperiencePort);

    // when
    var timeline = service.listExperiences();

    // then
    assertThat(timeline.experiences()).containsExactly(recente, antiga);
  }

  /**
   * A diferenca deliberada em relacao ao {@link ProfileService}.
   *
   * <p>La, origem vazia vira erro, porque portfolio sem perfil e sistema quebrado. Aqui vira
   * timeline vazia, porque portfolio sem experiencia cadastrada e apenas conteudo que o dono ainda
   * nao informou. Se este teste passar a exigir excecao, a secao Sobre sai do ar por falta de
   * conteudo.
   */
  @Test
  @DisplayName("deve devolver timeline vazia, e nao erro, quando a origem nao tem passagens")
  void shouldReturnEmptyTimeline_whenSourceHasNone() {
    // given
    given(loadExperiencePort.loadExperiences()).willReturn(List.of());
    var service = new ExperienceService(loadExperiencePort);

    // when
    var timeline = service.listExperiences();

    // then
    assertThat(timeline.isEmpty()).isTrue();
    assertThat(timeline.findCurrent()).isEmpty();
  }

  private static Experience umaExperiencia(String company, int anoInicio) {
    return new Experience(
        company, "Dev", LocalDate.of(anoInicio, 1, 1), null, "Descricao", List.of());
  }
}
