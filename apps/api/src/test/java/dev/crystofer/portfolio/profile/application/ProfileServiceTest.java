package dev.crystofer.portfolio.profile.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.crystofer.portfolio.profile.domain.model.Profile;
import dev.crystofer.portfolio.profile.domain.port.out.LoadProfilePort;
import dev.crystofer.portfolio.shared.error.ResourceNotFoundException;

/** Sem Spring: a porta e um duble, entao o caso de uso roda em milissegundos (secao 13.6). */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

  @Mock LoadProfilePort loadProfilePort;

  @Test
  @DisplayName("deve devolver o perfil quando a origem tem dado")
  void shouldReturnProfile_whenSourceHasOne() {
    // given
    var esperado = umPerfil();
    given(loadProfilePort.loadProfile()).willReturn(Optional.of(esperado));
    var service = new ProfileService(loadProfilePort);

    // when
    var obtido = service.getProfile();

    // then
    assertThat(obtido).isEqualTo(esperado);
  }

  @Test
  @DisplayName("deve traduzir origem vazia em erro, e nao propagar o Optional")
  void shouldThrow_whenSourceIsEmpty() {
    // given
    given(loadProfilePort.loadProfile()).willReturn(Optional.empty());
    var service = new ProfileService(loadProfilePort);

    // when
    var thrown = catchThrowable(service::getProfile);

    // then
    assertThat(thrown)
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Perfil nao encontrado");
  }

  @Test
  @DisplayName("deve manter fora da mensagem publica qualquer detalhe de infraestrutura")
  void shouldNotLeakInternals_whenResourceIsMissing() {
    // given
    given(loadProfilePort.loadProfile()).willReturn(Optional.empty());
    var service = new ProfileService(loadProfilePort);

    // when
    var thrown = catchThrowable(service::getProfile);

    // then
    assertThat(thrown.getMessage())
        .doesNotContainIgnoringCase("sql")
        .doesNotContainIgnoringCase("seed")
        .doesNotContainIgnoringCase("tabela")
        .doesNotContainIgnoringCase("migration");
  }

  private static Profile umPerfil() {
    return new Profile("Crystofer Demetino", "Backend", "Bio", null, null, true, List.of());
  }
}
