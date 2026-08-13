package dev.crystofer.portfolio.profile.adapter.in.web.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Um grupo de {@code GET /api/v1/skills}.
 *
 * <p>O arquivo se chama {@code SkillCategoryResponse}, e nao {@code SkillResponse} como a secao 16
 * lista. O motivo e do Java: o tipo publico da o nome ao arquivo, e o tipo publico aqui e a
 * categoria - o endpoint devolve grupos, nao competencias soltas. A competencia fica aninhada,
 * exatamente como {@code SocialLinkResponse} fica dentro de {@link ProfileResponse}.
 *
 * <p><strong>Nao expoe {@code displayOrder}.</strong> A lista ja chega ordenada, entao o campo
 * obrigaria o cliente a reordenar o que ja esta em ordem, e convidaria dois clientes a ordenar
 * diferente. A ordem do array e o contrato - a mesma decisao dos links sociais e da timeline.
 *
 * <p>Todo campo e {@code required}, inclusive o que pode vir nulo. Sao coisas diferentes e o
 * cliente tipado precisa das duas.
 */
@Schema(name = "SkillCategory", description = "Competencias agrupadas por categoria")
public record SkillCategoryResponse(
    @Schema(example = "Linguagens & Frameworks", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
    @Schema(
            description = "Competencias do grupo, da mais dominada para a menos",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<SkillResponse> skills) {

  /**
   * Uma competencia.
   *
   * @param proficiency codigo em minusculo, igual ao que a coluna guarda
   * @param yearsOfExperience {@code null} quando nao ha numero declarado - e diferente de zero, que
   *     significa "comecou agora"
   */
  @Schema(name = "Skill")
  public record SkillResponse(
      @Schema(example = "Java", requiredMode = Schema.RequiredMode.REQUIRED) String name,
      @Schema(
              example = "advanced",
              allowableValues = {"basic", "intermediate", "advanced"},
              requiredMode = Schema.RequiredMode.REQUIRED)
          String proficiency,
      @Schema(
              example = "3",
              types = {"integer", "null"},
              requiredMode = Schema.RequiredMode.REQUIRED)
          Integer yearsOfExperience) {}
}
