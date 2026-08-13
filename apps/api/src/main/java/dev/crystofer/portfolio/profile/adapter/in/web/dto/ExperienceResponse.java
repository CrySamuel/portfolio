package dev.crystofer.portfolio.profile.adapter.in.web.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Um item de {@code GET /api/v1/experiences}.
 *
 * <p>DTO separado do dominio pela mesma razao de {@link ProfileResponse}: serializar o modelo
 * amarraria o contrato publico a decisoes internas.
 *
 * <p><strong>Todo campo e {@code required}, inclusive o que pode vir nulo</strong> - sao coisas
 * diferentes e o cliente tipado precisa das duas. Aqui isso pesa mais do que no perfil, porque o
 * campo nulavel <em>carrega significado</em>: {@code endDate} ausente e o que define cargo atual.
 * Publicado como {@code string} simples, o TypeScript prometeria uma data que nunca chega, e o
 * componente que desenha o badge "Atual" quebraria justamente na posicao mais importante da
 * timeline.
 *
 * <p>Nao ha campo {@code current} booleano, e a omissao e deliberada. Ele seria derivavel de {@code
 * endDate}, e duas fontes para o mesmo fato e um lugar onde elas podem divergir - a mesma escolha
 * que a coluna e o dominio ja fazem.
 *
 * <p>Nao ha {@code id}. Chave tecnica de persistencia nao e informacao de negocio, e publicar uma
 * convidaria clientes a guardar referencias a linhas que so a migracao controla.
 *
 * <p>As datas nao declaram {@code format}, e a ausencia foi verificada. Supunha-se que sobrescrever
 * o tipo com {@code types} fizesse o springdoc perder o {@code format: date} que ele infere de
 * {@code LocalDate} - nao faz. O {@code OpenApiContractTest} confere as duas datas e e quem
 * perceberia se uma versao futura mudasse isso.
 *
 * @param endDate {@code null} enquanto a posicao for a atual
 */
@Schema(name = "Experience", description = "Uma passagem profissional da timeline")
public record ExperienceResponse(
    @Schema(example = "Acme", requiredMode = Schema.RequiredMode.REQUIRED) String company,
    @Schema(example = "Desenvolvedor Backend", requiredMode = Schema.RequiredMode.REQUIRED)
        String role,
    @Schema(example = "2022-03-01", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate startDate,
    @Schema(
            description = "Nulo significa cargo atual",
            example = "2024-01-31",
            types = {"string", "null"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate endDate,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
    @Schema(
            description = "Destaques da posicao; lista vazia quando nao ha",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> highlights) {}
