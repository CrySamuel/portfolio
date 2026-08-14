package dev.crystofer.portfolio.projects.domain.port.in;

import dev.crystofer.portfolio.projects.domain.model.Project;
import dev.crystofer.portfolio.shared.domain.Slug;

/**
 * Porta de entrada: obter um projeto pelo slug da URL.
 *
 * <p>Recebe {@link Slug}, e nao {@code String}, e e para isso que o value object existe. O caminho
 * da requisicao chega como texto; converte-lo na borda faz a validacao de formato acontecer uma
 * vez, antes de qualquer consulta - e um slug malformado vira 400 em vez de uma ida ao banco que
 * nunca teria como casar.
 *
 * <p><strong>Ausencia aqui e erro</strong>, ao contrario do catalogo vazio de {@link
 * ListProjectsUseCase}, e a diferenca e de significado: catalogo vazio e conteudo que ainda nao
 * existe, enquanto slug inexistente e um endereco que alguem pediu e o site nao tem. A
 * implementacao lanca a excecao de recurso nao encontrado, que vive na camada compartilhada de erro
 * - o dominio nao a conhece, e a regra do ArchUnit garante isso.
 *
 * @see dev.crystofer.portfolio.projects.domain.model.ProjectCatalog#findBySlug
 */
public interface GetProjectBySlugUseCase {

  Project getProjectBySlug(Slug slug);
}
