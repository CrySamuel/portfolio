package dev.crystofer.portfolio.projects.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
}
