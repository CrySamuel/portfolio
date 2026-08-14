package dev.crystofer.portfolio.projects.application;

import org.springframework.stereotype.Service;

import dev.crystofer.portfolio.projects.domain.model.Project;
import dev.crystofer.portfolio.projects.domain.model.ProjectCatalog;
import dev.crystofer.portfolio.projects.domain.port.in.GetProjectBySlugUseCase;
import dev.crystofer.portfolio.projects.domain.port.in.ListProjectsUseCase;
import dev.crystofer.portfolio.projects.domain.port.out.LoadProjectPort;
import dev.crystofer.portfolio.shared.domain.Slug;
import dev.crystofer.portfolio.shared.error.ResourceNotFoundException;

/**
 * Orquestra a leitura do catalogo de projetos.
 *
 * <p>Implementa os dois casos de uso porque eles operam sobre o mesmo agregado e a mesma porta de
 * saida. Separa-los em duas classes criaria dois lugares para a mesma dependencia sem separar
 * responsabilidade nenhuma.
 *
 * <p><strong>As duas leituras tratam a ausencia de formas opostas, e a diferenca e de
 * significado.</strong> Catalogo vazio e conteudo que o dono ainda nao informou, entao a lista sai
 * vazia e a secao apenas nao aparece; slug inexistente e um endereco que alguem pediu e o site nao
 * tem, entao vira 404. E aqui, num lugar so, que essa assimetria se resolve - a mesma escolha de
 * {@code ProfileService}, com o resultado invertido no primeiro caso.
 *
 * <p>A classe nao formata resposta, nao conhece HTTP e nao decide cache. Um servico de aplicacao
 * que crescesse para alem disso estaria acumulando responsabilidade de adaptador (secao 13.2).
 */
@Service
public class ProjectService implements ListProjectsUseCase, GetProjectBySlugUseCase {

  private final LoadProjectPort loadProjectPort;

  public ProjectService(LoadProjectPort loadProjectPort) {
    this.loadProjectPort = loadProjectPort;
  }

  /**
   * A porta devolve lista; o catalogo e montado aqui.
   *
   * <p>E o construtor de {@link ProjectCatalog} que ordena e recusa slug repetido, entao a garantia
   * de ordem passa a existir no momento em que o objeto passa a existir - e nao depende de o
   * adaptador ter lembrado do {@code ORDER BY}.
   */
  @Override
  public ProjectCatalog listProjects() {
    return new ProjectCatalog(loadProjectPort.loadProjects());
  }

  /**
   * A mensagem cita o slug pedido, e nao a tabela.
   *
   * <p>O slug veio da URL, entao devolve-lo ao cliente nao revela nada que ele ja nao soubesse - e
   * e o que permite a quem seguiu um link antigo entender o que aconteceu. Diagnostico de
   * infraestrutura fica no log do {@code GlobalExceptionHandler} (secao 2.4).
   */
  @Override
  public Project getProjectBySlug(Slug slug) {
    return loadProjectPort
        .loadProjectBySlug(slug)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "project", "Projeto nao encontrado: " + slug.value()));
  }
}
