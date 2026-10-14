package dev.crystofer.portfolio.projects.adapter.out.persistence.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;

import dev.crystofer.portfolio.shared.domain.ContentLanguage;

/**
 * Linha de {@code project_metric}.
 *
 * <p>O campo {@code value} espelha o nome da coluna, que a seccao 3.7 especifica. E palavra-chave
 * nao reservada no PostgreSQL e ja tinha sido aceita sem aspas quando a V4 foi aplicada.
 */
@Entity
@Table(name = "project_metric")
public class ProjectMetricEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String label;

  @Column(nullable = false)
  private String value;

  /**
   * {@code short}, e nao {@code int}, porque a coluna e {@code SMALLINT}.
   *
   * <p>Foi o {@code ddl-auto: validate} que ensinou isso no commit 30, recusando o contexto com
   * {@code found [int2], but expecting [integer]}. Quem cede e a entidade, que espelha o banco; a
   * conversao para o tipo natural do dominio e do mapper.
   */
  @Column(name = "display_order", nullable = false)
  private short displayOrder;

  /**
   * O rotulo e o valor nos outros idiomas. {@code @BatchSize} porque as metricas ja chegam em lote,
   * e as traducoes delas precisam chegar da mesma forma - uma consulta para todas.
   */
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "project_metric_translation",
      joinColumns = @JoinColumn(name = "metric_id"))
  @BatchSize(size = 64)
  private List<MetricTranslation> translations = new ArrayList<>();

  /** Exigido pelo Hibernate. Protegido para que ninguem o use por engano. */
  protected ProjectMetricEntity() {}

  /** Construtor completo, usado pelos testes do mapper. A aplicacao nunca constroi entidade. */
  public ProjectMetricEntity(Long id, String label, String value, short displayOrder) {
    this.id = id;
    this.label = label;
    this.value = value;
    this.displayOrder = displayOrder;
  }

  public Long getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public String getValue() {
    return value;
  }

  public short getDisplayOrder() {
    return displayOrder;
  }

  public List<MetricTranslation> getTranslations() {
    return translations;
  }

  /** Esta metrica com rotulo e valor no idioma pedido, ou ela mesma. */
  public ProjectMetricEntity traduzidoPara(ContentLanguage idioma) {
    if (idioma.isPadrao()) {
      return this;
    }

    return translations.stream()
        .filter(traducao -> traducao.getLocale().equals(idioma.tag()))
        .findFirst()
        .map(
            traducao ->
                new ProjectMetricEntity(id, traducao.getLabel(), traducao.getValue(), displayOrder))
        .orElse(this);
  }
}
