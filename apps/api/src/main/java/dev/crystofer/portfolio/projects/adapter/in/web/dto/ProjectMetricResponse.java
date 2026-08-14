package dev.crystofer.portfolio.projects.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Um numero que sustenta o resultado declarado por um projeto.
 *
 * <p>So aparece no detalhe. No card, uma metrica competiria por atencao com o resumo e obrigaria o
 * componente a reservar altura para uma quantidade que varia por projeto - contra a exigencia de
 * altura consistente da Definition of Done do MVP 3.
 *
 * <p>Nao ha {@code displayOrder}, e a omissao repete a de {@code socialLinks}: a lista ja chega
 * ordenada, e expor o numero obrigaria o cliente a reordenar o que ja esta em ordem. <strong>A
 * ordem do array e o contrato.</strong>
 *
 * <p>O valor vai como texto porque carrega unidade, e as unidades nao sao comensuraveis entre si -
 * "80ms", "40%", "R$ 800+". Publicar numero exigiria um campo de unidade ao lado e ainda assim nao
 * cobriria "4h para 2h".
 */
@Schema(name = "ProjectMetric", description = "Numero que sustenta o resultado de um projeto")
public record ProjectMetricResponse(
    @Schema(example = "Economia em um mes", requiredMode = Schema.RequiredMode.REQUIRED)
        String label,
    @Schema(
            description = "Valor com a unidade, como sera exibido",
            example = "R$ 800+",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String value) {}
