package dev.crystofer.portfolio.projects.adapter.in.web.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Um item de {@code GET /api/v1/projects}.
 *
 * <p><strong>Nao traz a narrativa, e nao traz os enderecos.</strong> Sao duas omissoes com motivos
 * distintos, e as duas sao decisao e nao esquecimento.
 *
 * <p>{@code problem}, {@code solution} e {@code outcome} ficam de fora porque a listagem nao os
 * exibe: mandar a narrativa completa de todos os projetos para uma tela que mostra o resumo seria
 * pagar banda por texto que ninguem le - e, com ISR, pagar tambem no HTML pre-renderizado.
 *
 * <p>{@code repoUrl} e {@code liveUrl} ficam de fora por acessibilidade. O commit 37 exige que o
 * card tenha <strong>uma unica area de foco</strong>, e link dentro de card que ja e link cria um
 * alvo aninhado que o leitor de tela anuncia duas vezes. Nao publicar os enderecos no resumo torna
 * o card correto por construcao, em vez de depender de o componente lembrar de nao os desenhar.
 *
 * @param coverImage {@code null} quando o projeto nao tem capa
 * @param publishedAt {@code null} quando nao ha data honesta a declarar
 */
@Schema(name = "ProjectSummary", description = "Projeto como aparece na listagem e nos cards")
public record ProjectSummaryResponse(
    @Schema(
            description = "Identificador da URL publica",
            example = "finai",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String slug,
    @Schema(example = "FinAI", requiredMode = Schema.RequiredMode.REQUIRED) String title,
    @Schema(
            description = "Uma ou duas frases, o texto do card",
            example = "Assistente financeiro no Telegram que le texto livre.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String summary,
    @Schema(
            description = "Caminho da imagem de capa; nulo quando nao ha",
            example = "/images/projetos/finai.png",
            types = {"string", "null"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        String coverImage,
    @Schema(
            description = "Verdadeiro para os projetos em destaque na home",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED)
        boolean featured,
    @Schema(
            description = "Nulo quando nao ha data honesta a declarar",
            example = "2026-03-24",
            types = {"string", "null"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate publishedAt,
    @Schema(
            description = "Tecnologias declaradas, em ordem alfabetica; alimenta o filtro",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<TechnologyResponse> technologies) {}
