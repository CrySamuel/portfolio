package dev.crystofer.portfolio.profile.adapter.in.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import dev.crystofer.portfolio.profile.adapter.in.web.dto.ProfileResponse;
import dev.crystofer.portfolio.profile.domain.model.Profile;
import dev.crystofer.portfolio.profile.domain.model.SocialLink;
import dev.crystofer.portfolio.profile.domain.model.SocialPlatform;

class ProfileWebMapperTest {

  private final ProfileWebMapper mapper = Mappers.getMapper(ProfileWebMapper.class);

  @Test
  @DisplayName("deve publicar a plataforma em minusculo, no codigo que o front consome")
  void shouldLowercasePlatform_whenMappingLink() {
    // given
    var link = new SocialLink(SocialPlatform.LINKEDIN, "https://linkedin.com/in/x", 0);

    // when
    var response = mapper.toResponse(link);

    // then
    assertThat(response.platform()).isEqualTo("linkedin");
    assertThat(response.url()).isEqualTo("https://linkedin.com/in/x");
  }

  @Test
  @DisplayName("deve preservar a ordem de exibicao na ordem do array")
  void shouldKeepOrder_whenMappingProfile() {
    // given
    var profile =
        new Profile(
            "Nome",
            "Headline",
            "Bio",
            "Remoto · Brasil",
            null,
            true,
            List.of(
                new SocialLink(SocialPlatform.EMAIL, "mailto:a@b.com", 2),
                new SocialLink(SocialPlatform.GITHUB, "https://github.com/x", 0)));

    // when
    var response = mapper.toResponse(profile);

    // then
    assertThat(response.socialLinks())
        .extracting(ProfileResponse.SocialLinkResponse::platform)
        .containsExactly("github", "email");
  }

  @Test
  @DisplayName("deve manter campos opcionais como nulos, e nao omiti-los")
  void shouldKeepOptionalFieldsNull_whenAbsentInDomain() {
    // given
    var profile = new Profile("Nome", "Headline", "Bio", null, null, false, List.of());

    // when
    var response = mapper.toResponse(profile);

    // then
    assertThat(response.location()).isNull();
    assertThat(response.resumeUrl()).isNull();
    assertThat(response.availableForWork()).isFalse();
    assertThat(response.socialLinks()).isEmpty();
  }
}
