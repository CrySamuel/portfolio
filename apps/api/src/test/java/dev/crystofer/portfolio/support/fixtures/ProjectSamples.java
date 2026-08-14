package dev.crystofer.portfolio.support.fixtures;

import java.time.LocalDate;
import java.util.List;

import dev.crystofer.portfolio.projects.domain.model.Project;
import dev.crystofer.portfolio.projects.domain.model.ProjectMetric;
import dev.crystofer.portfolio.projects.domain.model.Technology;
import dev.crystofer.portfolio.projects.domain.model.TechnologyCategory;
import dev.crystofer.portfolio.shared.domain.Slug;

/**
 * Projetos de dominio prontos, para os testes que nao tocam banco.
 *
 * <p>Separado de {@link ProjectFixtures}, que escreve linhas: aquele monta cenario de integracao,
 * este monta objetos. Juntar os dois obrigaria os testes unitarios a carregar {@code JdbcTemplate}
 * para usar um construtor.
 *
 * <p>O record tem catorze componentes, entao repetir a construcao em cada teste esconderia a linha
 * que importa no meio das treze iguais.
 */
public final class ProjectSamples {

  private ProjectSamples() {}

  /** Projeto minimo e valido, sem tecnologia e sem metrica. */
  public static Project projeto(String slug, String title, int displayOrder, boolean featured) {
    return new Project(
        Slug.of(slug),
        title,
        "Resumo do card.",
        "O problema.",
        "A solucao.",
        "O resultado.",
        null,
        null,
        null,
        featured,
        displayOrder,
        null,
        List.of(),
        List.of());
  }

  /** Projeto completo, com os quatro campos nulaveis preenchidos e as duas colecoes cheias. */
  public static Project projetoCompleto() {
    return new Project(
        Slug.of("finai"),
        "FinAI",
        "Assistente financeiro no Telegram.",
        "O problema.",
        "A solucao.",
        "O resultado.",
        "https://github.com/CrySamuel/FinAI-Bot",
        "https://t.me/gestor_crys_bot",
        "/images/projetos/finai.png",
        true,
        0,
        LocalDate.of(2026, 3, 24),
        List.of(
            new Technology("Python", Slug.of("python"), TechnologyCategory.LANGUAGE, null),
            new Technology(
                "Oracle Cloud", Slug.of("oracle-cloud"), TechnologyCategory.INFRASTRUCTURE, null)),
        List.of(
            new ProjectMetric("Economia em um mes", "R$ 800+", 0),
            new ProjectMetric("Investimento mensal", "R$ 200+", 1)));
  }
}
