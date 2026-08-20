package dev.crystofer.portfolio.github.domain.port.out;

import dev.crystofer.portfolio.github.domain.model.GitHubStats;

/**
 * Porta de saida: obter as estatisticas de um perfil na origem.
 *
 * <p><strong>Chama-se <em>provider</em>, e nao <em>load</em> como as outras portas de
 * saida</strong> - e a diferenca de nome carrega a diferenca de natureza. As demais leem o banco do
 * proprio sistema, que ou responde ou derruba a aplicacao inteira. Esta atravessa a internet ate um
 * terceiro que tem limite de requisicoes, responde <strong>403 quando a cota acaba</strong> - e nao
 * 429, o que engana implementacao ingenua - e cai de vez em quando.
 *
 * <p><strong>O contrato e que ela sempre devolve estatisticas.</strong> Nao declara excecao, e isso
 * nao e otimismo: e onde o ADR-0008 poe a fronteira. Timeout, retentativa, disjuntor e a cadeia de
 * fallback - cache valido, cache expirado, {@link GitHubStats#empty(String)} - ficam todos no
 * adaptador que implementa esta interface, entao a camada de aplicacao nao tem ramo de falha e o
 * dominio nao conhece {@code Resilience4j}. Se a resiliencia vazasse para ca, cada chamador
 * escreveria a propria versao dela.
 *
 * <p>O nome de usuario e parametro, e nao configuracao lida aqui dentro, porque configuracao e
 * assunto da camada de aplicacao - o dominio nao le {@code @Value}. E o mesmo motivo pelo qual a
 * porta de entrada nao tem parametro nenhum: quem sabe de quem sao as estatisticas e o servico, no
 * meio.
 */
public interface GitHubStatsProviderPort {

  /**
   * @param username perfil publico a consultar
   * @return as estatisticas, possivelmente vazias quando a origem esta indisponivel
   */
  GitHubStats fetchStats(String username);

  /**
   * O mesmo retrato, buscado <strong>sem consultar o que ja esta guardado</strong>.
   *
   * <p>Existe porque {@link #fetchStats(String)} nao serve para reaquecer: a entrada guardada ainda
   * esta viva quando o reaquecimento passa - de proposito, para nao haver janela sem retrato -,
   * entao aquela chamada receberia um acerto e nao buscaria nada. Um componente que promete
   * reaquecer e recebe acerto e um componente que nao faz nada.
   *
   * <p>Falha aqui <strong>preserva</strong> o retrato anterior, e nao o apaga: quem chama continua
   * servindo o que tinha ate a proxima tentativa. E por isso que a operacao substitui a entrada
   * depois de ter sucesso, em vez de invalida-la antes de tentar.
   *
   * @param username perfil publico a consultar
   * @return as estatisticas recem-buscadas, possivelmente vazias
   */
  GitHubStats refreshStats(String username);
}
