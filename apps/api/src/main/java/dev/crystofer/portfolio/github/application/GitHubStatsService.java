package dev.crystofer.portfolio.github.application;

import org.springframework.stereotype.Service;

import dev.crystofer.portfolio.github.domain.model.GitHubStats;
import dev.crystofer.portfolio.github.domain.port.in.GetGitHubStatsUseCase;
import dev.crystofer.portfolio.github.domain.port.out.GitHubStatsProviderPort;
import dev.crystofer.portfolio.shared.config.properties.GitHubProperties;

/**
 * O caso de uso das estatisticas do GitHub.
 *
 * <p><strong>E o servico mais curto do sistema, e a brevidade e o ponto.</strong> Ele existe para
 * decidir <em>de quem</em> sao as estatisticas - o dono do portfolio, vindo da configuracao - e
 * nada mais. Sem ele, ou a porta de entrada receberia o nome de usuario como parametro,
 * transformando a API num proxy publico da API do GitHub, ou o dominio leria configuracao, o que a
 * regra do ArchUnit proibe.
 *
 * <p><strong>Nao ha try, nem tratamento de ausencia.</strong> A porta de saida promete sempre
 * devolver estatisticas - a resiliencia inteira vive no adaptador, como o ADR-0008 determina -,
 * entao aqui nao existe ramo de falha a escrever. E o oposto do {@code ProfileService}, que lanca
 * quando nao ha perfil: perfil ausente e sistema quebrado, GitHub fora do ar e terca-feira.
 */
@Service
class GitHubStatsService implements GetGitHubStatsUseCase {

  private final GitHubStatsProviderPort provider;
  private final GitHubProperties properties;

  GitHubStatsService(GitHubStatsProviderPort provider, GitHubProperties properties) {
    this.provider = provider;
    this.properties = properties;
  }

  @Override
  public GitHubStats getGitHubStats() {
    return provider.fetchStats(properties.username());
  }

  @Override
  public GitHubStats refreshGitHubStats() {
    return provider.refreshStats(properties.username());
  }
}
