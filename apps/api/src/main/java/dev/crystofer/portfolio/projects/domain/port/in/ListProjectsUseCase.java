package dev.crystofer.portfolio.projects.domain.port.in;

import dev.crystofer.portfolio.projects.domain.model.ProjectCatalog;

/**
 * Porta de entrada: listar o catalogo de projetos.
 *
 * <p>Devolve {@link ProjectCatalog}, e nao uma lista solta, pela mesma razao que a listagem de
 * competencias devolve o catalogo delas: a ordem e o recorte dos destacados sao regra de negocio, e
 * regra de negocio viaja dentro do tipo em vez de ficar por conta de quem consome.
 *
 * <p>Catalogo vazio nao e erro, pela mesma razao da timeline: portfolio sem projeto cadastrado e
 * conteudo que o dono ainda nao informou, e nao falha de infraestrutura. Traduzir isso em excecao
 * poria a secao inteira fora do ar enquanto o seed nao chega.
 */
public interface ListProjectsUseCase {

  ProjectCatalog listProjects();
}
