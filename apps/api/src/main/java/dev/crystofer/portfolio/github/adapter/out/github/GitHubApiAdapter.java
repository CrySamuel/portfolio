package dev.crystofer.portfolio.github.adapter.out.github;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import dev.crystofer.portfolio.github.adapter.out.github.dto.GitHubContributionsResponse;
import dev.crystofer.portfolio.github.adapter.out.github.dto.GitHubRepositoryResponse;
import dev.crystofer.portfolio.github.adapter.out.github.dto.GitHubUserResponse;
import dev.crystofer.portfolio.github.adapter.out.github.mapper.GitHubResponseMapper;
import dev.crystofer.portfolio.github.domain.model.GitHubStats;
import dev.crystofer.portfolio.github.domain.model.LanguageUsage;
import dev.crystofer.portfolio.github.domain.port.out.GitHubStatsProviderPort;
import dev.crystofer.portfolio.shared.config.CacheConfig;
import dev.crystofer.portfolio.shared.config.properties.GitHubProperties;

/**
 * A implementacao da porta de saida: o GitHub de verdade, por HTTP.
 *
 * <p><strong>Um retrato custa varias chamadas, e o cache existe por isso.</strong> Perfil,
 * repositorios e uma consulta de linguagens <em>por repositorio</em> - com os limites padrao, 22
 * requisicoes. Sem cache, cada revalidacao do site gastaria isso; com as seis horas da {@link
 * CacheConfig}, o custo real fica em quatro reaquecimentos por dia, dentro das 60 requisicoes por
 * hora que existem ate sem token.
 *
 * <p><strong>O {@code unless} e o que impede o vazio de congelar.</strong> Sem ele, uma falha
 * passageira seria guardada por seis horas e o site mostraria a secao vazia mesmo depois de o
 * GitHub voltar - o oposto do que a cadeia de fallback do ADR-0008 quer.
 *
 * <p><strong>O {@code catch} largo aqui e andaime, e tem prazo.</strong> A porta promete sempre
 * devolver estatisticas, entao alguem precisa cumprir a promessa desde o primeiro commit que a
 * implementa. No commit 42 ele sai e entram {@code @CircuitBreaker}, {@code @Retry} e
 * {@code @Bulkhead} com {@code fallbackMethod}, que e onde o ADR-0008 poe essa responsabilidade.
 * Ate la, qualquer falha - rede, 403 de cota, JSON malformado, invariante de dominio recusando um
 * repositorio estranho - vira o retrato vazio, e o log guarda a causa.
 */
@Component
class GitHubApiAdapter implements GitHubStatsProviderPort {

  private static final Logger log = LoggerFactory.getLogger(GitHubApiAdapter.class);

  /**
   * O total de contribuicoes do ultimo ano, que <strong>so existe no GraphQL</strong>.
   *
   * <p>Nenhum endpoint REST publica esse numero. E o GraphQL do GitHub exige autenticacao para
   * qualquer consulta, entao esta e a unica parte do retrato que depende do token - sem ele, a
   * chamada nem e tentada, e o dominio recebe zero.
   */
  private static final String CONTRIBUTIONS_QUERY =
      """
      query($login: String!) {
        user(login: $login) {
          contributionsCollection {
            contributionCalendar { totalContributions }
          }
        }
      }
      """;

  private static final ParameterizedTypeReference<List<GitHubRepositoryResponse>> REPOSITORIES =
      new ParameterizedTypeReference<>() {};

  /** {@code GET /repos/{owner}/{repo}/languages} devolve um objeto de nome para bytes. */
  private static final ParameterizedTypeReference<Map<String, Long>> LANGUAGES =
      new ParameterizedTypeReference<>() {};

  private final RestClient rest;
  private final RestClient graphql;
  private final GitHubResponseMapper mapper;
  private final GitHubProperties properties;

  GitHubApiAdapter(
      @Qualifier("gitHubRestClient") RestClient rest,
      @Qualifier("gitHubGraphQlClient") RestClient graphql,
      GitHubResponseMapper mapper,
      GitHubProperties properties) {
    this.rest = rest;
    this.graphql = graphql;
    this.mapper = mapper;
    this.properties = properties;
  }

  @Override
  @Cacheable(cacheNames = CacheConfig.GITHUB_STATS, key = "#username", unless = "#result.isEmpty()")
  public GitHubStats fetchStats(String username) {
    try {
      GitHubUserResponse user = carregarPerfil(username);
      List<GitHubRepositoryResponse> repositorios = carregarRepositorios(username);

      return new GitHubStats(
          user.login(),
          user.publicRepos(),
          carregarContribuicoes(username),
          carregarLinguagens(user.login(), repositorios),
          repositorios.stream().map(mapper::toSummary).toList());
    } catch (Exception falha) {
      // A mensagem diz o tipo e o texto, e nao a pilha: em operacao normal esta
      // linha e o unico sinal de que o GitHub caiu, e ela precisa caber num
      // alerta. A pilha reapareceria a cada revalidacao, seis vezes por dia.
      log.warn("GitHub indisponivel para {}, devolvendo retrato vazio. causa={}", username, falha);
      return GitHubStats.empty(username);
    }
  }

  private GitHubUserResponse carregarPerfil(String username) {
    return rest.get().uri("/users/{username}", username).retrieve().body(GitHubUserResponse.class);
  }

  /**
   * Os repositorios do perfil, sem os que nao contam.
   *
   * <p>Fork sai porque nao e codigo escrito pela pessoa - deixa-lo entrar faria a distribuicao de
   * linguagens do perfil somar a de outra gente. Arquivado sai da vitrine pelo motivo oposto: ele e
   * dela, mas nao representa o que ela mantem hoje, e ocuparia lugar de quem representa.
   *
   * <p>A ordenacao pedida e {@code pushed}, e nao {@code updated} como a secao 9.2 escreve. {@code
   * updated_at} muda com edicao de descricao e com mudanca de visibilidade; {@code pushed_at} muda
   * quando ha codigo novo, que e o que a lista quer dizer. Como o dominio desempata por data de
   * push, pedir a mesma ordem evita cortar os 30 primeiros por um criterio e exibi-los por outro.
   */
  private List<GitHubRepositoryResponse> carregarRepositorios(String username) {
    List<GitHubRepositoryResponse> resposta =
        rest.get()
            .uri(
                uri ->
                    uri.path("/users/{username}/repos")
                        .queryParam("type", "owner")
                        .queryParam("sort", "pushed")
                        .queryParam("direction", "desc")
                        .queryParam("per_page", properties.repositoriesToLoad())
                        .build(username))
            .retrieve()
            .body(REPOSITORIES);

    if (resposta == null) {
      return List.of();
    }
    return resposta.stream()
        .filter(repositorio -> !repositorio.fork() && !repositorio.archived())
        .toList();
  }

  /**
   * As linguagens somadas byte a byte, custando uma requisicao por repositorio.
   *
   * <p>E a parte cara do retrato, e por isso o limite e configuravel e menor que o numero de
   * repositorios carregados. A alternativa barata seria contar o campo {@code language} de cada
   * repositorio - uma requisicao no total -, mas ele so diz a linguagem predominante: um projeto
   * Java com metade de TypeScript apareceria como Java puro, e o grafico viraria uma contagem de
   * repositorios disfarcada de distribuicao de codigo.
   */
  private List<LanguageUsage> carregarLinguagens(
      String owner, List<GitHubRepositoryResponse> repositorios) {
    Map<String, Long> soma = new LinkedHashMap<>();

    for (GitHubRepositoryResponse repositorio :
        repositorios.stream().limit(properties.repositoriesForLanguages()).toList()) {
      Map<String, Long> bytes =
          rest.get()
              .uri("/repos/{owner}/{repo}/languages", owner, repositorio.name())
              .retrieve()
              .body(LANGUAGES);

      if (bytes != null) {
        bytes.forEach((linguagem, quantidade) -> soma.merge(linguagem, quantidade, Long::sum));
      }
    }

    return mapper.toLanguages(soma);
  }

  /**
   * As contribuicoes do ultimo ano, quando ha token.
   *
   * <p>Sem token a chamada nao e tentada: o GraphQL do GitHub responde 401 a qualquer consulta
   * anonima, e uma falha previsivel nao deve virar uma linha de aviso a cada reaquecimento em
   * desenvolvimento. O dominio recebe zero, e zero e o que a secao interpreta como "nao ha numero a
   * mostrar".
   */
  private int carregarContribuicoes(String username) {
    if (!properties.hasToken()) {
      return 0;
    }

    GitHubContributionsResponse resposta =
        graphql
            .post()
            .body(Map.of("query", CONTRIBUTIONS_QUERY, "variables", Map.of("login", username)))
            .retrieve()
            .body(GitHubContributionsResponse.class);

    return resposta == null ? 0 : resposta.totalContributions();
  }
}
