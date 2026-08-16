package dev.crystofer.portfolio.github.adapter.in.web.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Um repositorio em destaque.
 *
 * <p>Descricao e linguagem principal sao <strong>obrigatorias e nulaveis</strong> - a chave sempre
 * vem, o valor pode ser nulo. E a mesma decisao de {@code repoUrl} no catalogo de projetos, e ela
 * existe porque os dois casos acontecem de verdade: repositorio sem descricao e comum, e
 * repositorio so com arquivos de configuracao nao tem linguagem detectada. Com a chave sempre
 * presente, o tipo gerado no cliente TypeScript sai como {@code string | null} e obriga quem
 * consome a tratar o caso.
 *
 * @param name nome do repositorio
 * @param description descricao curta; nulo quando nao ha
 * @param url endereco publico
 * @param primaryLanguage linguagem predominante; nulo quando o GitHub nao detecta nenhuma
 * @param stars estrelas recebidas
 * @param lastPushedAt data do ultimo push, que e o que distingue projeto vivo de parado
 */
@Schema(name = "Repository", description = "Repositorio publico em destaque")
public record RepositoryResponse(
    @Schema(example = "portfolio", requiredMode = Schema.RequiredMode.REQUIRED) String name,
    @Schema(
            description = "Descricao curta; nulo quando nao ha",
            example = "Monorepo full-stack com API Java em producao",
            requiredMode = Schema.RequiredMode.REQUIRED,
            types = {"string", "null"})
        String description,
    @Schema(
            example = "https://github.com/CrySamuel/portfolio",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String url,
    @Schema(
            description = "Linguagem predominante; nulo quando o GitHub nao detecta nenhuma",
            example = "Java",
            requiredMode = Schema.RequiredMode.REQUIRED,
            types = {"string", "null"})
        String primaryLanguage,
    @Schema(example = "3", requiredMode = Schema.RequiredMode.REQUIRED) int stars,
    @Schema(
            description = "Data do ultimo push",
            example = "2026-08-15",
            requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate lastPushedAt) {}
