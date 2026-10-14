package dev.crystofer.portfolio.projects.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Linha de {@code project_metric_translation}: rotulo e valor noutro idioma.
 *
 * <p>O valor vem junto porque as vezes muda com o idioma - "3 meses" vira "3 months" -, e as vezes
 * nao: "R$ 800+" e o mesmo. A tabela exige os dois, e quem traduz decide.
 */
@Embeddable
public class MetricTranslation {

  @Column(nullable = false)
  private String locale;

  @Column(nullable = false)
  private String label;

  @Column(nullable = false)
  private String value;

  /** Exigido pelo Hibernate. */
  protected MetricTranslation() {}

  public MetricTranslation(String locale, String label, String value) {
    this.locale = locale;
    this.label = label;
    this.value = value;
  }

  public String getLocale() {
    return locale;
  }

  public String getLabel() {
    return label;
  }

  public String getValue() {
    return value;
  }
}
