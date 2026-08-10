package dev.crystofer.portfolio.profile.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.crystofer.portfolio.shared.domain.EmailAddress;

class SocialLinkTest {

  @Test
  @DisplayName("deve montar a URL mailto a partir do value object")
  void shouldBuildMailtoUrl_whenCreatedFromEmailAddress() {
    // when
    var link = SocialLink.email(EmailAddress.of("contato@example.com"), 2);

    // then
    assertThat(link.url()).isEqualTo("mailto:contato@example.com");
    assertThat(link.platform()).isEqualTo(SocialPlatform.EMAIL);
  }

  @Test
  @DisplayName("deve devolver o endereco por tras de um link de e-mail")
  void shouldReturnAddress_whenPlatformIsEmail() {
    // given
    var link = SocialLink.email(EmailAddress.of("contato@example.com"), 0);

    // when
    var address = link.emailAddress();

    // then
    assertThat(address.value()).isEqualTo("contato@example.com");
  }

  @Test
  @DisplayName("deve recusar pedido de e-mail em link que nao e de e-mail")
  void shouldReject_whenAskedForEmailOfNonEmailPlatform() {
    // given
    var link = new SocialLink(SocialPlatform.GITHUB, "https://github.com/CrySamuel", 0);

    // when
    var thrown = catchThrowable(link::emailAddress);

    // then
    assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("GITHUB");
  }

  @Test
  @DisplayName("deve rejeitar link de plataforma web fora de https")
  void shouldReject_whenWebPlatformIsNotHttps() {
    // when
    var thrown =
        catchThrowable(
            () -> new SocialLink(SocialPlatform.GITHUB, "http://github.com/CrySamuel", 0));

    // then
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("https");
  }

  @Test
  @DisplayName("deve rejeitar link de e-mail apontando para https")
  void shouldReject_whenEmailPlatformUsesHttps() {
    // when
    var thrown =
        catchThrowable(() -> new SocialLink(SocialPlatform.EMAIL, "https://example.com", 0));

    // then
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mailto:");
  }

  @Test
  @DisplayName("deve rejeitar mailto com endereco invalido")
  void shouldReject_whenMailtoAddressIsInvalid() {
    // when
    var thrown = catchThrowable(() -> new SocialLink(SocialPlatform.EMAIL, "mailto:sem-arroba", 0));

    // then
    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("formato invalido");
  }

  @Test
  @DisplayName("deve rejeitar ordem de exibicao negativa")
  void shouldReject_whenDisplayOrderIsNegative() {
    // when
    var thrown =
        catchThrowable(() -> new SocialLink(SocialPlatform.GITHUB, "https://github.com/x", -1));

    // then
    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("negativa");
  }

  @Test
  @DisplayName("deve rejeitar URL acima do limite da coluna")
  void shouldReject_whenUrlExceedsColumnLength() {
    // given
    var longUrl = "https://github.com/" + "a".repeat(500);

    // when
    var thrown = catchThrowable(() -> new SocialLink(SocialPlatform.GITHUB, longUrl, 0));

    // then
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("500");
  }
}
