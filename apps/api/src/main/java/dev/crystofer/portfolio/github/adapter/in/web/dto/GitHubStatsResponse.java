package dev.crystofer.portfolio.github.adapter.in.web.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * O retrato do perfil publico, como a secao de estatisticas o exibe.
 *
 * <p><strong>O total de repositorios publicos e o tamanho da lista sao numeros diferentes, de
 * proposito.</strong> O primeiro conta tudo o que e publico no perfil; a lista traz so os que a
 * secao mostra, ja filtrados - sem fork, sem arquivado e sem o repositorio de README do perfil.
 * Igualar os dois obrigaria a exibir dezenas de cards, e o campo perderia o significado que tem:
 * quantos repositorios existem.
 *
 * <p><strong>Tudo zerado e vazio e uma resposta valida, e nao um erro.</strong> E o que a API
 * devolve quando o GitHub esta fora do ar - a cadeia de fallback do ADR-0008 chegando ao ultimo
 * degrau. Quem consome desenha o estado vazio, e nao um erro: a secao e a unica do portfolio que
 * depende de um terceiro, e o site foi desenhado para funcionar sem ela.
 *
 * @param username dono do perfil
 * @param publicRepositories quantos repositorios publicos existem no total
 * @param contributionsLastYear contribuicoes no ultimo ano; zero quando nao ha token configurado
 * @param languages fatias do grafico, da maior para a menor
 * @param repositories repositorios em destaque, em ordem de relevancia
 */
@Schema(name = "GitHubStats", description = "Estatisticas do perfil publico no GitHub")
public record GitHubStatsResponse(
    @Schema(example = "CrySamuel", requiredMode = Schema.RequiredMode.REQUIRED) String username,
    @Schema(
            description = "Total de repositorios publicos do perfil",
            example = "17",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int publicRepositories,
    @Schema(
            description =
                "Contribuicoes no ultimo ano. Vem zero quando nao ha token, porque esse numero so"
                    + " existe no GraphQL do GitHub, que exige autenticacao",
            example = "240",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int contributionsLastYear,
    @Schema(
            description = "Fatias do grafico, da maior para a menor; vazia quando nao ha dado",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<LanguageShareResponse> languages,
    @Schema(
            description = "Repositorios em destaque; vazia quando nao ha dado",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<RepositoryResponse> repositories) {}
