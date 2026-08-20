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

  /**
   * Busca de novo e substitui o que estiver guardado.
   *
   * <p><strong>E uma operacao diferente de ler, e nao um detalhe de implementacao.</strong> Quem le
   * aceita o retrato guardado - e o que torna a leitura barata. Quem reaquece quer justamente o
   * contrario: ir a origem mesmo havendo entrada viva, porque o objetivo dele e que a proxima
   * leitura encontre algo novo. Sao intencoes opostas, e por isso duas operacoes.
   *
   * <p>Nao lanca, pela mesma razao da outra: o pior desfecho e o retrato anterior seguir servindo.
   *
   * @return as estatisticas recem-buscadas, possivelmente vazias
   */
  GitHubStats refreshGitHubStats();
}
