package dev.crystofer.portfolio.projects.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Linha de {@code project_translation}: titulo e narrativa noutro idioma.
 *
 * <p>Tipo de valor, como as traducoes do modulo de perfil. O slug nao esta aqui de proposito: ele e
 * o endereco publico do projeto, e o mesmo projeto tem o mesmo slug em qualquer idioma - {@code
 * /projetos/finai} e {@code /en-US/projects/finai} (ADR-0013).
 */
@Embeddable
public class ProjectTranslation {

  @Column(nullable = false)
  private String locale;

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

  /** Exigido pelo Hibernate. */
  protected ProjectTranslation() {}

  public ProjectTranslation(
      String locale,
      String title,
      String summary,
      String problem,
      String solution,
      String outcome) {
    this.locale = locale;
    this.title = title;
    this.summary = summary;
    this.problem = problem;
    this.solution = solution;
    this.outcome = outcome;
  }

  public String getLocale() {
    return locale;
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
}
