package dev.crystofer.portfolio.projects.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import dev.crystofer.portfolio.projects.adapter.out.persistence.entity.ProjectEntity;

/**
 * Acesso Spring Data a {@code project}.
 *
 * <p>Detalhe de infraestrutura, e por isso vive no adaptador: o caso de uso enxerga {@code
 * LoadProjectPort}, nunca esta interface.
 */
public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, Long> {

  /**
   * Os projetos com suas tecnologias, em uma consulta so; as metricas vem numa segunda, por lote.
   *
   * <p>O grafo cobre apenas {@code technologies} de proposito. Acrescentar {@code metrics} a lista
   * faria o Hibernate recusar com {@code MultipleBagFetchException} - e, se as colecoes fossem
   * {@code Set} em vez de {@code List}, ele aceitaria e produziria produto cartesiano, que e a
   * falha pior: nao quebra, so multiplica linhas.
   *
   * <p>O {@code ORDER BY} do nome do metodo nao e o que garante a ordem exibida - quem garante e o
   * {@code ProjectCatalog}, no dominio. Ele existe para o planejador entregar as linhas ja
   * ordenadas.
   */
  @EntityGraph(attributePaths = "technologies")
  List<ProjectEntity> findAllByOrderByDisplayOrderAsc();

  /**
   * Um projeto pelo slug da URL, com o mesmo grafo.
   *
   * <p>Existe como consulta propria, e nao como filtro sobre a listagem, pela razao registrada na
   * porta: carregar o catalogo inteiro para devolver um item funciona com dois projetos e passa a
   * nao funcionar sem que nada avise.
   */
  @EntityGraph(attributePaths = "technologies")
  Optional<ProjectEntity> findBySlug(String slug);
}
