package dev.crystofer.portfolio.profile.domain.port.in;

import dev.crystofer.portfolio.profile.domain.model.SkillCatalog;

/**
 * Porta de entrada: listar as competencias agrupadas por categoria.
 *
 * <p>Devolve {@link SkillCatalog}, e nao uma lista solta. O agrupamento e a ordem sao regra de
 * negocio - e o que a F05 determina ao dizer que o agrupamento nao e formatacao -, entao viajam
 * dentro do tipo em vez de ficarem por conta de quem consome.
 *
 * <p>Catalogo vazio nao e erro, pela mesma razao da timeline: portfolio sem competencia cadastrada
 * e conteudo que o dono ainda nao informou, e nao falha de infraestrutura.
 */
public interface ListSkillsUseCase {

  SkillCatalog listSkills();
}
