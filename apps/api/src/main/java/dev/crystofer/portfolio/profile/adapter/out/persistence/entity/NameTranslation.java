package dev.crystofer.portfolio.profile.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Um nome noutro idioma - a linha de {@code skill_category_translation} e a de {@code
 * skill_translation}, que tem a mesma forma.
 *
 * <p>Uma classe para as duas, porque as duas sao exatamente isto: idioma e nome. A tabela de cada
 * uma fica no {@code @CollectionTable} de quem a usa, e nao aqui.
 */
@Embeddable
public class NameTranslation {

  @Column(nullable = false)
  private String locale;

  @Column(nullable = false)
  private String name;

  /** Exigido pelo Hibernate. */
  protected NameTranslation() {}

  public NameTranslation(String locale, String name) {
    this.locale = locale;
    this.name = name;
  }

  public String getLocale() {
    return locale;
  }

  public String getName() {
    return name;
  }
}
