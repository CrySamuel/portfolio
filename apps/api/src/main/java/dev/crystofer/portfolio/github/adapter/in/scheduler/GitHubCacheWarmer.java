package dev.crystofer.portfolio.github.adapter.in.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.crystofer.portfolio.github.domain.port.in.GetGitHubStatsUseCase;

/**
 * Reaquece o cache antes que ele expire.
 *
 * <p><strong>E um adaptador de entrada</strong>, como o controlador - a diferenca e quem dispara.
 * La e uma requisicao HTTP, aqui e o relogio. Por isso ele fala com o caso de uso, e nao com a
 * porta de saida: se falasse direto com o adaptador do GitHub, o cache seria preenchido com uma
 * chave que ninguem consulta no dia em que o nome de usuario mudasse de origem.
 *
 * <p><strong>O intervalo e menor que o prazo do cache, e nao igual.</strong> Com os dois iguais,
 * cada reaquecimento chegaria no instante em que a entrada expira - e qualquer atraso de segundos
 * deixaria uma janela em que a proxima visita paga o custo das 22 requisicoes. Cinco horas contra
 * seis dao uma hora de folga, e o custo dessa folga e uma chamada a mais por dia.
 *
 * <p>Nao ha tratamento de erro aqui, e nao e esquecimento: o caso de uso nao lanca. Se o GitHub
 * estiver fora, o retrato vazio simplesmente nao entra no cache - o {@code unless} do adaptador
 * cuida disso - e a entrada anterior continua servindo ate a proxima tentativa.
 */
@Component
class GitHubCacheWarmer {

  private static final Logger log = LoggerFactory.getLogger(GitHubCacheWarmer.class);

  private final GetGitHubStatsUseCase getGitHubStats;

  GitHubCacheWarmer(GetGitHubStatsUseCase getGitHubStats) {
    this.getGitHubStats = getGitHubStats;
  }

  /**
   * O primeiro reaquecimento acontece um minuto depois do boot, e nao no boot.
   *
   * <p>O servico do plano gratuito hiberna e volta com frequencia. Chamar o GitHub durante a
   * inicializacao atrasaria a subida - e a plataforma tem prazo para considerar o servico vivo -,
   * alem de gastar cota toda vez que a instancia acorda.
   */
  @Scheduled(initialDelay = 60_000, fixedDelay = 5 * 60 * 60 * 1_000)
  void reaquecer() {
    var stats = getGitHubStats.getGitHubStats();
    log.info(
        "Cache do GitHub reaquecido para {}: {} repositorios, {} linguagens",
        stats.username(),
        stats.repositories().size(),
        stats.languages().size());
  }
}
