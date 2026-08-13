package dev.crystofer.portfolio.profile.adapter.in.web.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import dev.crystofer.portfolio.profile.adapter.in.web.dto.SkillCategoryResponse;
import dev.crystofer.portfolio.profile.domain.model.Proficiency;
import dev.crystofer.portfolio.profile.domain.model.Skill;
import dev.crystofer.portfolio.profile.domain.model.SkillCategory;

/**
 * Converte o catalogo do dominio no corpo da resposta.
 *
 * <p>A conversao preserva a ordem recebida, nos dois niveis - categorias e competencias -, e e
 * assim que a garantia do {@code SkillCatalog} chega ao JSON. O mapper nao reordena nada.
 *
 * <p>{@code displayOrder} nao aparece no destino, e nao precisa: o {@code unmappedTargetPolicy}
 * vigia campos do <em>alvo</em> sem origem, que e onde mora o defeito caro. Propriedade de origem
 * que ninguem le e apenas dado que este contrato escolheu nao expor.
 */
@Mapper
public interface SkillWebMapper {

  SkillCategoryResponse toResponse(SkillCategory category);

  List<SkillCategoryResponse> toResponse(List<SkillCategory> categories);

  SkillCategoryResponse.SkillResponse toResponse(Skill skill);

  /**
   * O enum vira o codigo minusculo que a coluna guarda e o front consome.
   *
   * <p>Publicar {@code "ADVANCED"} obrigaria o cliente a normalizar, e a decidir sozinho como. O
   * formato do JSON e decisao do adaptador, nao consequencia de como o enum foi escrito em Java -
   * por isso a traducao usa {@code code()} e nao {@code name()}.
   */
  default String toProficiencyCode(Proficiency proficiency) {
    return proficiency.code();
  }
}
