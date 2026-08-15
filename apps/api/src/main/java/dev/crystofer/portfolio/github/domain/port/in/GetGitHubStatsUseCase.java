package dev.crystofer.portfolio.github.domain.port.in;

import dev.crystofer.portfolio.github.domain.model.GitHubStats;

/**
 * Porta de entrada: as estatisticas do perfil publico do GitHub.
 *
 * <p><strong>Sem parametro, e a ausencia dele e a decisao.</strong> O portfolio tem um dono, e o
 * nome de usuario e configuracao - {@code GITHUB_USERNAME} - e nao pergunta de quem chama. Aceitar
 * um nome pelo endpoint transformaria a API num proxy publico da API do GitHub: qualquer um
 * consultaria qualquer perfil gastando a cota do token do dono, e o cache de 6h passaria a guardar
 * entradas que ninguem pediu duas vezes.
 *
 * <p><strong>Nunca lanca por indisponibilidade.</strong> Esta e a diferenca desta porta para as
 * outras tres do sistema, e ela vem do ADR-0008: GitHub fora do ar nao e erro do portfolio, e sim
 * um estado previsto. O pior retorno possivel e {@link GitHubStats#empty(String)}, entao quem
 * consome nao tem ramo de falha a escrever.
 */
public interface GetGitHubStatsUseCase {

  /**
   * @return as estatisticas do dono do portfolio, possivelmente vazias
   */
  GitHubStats getGitHubStats();
}
