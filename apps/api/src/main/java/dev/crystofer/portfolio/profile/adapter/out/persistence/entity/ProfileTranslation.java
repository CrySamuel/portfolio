package dev.crystofer.portfolio.profile.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Linha de {@code profile_translation}: o texto do perfil noutro idioma.
 *
 * <p><strong>{@code @Embeddable}, e nao {@code @Entity}.</strong> A traducao nao tem identidade
 * propria - a chave da tabela e o par (perfil, idioma) -, nao e consultada sozinha e nao existe sem
 * o perfil. E a definicao de tipo de valor, e o JPA a mapeia como {@code @ElementCollection} no
 * dono, sem id, sem repositorio e sem o mapeamento de chave composta que uma entidade exigiria.
 */
@Embeddable
public class ProfileTranslation {

  @Column(nullable = false)
  private String locale;

  @Column(nullable = false)
  private String headline;

  @Column(nullable = false)
  private String bio;

  /** Nulavel como no original: sem local traduzido, vale o do perfil. */
  @Column private String location;

  /** Exigido pelo Hibernate. */
  protected ProfileTranslation() {}

  public ProfileTranslation(String locale, String headline, String bio, String location) {
    this.locale = locale;
    this.headline = headline;
    this.bio = bio;
    this.location = location;
  }

  public String getLocale() {
    return locale;
  }

  public String getHeadline() {
    return headline;
  }

  public String getBio() {
    return bio;
  }

  public String getLocation() {
    return location;
  }
}
