package dev.crystofer.portfolio.profile.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import dev.crystofer.portfolio.profile.adapter.out.persistence.entity.ProfileEntity;
import dev.crystofer.portfolio.profile.adapter.out.persistence.entity.SocialLinkEntity;
import dev.crystofer.portfolio.profile.domain.model.SocialPlatform;

class ProfilePersistenceMapperTest {

  private final ProfilePersistenceMapper mapper = Mappers.getMapper(ProfilePersistenceMapper.class);

  @Test
  @DisplayName("deve converter a entidade inteira para o modelo de dominio")
  void shouldMapAllFields_whenEntityIsComplete() {
    // given
    var entity =
        new ProfileEntity(
            1L,
            "Crystofer Demetino",
            "Desenvolvedor Backend",
            "Bio",
            "Remoto - Brasil",
            "https://example.com/cv.pdf",
            true,
            List.of(new SocialLinkEntity(1L, "github", "https://github.com/CrySamuel", (short) 0)));

    // when
    var profile = mapper.toDomain(entity);

    // then
    assertThat(profile.fullName()).isEqualTo("Crystofer Demetino");
    assertThat(profile.headline()).isEqualTo("Desenvolvedor Backend");
    assertThat(profile.bio()).isEqualTo("Bio");
    assertThat(profile.findLocation()).contains("Remoto - Brasil");
    assertThat(profile.findResumeUrl()).contains("https://example.com/cv.pdf");
    assertThat(profile.availableForWork()).isTrue();
    assertThat(profile.socialLinks()).hasSize(1);
  }

  @Test
  @DisplayName("deve traduzir a plataforma em minusculo do banco para a constante do dominio")
  void shouldTranslatePlatform_whenColumnIsLowercase() {
    // when
    var link = mapper.toDomain(new SocialLinkEntity(1L, "linkedin", "https://x.com/y", (short) 3));

    // then
    assertThat(link.platform()).isEqualTo(SocialPlatform.LINKEDIN);
    assertThat(link.displayOrder()).isEqualTo(3);
  }

  @Test
  @DisplayName("deve falhar alto quando a coluna trouxer plataforma desconhecida")
  void shouldFail_whenPlatformIsUnknown() {
    // when
    var thrown = catchThrowable(() -> mapper.toPlatform("mastodon"));

    // then
    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Plataforma desconhecida")
        .hasMessageContaining("mastodon");
  }

  @Test
  @DisplayName("deve manter campos opcionais ausentes como ausentes")
  void shouldKeepOptionalFieldsEmpty_whenColumnsAreNull() {
    // given
    var entity = new ProfileEntity(1L, "Nome", "Headline", "Bio", null, null, false, List.of());

    // when
    var profile = mapper.toDomain(entity);

    // then
    assertThat(profile.findLocation()).isEmpty();
    assertThat(profile.findResumeUrl()).isEmpty();
    assertThat(profile.socialLinks()).isEmpty();
  }

  @Test
  @DisplayName("deve aplicar as invariantes do dominio ao converter, e nao apenas copiar campos")
  void shouldEnforceDomainInvariants_whenEntityHasRepeatedPlatform() {
    // given
    var entity =
        new ProfileEntity(
            1L,
            "Nome",
            "Headline",
            "Bio",
            null,
            null,
            false,
            List.of(
                new SocialLinkEntity(1L, "github", "https://github.com/a", (short) 0),
                new SocialLinkEntity(2L, "github", "https://github.com/b", (short) 1)));

    // when
    var thrown = catchThrowable(() -> mapper.toDomain(entity));

    // then
    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Plataforma repetida");
  }
}
