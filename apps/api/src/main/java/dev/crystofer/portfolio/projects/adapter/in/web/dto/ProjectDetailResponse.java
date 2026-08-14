package dev.crystofer.portfolio.projects.adapter.in.web.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A resposta de {@code GET /api/v1/projects/{slug}}.
 *
 * <p>Traz o que o resumo omite: a narrativa completa, os enderecos e as metricas. E o contrato da
 * pagina de detalhe, que e onde a estrutura problema, solucao e resultado aparece na tela.
 *
 * <p><strong>Todo campo e {@code required}, inclusive os quatro que podem vir nulos</strong> - sao
 * coisas diferentes e o cliente tipado precisa das duas. Sem {@code types}, o TypeScript prometeria
 * uma {@code string} onde chega {@code null}, e o componente que decide entre desenhar ou nao o
 * botao do repositorio quebraria no projeto que nao tem um.
 *
 * <p>Nao ha {@code id} nem {@code displayOrder}. Chave tecnica nao e informacao de negocio, e a
 * ordem ja e o proprio arranjo do array - a mesma escolha que {@code socialLinks} fez.
 *
 * @param repoUrl {@code null} quando o projeto nao tem repositorio publico
 * @param liveUrl {@code null} quando nao ha nada no ar para visitar
 * @param coverImage {@code null} quando o projeto nao tem capa
 * @param publishedAt {@code null} quando nao ha data honesta a declarar
 */
@Schema(name = "ProjectDetail", description = "Projeto com a narrativa completa")
public record ProjectDetailResponse(
    @Schema(
            description = "Identificador da URL publica",
            example = "finai",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String slug,
    @Schema(example = "FinAI", requiredMode = Schema.RequiredMode.REQUIRED) String title,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String summary,
    @Schema(description = "O que doia antes", requiredMode = Schema.RequiredMode.REQUIRED)
        String problem,
    @Schema(description = "O que foi construido", requiredMode = Schema.RequiredMode.REQUIRED)
        String solution,
    @Schema(
            description = "O que mudou, de preferencia com numero",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String outcome,
    @Schema(
            description = "Nulo quando nao ha repositorio publico",
            example = "https://github.com/CrySamuel/FinAI-Bot",
            types = {"string", "null"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        String repoUrl,
    @Schema(
            description = "Nulo quando nao ha nada no ar para visitar",
            example = "https://t.me/gestor_crys_bot",
            types = {"string", "null"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        String liveUrl,
    @Schema(
            description = "Caminho da imagem de capa; nulo quando nao ha",
            types = {"string", "null"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        String coverImage,
    @Schema(example = "true", requiredMode = Schema.RequiredMode.REQUIRED) boolean featured,
    @Schema(
            description = "Nulo quando nao ha data honesta a declarar",
            example = "2026-03-24",
            types = {"string", "null"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate publishedAt,
    @Schema(
            description = "Tecnologias declaradas, em ordem alfabetica",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<TechnologyResponse> technologies,
    @Schema(
            description = "Numeros que sustentam o resultado; lista vazia quando nao ha",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<ProjectMetricResponse> metrics) {}
