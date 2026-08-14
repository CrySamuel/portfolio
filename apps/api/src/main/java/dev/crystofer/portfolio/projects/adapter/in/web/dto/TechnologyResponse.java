package dev.crystofer.portfolio.projects.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Uma tecnologia declarada por um projeto.
 *
 * <p>Aparece nas duas respostas - resumo e detalhe -, porque o filtro da listagem depende dela e a
 * pagina de detalhe tambem a exibe.
 *
 * <p>O {@code slug} vai como {@code String}, e nao como objeto envolvendo o valor. O value object
 * existe para o compilador do lado Java; publicar {@code {"value": "java"}} no JSON obrigaria todo
 * cliente a desembrulhar um nivel para chegar ao texto que ele ja teria recebido direto.
 *
 * @param iconSlug {@code null} enquanto nao houver sprite de icones proprio
 */
@Schema(name = "Technology", description = "Tecnologia usada por um projeto")
public record TechnologyResponse(
    @Schema(example = "Spring Boot", requiredMode = Schema.RequiredMode.REQUIRED) String name,
    @Schema(
            description = "Identificador usado no filtro da listagem",
            example = "spring-boot",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String slug,
    @Schema(
            description = "Familia da tecnologia",
            example = "framework",
            allowableValues = {"language", "framework", "database", "infrastructure", "tool"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        String category,
    @Schema(
            description = "Nulo enquanto nao houver sprite de icones",
            example = "spring",
            types = {"string", "null"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        String iconSlug) {}
