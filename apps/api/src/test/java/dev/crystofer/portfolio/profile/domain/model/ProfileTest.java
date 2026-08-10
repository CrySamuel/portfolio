package dev.crystofer.portfolio.profile.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileTest {

  private static final SocialLink GITHUB =
      new SocialLink(SocialPlatform.GITHUB, "https://github.com/CrySamuel", 0);
  private static final SocialLink LINKEDIN =
      new SocialLink(SocialPlatform.LINKEDIN, "https://linkedin.com/in/x", 1);

  @Test
  @DisplayName("deve ordenar os links por ordem de exibicao, independente da ordem recebida")
  void shouldSortLinks_whenGivenOutOfOrder() {
    // given
    var foraDeOrdem = List.of(LINKEDIN, GITHUB);

    // when
    var profile = profileWith(foraDeOrdem);

    // then
    assertThat(profile.socialLinks()).containsExactly(GITHUB, LINKEDIN);
  }

  @Test
  @DisplayName("deve ignorar alteracoes na lista original apos a construcao")
  void shouldNotShareState_whenSourceListIsMutatedLater() {
    // given
    var mutavel = new ArrayList<>(List.of(GITHUB));
    var profile = profileWith(mutavel);

    // when
    mutavel.add(LINKEDIN);

    // then
    assertThat(profile.socialLinks()).containsExactly(GITHUB);
  }

  @Test
  @DisplayName("deve rejeitar a mesma plataforma declarada duas vezes")
  void shouldReject_whenPlatformIsRepeated() {
    // given
    var repetida =
        List.of(GITHUB, new SocialLink(SocialPlatform.GITHUB, "https://github.com/y", 1));

    // when
    var thrown = catchThrowable(() -> profileWith(repetida));

    // then
    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Plataforma repetida");
  }

  @Test
  @DisplayName("deve encontrar o link de uma plataforma declarada")
  void shouldFindLink_whenPlatformIsPresent() {
    // given
    var profile = profileWith(List.of(GITHUB, LINKEDIN));

    // when
    var encontrado = profile.findLink(SocialPlatform.LINKEDIN);

    // then
    assertThat(encontrado).contains(LINKEDIN);
  }

  @Test
  @DisplayName("deve devolver vazio ao procurar plataforma nao declarada")
  void shouldReturnEmpty_whenPlatformIsAbsent() {
    // given
    var profile = profileWith(List.of(GITHUB));

    // when
    var encontrado = profile.findLink(SocialPlatform.EMAIL);

    // then
    assertThat(encontrado).isEmpty();
  }

  @Test
  @DisplayName("deve tratar localizacao ausente como vazia, e nao como erro")
  void shouldReturnEmptyLocation_whenNotInformed() {
    // when
    var profile = profileWith(List.of());

    // then
    assertThat(profile.findLocation()).isEmpty();
    assertThat(profile.findResumeUrl()).isEmpty();
  }

  @Test
  @DisplayName("deve rejeitar localizacao presente porem em branco")
  void shouldReject_whenLocationIsPresentButBlank() {
    // when
    var thrown =
        catchThrowable(() -> new Profile("Nome", "Headline", "Bio", "   ", null, false, List.of()));

    // then
    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Localizacao");
  }

  @Test
  @DisplayName("deve rejeitar nome completo em branco")
  void shouldReject_whenFullNameIsBlank() {
    // when
    var thrown =
        catchThrowable(() -> new Profile("  ", "Headline", "Bio", null, null, false, List.of()));

    // then
    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Nome completo");
  }

  @Test
  @DisplayName("deve rejeitar headline acima do limite da coluna")
  void shouldReject_whenHeadlineExceedsColumnLength() {
    // given
    var longa = "a".repeat(181);

    // when
    var thrown =
        catchThrowable(() -> new Profile("Nome", longa, "Bio", null, null, false, List.of()));

    // then
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("180");
  }

  @Test
  @DisplayName("deve exigir lista de links, ainda que vazia")
  void shouldReject_whenLinkListIsNull() {
    // when
    var thrown =
        catchThrowable(() -> new Profile("Nome", "Headline", "Bio", null, null, false, null));

    // then
    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("List.of()");
  }

  private static Profile profileWith(List<SocialLink> links) {
    return new Profile(
        "Crystofer Demetino", "Desenvolvedor Backend", "Bio", null, null, false, links);
  }
}
