package dev.crystofer.portfolio.profile.domain.port.out;

import java.util.List;

import dev.crystofer.portfolio.profile.domain.model.SkillCategory;

/**
 * Porta de saida: carregar as categorias com suas competencias.
 *
 * <p>Devolve {@code List}, e nao {@code SkillCatalog}, pela mesma razao de {@link
 * LoadExperiencePort}: a porta descreve o que a origem tem a oferecer, na ordem em que ela
 * entregar. Quem transforma isso no catalogo ordenado e a camada de aplicacao, num lugar so.
 *
 * <p>Escrita nao entra - o conteudo e alterado por migracao (ADR-0004).
 */
public interface LoadSkillPort {

  /**
   * @return as categorias registradas, possivelmente vazia
   */
  List<SkillCategory> loadSkillCategories();
}
