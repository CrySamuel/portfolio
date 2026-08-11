package dev.crystofer.portfolio.profile.adapter.in.web.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Corpo de {@code GET /api/v1/profile}.
 *
 * <p>DTO separado do modelo de dominio, e nao o dominio serializado direto. Serializar o dominio
 * amarraria o contrato publico a decisoes internas: renomear um campo por clareza viraria mudanca
 * incompativel para todo cliente, e qualquer campo novo vazaria sem ninguem decidir publica-lo.
 *
 * <p>Os nomes sao camelCase (secao 3.8) e este record e a fonte do schema OpenAPI, do qual o pacote
 * {@code @portfolio/api-client} e gerado.
 *
 * <p><strong>Todo campo e {@code required}, inclusive os que podem vir nulos.</strong> Sao coisas
 * diferentes e o cliente tipado precisa das duas: {@code required} promete que a chave esta sempre
 * no JSON, e a nulabilidade diz o que pode haver dentro dela. Sem o {@code required}, o tipo gerado
 * sai com tudo opcional e cada leitura de campo obriga a tratar uma ausencia que nunca acontece -
 * um contrato que descreve mal a API e piora o codigo de quem a consome.
 *
 * @param location {@code null} quando nao informada - o campo existe sempre, para que o cliente
 *     tipado nao precise distinguir ausente de nulo
 * @param resumeUrl {@code null} enquanto nao houver curriculo publicado
 */
@Schema(name = "Profile", description = "Perfil publico do portfolio")
public record ProfileResponse(
    @Schema(example = "Crystofer Demetino", requiredMode = Schema.RequiredMode.REQUIRED)
        String fullName,
    @Schema(
            example = "Desenvolvedor Backend — Java & Spring Boot",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String headline,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bio,
    @Schema(
            example = "Remoto · Brasil",
            types = {"string", "null"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        String location,
    @Schema(
            types = {"string", "null"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        String resumeUrl,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean availableForWork,
    @Schema(
            description = "Perfis externos, ja em ordem de exibicao",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<SocialLinkResponse> socialLinks) {

  /**
   * Um perfil externo.
   *
   * <p>Nao expoe {@code displayOrder}. A lista ja chega ordenada, entao o campo obrigaria o cliente
   * a reordenar o que ja esta em ordem - e convidaria dois clientes a ordenar diferente. A ordem do
   * array e o contrato.
   *
   * @param platform minusculo, igual ao que o front usa em {@code SocialLinks.tsx}
   */
  @Schema(name = "SocialLink")
  public record SocialLinkResponse(
      @Schema(
              example = "github",
              allowableValues = {"github", "linkedin", "email"},
              requiredMode = Schema.RequiredMode.REQUIRED)
          String platform,
      @Schema(example = "https://github.com/CrySamuel", requiredMode = Schema.RequiredMode.REQUIRED)
          String url) {}
}
