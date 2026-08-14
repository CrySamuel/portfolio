package dev.crystofer.portfolio.projects.domain.model;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.crystofer.portfolio.shared.domain.Slug;

/**
 * Os projetos na ordem em que a listagem os exibe.
 *
 * <p>Existe pela mesma razao que {@code Timeline} e {@code SkillCatalog}: para que a ordem seja
 * verdade por construcao e nao promessa que cada consulta precise lembrar de cumprir. A secao 16
 * nao lista este tipo entre os arquivos do commit, e a escolha repete a do commit 25 - um
 * comparador publico que cada chamador aplicasse falharia por omissao, e o modo de falhar e que a
 * lista continua vindo cheia, so que fora de ordem.
 *
 * <p>Ha dois consumidores com necessidades diferentes, e e isso que torna o tipo util em vez de
 * decorativo: a home mostra so os destacados (F06) e a listagem mostra todos (F07). Sem o catalogo,
 * "destacado, na ordem editorial" viraria um filtro escrito duas vezes.
 *
 * @param projects projetos em ordem de exibicao, sem slug repetido
 */
public record ProjectCatalog(List<Project> projects) {

  /**
   * Ordem editorial, com o titulo desempatando.
   *
   * <p>O {@code displayOrder} vem da coluna porque o primeiro card e o projeto mais forte, nao o
   * mais recente - e "mais forte" nao esta em nenhum outro campo. O titulo fecha a ordem: dois
   * projetos com o mesmo numero sairiam em ordem indefinida, e a pagina mudaria de aparencia entre
   * dois deploys sem que nada tivesse mudado.
   */
  private static final Comparator<Project> POR_ORDEM_EDITORIAL =
      Comparator.comparingInt(Project::displayOrder).thenComparing(Project::title);

  public ProjectCatalog {
    projects = order(projects);
  }

  /** Catalogo sem nenhum projeto - estado legitimo enquanto nao houver seed. */
  public static ProjectCatalog empty() {
    return new ProjectCatalog(List.of());
  }

  public boolean isEmpty() {
    return projects.isEmpty();
  }

  /**
   * Os projetos em destaque, na mesma ordem editorial.
   *
   * <p>A home pede este subconjunto e a listagem pede o todo. Filtrar aqui, e nao no componente,
   * mantem a decisao num lugar so - e impede que a home e a listagem discordem sobre o que "em
   * destaque" significa.
   */
  public List<Project> featured() {
    return projects.stream().filter(Project::featured).toList();
  }

  /**
   * O projeto de um slug, se houver.
   *
   * <p>Devolve {@link Optional} porque slug inexistente e pergunta legitima - alguem digitou a URL
   * errada, ou seguiu um link antigo. Quem traduz a ausencia em 404 e a camada de aplicacao, que e
   * onde a excecao de recurso nao encontrado vive; o dominio nao a conhece, e a regra do ArchUnit
   * garante isso.
   */
  public Optional<Project> findBySlug(Slug slug) {
    return projects.stream().filter(project -> project.slug().equals(slug)).findFirst();
  }

  private static List<Project> order(List<Project> projects) {
    if (projects == null) {
      throw new IllegalArgumentException(
          "Lista de projetos e obrigatoria; use ProjectCatalog.empty() se vazia");
    }
    Set<Slug> vistos = new HashSet<>();
    for (Project project : projects) {
      if (!vistos.add(project.slug())) {
        throw new IllegalArgumentException("Projeto repetido no catalogo: " + project.slug());
      }
    }
    return projects.stream().sorted(POR_ORDEM_EDITORIAL).toList();
  }
}
