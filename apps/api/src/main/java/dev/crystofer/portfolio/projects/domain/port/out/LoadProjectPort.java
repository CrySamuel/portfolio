package dev.crystofer.portfolio.projects.domain.port.out;

import java.util.List;
import java.util.Optional;

import dev.crystofer.portfolio.projects.domain.model.Project;
import dev.crystofer.portfolio.shared.domain.Slug;

/**
 * Porta de saida: carregar projetos da origem de conteudo.
 *
 * <p>Devolve {@code List}, e nao {@code ProjectCatalog}, pela mesma razao das outras portas de
 * saida: a porta descreve o que a origem tem a oferecer, na ordem em que ela entregar. Quem
 * transforma isso no catalogo ordenado e a camada de aplicacao, num lugar so.
 *
 * <p>Os dois metodos existem em vez de um, e a diferenca e de custo. Buscar um projeto pelo slug
 * carregando o catalogo inteiro para depois filtrar funciona com dois projetos e passa a nao
 * funcionar sem que nada avise - o adaptador do commit 35 resolve o detalhe numa consulta so, com
 * as tecnologias e as metricas no mesmo {@code EntityGraph}.
 *
 * <p>Escrita nao entra - o conteudo e alterado por migracao (ADR-0004).
 */
public interface LoadProjectPort {

  /**
   * @return os projetos registrados, possivelmente vazia
   */
  List<Project> loadProjects();

  /**
   * @return o projeto do slug, ou vazio se nao houver
   */
  Optional<Project> loadProjectBySlug(Slug slug);
}
