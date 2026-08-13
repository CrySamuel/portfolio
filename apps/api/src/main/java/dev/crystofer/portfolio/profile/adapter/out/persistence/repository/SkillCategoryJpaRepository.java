package dev.crystofer.portfolio.profile.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import dev.crystofer.portfolio.profile.adapter.out.persistence.entity.SkillCategoryEntity;

/**
 * Acesso Spring Data a {@code skill_category}.
 *
 * <p>Detalhe de infraestrutura, e por isso vive no adaptador: o caso de uso enxerga {@code
 * LoadSkillPort}, nunca esta interface.
 */
public interface SkillCategoryJpaRepository extends JpaRepository<SkillCategoryEntity, Long> {

  /**
   * As categorias com suas competencias, em uma consulta so.
   *
   * <p><strong>Aqui o {@code @EntityGraph} deixa de ser habito e passa a ser necessidade.</strong>
   * No perfil ele evitava um round trip, porque a tabela tem uma linha; com meia duzia de
   * categorias, sem ele seriam sete consultas para montar a secao - uma pelas categorias e uma por
   * categoria ao tocar a colecao preguicosa. O N+1 classico.
   *
   * <p>O {@code ORDER BY} do nome do metodo nao e o que garante a ordem exibida - quem garante e o
   * {@code SkillCatalog}, no dominio. Ele existe para o planejador entregar as linhas ja ordenadas.
   */
  @EntityGraph(attributePaths = "skills")
  List<SkillCategoryEntity> findAllByOrderByDisplayOrderAsc();
}
