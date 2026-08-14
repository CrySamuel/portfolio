package dev.crystofer.portfolio.projects.adapter.out.persistence.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;

/**
 * Linha de {@code project}, com as tecnologias e as metricas associadas.
 *
 * <p><strong>Sao duas colecoes, e e isso que torna este mapeamento diferente dos
 * anteriores.</strong> {@code ProfileEntity} e {@code SkillCategoryEntity} tem uma cada, entao um
 * {@code @EntityGraph} resolvia tudo num LEFT JOIN. Duas colecoes no mesmo fetch join produzem
 * <em>produto cartesiano</em> - cada tecnologia repetida uma vez por metrica -, e o Hibernate
 * recusa o caso quando as duas sao {@code List} sem indice, com {@code MultipleBagFetchException}.
 *
 * <p>A saida adotada divide o trabalho: as tecnologias vem no grafo, junto com o projeto, e as
 * metricas vem por lote. O numero de consultas fica <strong>constante</strong>, e nao proporcional
 * a quantidade de projetos, que e o que a Definition of Done do MVP 3 exige ao proibir N+1.
 */
@Entity
@Table(name = "project")
public class ProjectEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String slug;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String summary;

  @Column(nullable = false)
  private String problem;

  @Column(nullable = false)
  private String solution;

  @Column(nullable = false)
  private String outcome;

  @Column(name = "repo_url")
  private String repoUrl;

  @Column(name = "live_url")
  private String liveUrl;

  @Column(name = "cover_image")
  private String coverImage;

  @Column(nullable = false)
  private boolean featured;

  /** {@code short} porque a coluna e {@code SMALLINT} - ver {@link ProjectMetricEntity}. */
  @Column(name = "display_order", nullable = false)
  private short displayOrder;

  @Column(name = "published_at")
  private LocalDate publishedAt;

  /**
   * Resolvida pelo {@code @EntityGraph} do repositorio, junto com o projeto.
   *
   * <p>Das duas colecoes, esta e a que entra no grafo, e a escolha tem motivo: e a que precisa de
   * dois joins para ser montada, atravessando {@code project_tech} ate {@code technology}. Deixa-la
   * por lote custaria a consulta extra em cima do trabalho maior.
   *
   * <p>O {@code @OrderBy} e por nome, e aqui ele <em>pode</em> repetir a ordem do dominio - o que
   * em {@code SkillCategoryEntity} era impossivel. A diferenca e que a coluna guarda o nome
   * exibido, e nao um codigo: a ordem alfabetica dela e a mesma que {@code Project} aplica. Ordenar
   * por {@code category} e que seria o erro daquele commit outra vez, porque a ordem alfabetica dos
   * codigos nao tem relacao com nada.
   *
   * <p>Como quem garante a ordem e o dominio, esta anotacao e redundancia deliberada, do mesmo modo
   * que a de {@code ProfileEntity}. Ja custou uma suposicao errada uma vez - mudanca de consulta
   * nao prova ordem.
   */
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "project_tech",
      joinColumns = @JoinColumn(name = "project_id"),
      inverseJoinColumns = @JoinColumn(name = "technology_id"))
  @OrderBy("name ASC")
  private List<TechnologyEntity> technologies = new ArrayList<>();

  /**
   * Resolvida por lote, e nao pelo grafo.
   *
   * <p>O {@code @BatchSize} faz o Hibernate carregar as metricas de <em>todos</em> os projetos ja
   * carregados numa consulta so, na primeira vez que qualquer uma delas e tocada. E o que troca o
   * N+1 por uma consulta fixa sem pagar o produto cartesiano de um segundo fetch join.
   *
   * <p>O tamanho e folgado de proposito: o catalogo tem unidades de projetos, entao 32 garante que
   * um unico lote cobre tudo. Lote menor que a quantidade de projetos voltaria a produzir mais de
   * uma consulta - poucas, mas proporcionais.
   *
   * <p>O {@code @OrderBy} usa {@code displayOrder}, que e numero e nao codigo, entao a ordem do SQL
   * coincide com a do dominio.
   */
  @OneToMany(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  @OrderBy("displayOrder ASC")
  @BatchSize(size = 32)
  private List<ProjectMetricEntity> metrics = new ArrayList<>();

  /** Exigido pelo Hibernate. Protegido para que ninguem o use por engano. */
  protected ProjectEntity() {}

  /** Construtor completo, usado pelos testes do mapper. A aplicacao nunca constroi entidade. */
  public ProjectEntity(
      Long id,
      String slug,
      String title,
      String summary,
      String problem,
      String solution,
      String outcome,
      String repoUrl,
      String liveUrl,
      String coverImage,
      boolean featured,
      short displayOrder,
      LocalDate publishedAt,
      List<TechnologyEntity> technologies,
      List<ProjectMetricEntity> metrics) {
    this.id = id;
    this.slug = slug;
    this.title = title;
    this.summary = summary;
    this.problem = problem;
    this.solution = solution;
    this.outcome = outcome;
    this.repoUrl = repoUrl;
    this.liveUrl = liveUrl;
    this.coverImage = coverImage;
    this.featured = featured;
    this.displayOrder = displayOrder;
    this.publishedAt = publishedAt;
    this.technologies = technologies;
    this.metrics = metrics;
  }

  public Long getId() {
    return id;
  }

  public String getSlug() {
    return slug;
  }

  public String getTitle() {
    return title;
  }

  public String getSummary() {
    return summary;
  }

  public String getProblem() {
    return problem;
  }

  public String getSolution() {
    return solution;
  }

  public String getOutcome() {
    return outcome;
  }

  public String getRepoUrl() {
    return repoUrl;
  }

  public String getLiveUrl() {
    return liveUrl;
  }

  public String getCoverImage() {
    return coverImage;
  }

  public boolean isFeatured() {
    return featured;
  }

  public short getDisplayOrder() {
    return displayOrder;
  }

  public LocalDate getPublishedAt() {
    return publishedAt;
  }

  public List<TechnologyEntity> getTechnologies() {
    return technologies;
  }

  public List<ProjectMetricEntity> getMetrics() {
    return metrics;
  }
}
