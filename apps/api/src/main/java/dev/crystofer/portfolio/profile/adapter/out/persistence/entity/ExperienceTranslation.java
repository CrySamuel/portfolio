package dev.crystofer.portfolio.profile.adapter.out.persistence.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Linha de {@code experience_translation}: cargo, descricao e destaques noutro idioma.
 *
 * <p>Tipo de valor, como {@link ProfileTranslation} e pela mesma razao. A empresa e as datas nao
 * estao aqui porque nao se traduzem: "DXC Technology" e marco de 2024 sao os mesmos em ingles.
 */
@Embeddable
public class ExperienceTranslation {

  @Column(nullable = false)
  private String locale;

  @Column(nullable = false)
  private String role;

  @Column(nullable = false)
  private String description;

  /** O mesmo mapeamento de {@code jsonb} do original, e a mesma forma: lista de frases. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private List<String> highlights;

  /** Exigido pelo Hibernate. */
  protected ExperienceTranslation() {}

  public ExperienceTranslation(
      String locale, String role, String description, List<String> highlights) {
    this.locale = locale;
    this.role = role;
    this.description = description;
    this.highlights = highlights;
  }

  public String getLocale() {
    return locale;
  }

  public String getRole() {
    return role;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getHighlights() {
    return highlights;
  }
}
