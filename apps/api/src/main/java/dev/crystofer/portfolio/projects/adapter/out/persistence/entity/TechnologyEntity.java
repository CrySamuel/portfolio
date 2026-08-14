package dev.crystofer.portfolio.projects.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Linha de {@code technology}.
 *
 * <p>Nao ha referencia de volta para {@code project}, e a ausencia repete a escolha das outras
 * entidades deste projeto: bidirecional serve para sincronizar os dois lados durante a escrita, e
 * aqui nao ha escrita - o conteudo entra por migracao (ADR-0004).
 *
 * <p>{@code created_at} e {@code updated_at} ficam fora do mapeamento de proposito. O {@code
 * ddl-auto: validate} nao reclama de coluna que existe no banco e nao na entidade - so do
 * contrario.
 */
@Entity
@Table(name = "technology")
public class TechnologyEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String slug;

  @Column(nullable = false)
  private String category;

  @Column(name = "icon_slug")
  private String iconSlug;

  /** Exigido pelo Hibernate. Protegido para que ninguem o use por engano. */
  protected TechnologyEntity() {}

  /** Construtor completo, usado pelos testes do mapper. A aplicacao nunca constroi entidade. */
  public TechnologyEntity(Long id, String name, String slug, String category, String iconSlug) {
    this.id = id;
    this.name = name;
    this.slug = slug;
    this.category = category;
    this.iconSlug = iconSlug;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getSlug() {
    return slug;
  }

  public String getCategory() {
    return category;
  }

  public String getIconSlug() {
    return iconSlug;
  }
}
