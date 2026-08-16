package dev.crystofer.portfolio.github.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Uma fatia do grafico de linguagens.
 *
 * <p><strong>Publica porcentagem, e nao o peso interno.</strong> O peso e uma unidade do dominio -
 * um milhao de pontos por repositorio, repartidos entre as linguagens dele - e nao significa nada
 * fora dali. Expo-lo obrigaria o cliente a somar tudo e dividir, ou seja, a repetir uma conta que
 * este lado ja fez; e dois clientes podem somar diferente.
 *
 * <p>O numero vem sem arredondar. Onde absorver a diferenca que impede a soma de fechar exatamente
 * em 100% e decisao de quem desenha o grafico, e ela muda conforme a forma do desenho.
 *
 * @param name nome da linguagem, com a capitalizacao que o GitHub usa
 * @param share fatia em porcentagem
 */
@Schema(name = "LanguageShare", description = "Participacao de uma linguagem no perfil")
public record LanguageShareResponse(
    @Schema(example = "Java", requiredMode = Schema.RequiredMode.REQUIRED) String name,
    @Schema(
            description = "Fatia em porcentagem, sem arredondar; a soma das fatias fecha em 100",
            example = "36.4",
            requiredMode = Schema.RequiredMode.REQUIRED)
        double share) {}
