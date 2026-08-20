package dev.crystofer.portfolio.github.adapter.out.github;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CachePut;
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
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

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
 * <p><strong>A resiliencia esta nas anotacoes, e nao num {@code catch}.</strong> O andaime do
 * commit anterior saiu: a retentativa cobre a falha passageira, o disjuntor para de bater na porta
 * de quem ja caiu, o bulkhead limita chamadas simultaneas e o {@code fallbackMethod} garante a
 * promessa da porta - sempre devolve estatisticas. Os numeros de cada um vivem no {@code
 * application.yml}, que e onde se ajustam sem recompilar.
 *
 * <p>A ordem em que os aspectos se aninham importa e esta declarada: <strong>o cache fica por
 * fora</strong> (ver {@code CacheConfig}), entao um acerto de cache nao passa pelo disjuntor. Se
 * passasse, seis horas de respostas cacheadas contariam como sucesso e limpariam a estatistica de
 * falha do circuito - que ficaria fechado sobre um GitHub que caiu.
 *
 * <p><strong>E pela mesma ordem que o {@code fallbackMethod} mora no {@code @Retry}, e nao no
 * {@code @CircuitBreaker}.</strong> O Resilience4j aninha a retentativa <em>por fora</em> do
 * disjuntor. Com o fallback no disjuntor, o retrato vazio era devolvido antes de a excecao chegar a
 * retentativa - que via uma chamada bem-sucedida e nunca retentava coisa alguma. As {@code
 * max-attempts: 3} e a lista {@code retry-exceptions} do {@code application.yml} descreviam um
 * comportamento que nao existia, e foi o teste do 500 que mostrou: uma requisicao onde a
 * configuracao prometia tres. <strong>O fallback pertence ao aspecto de fora</strong>, que e o
 * unico ponto de onde se ve a cadeia inteira ja esgotada.
 */
@Component
class GitHubApiAdapter implements GitHubStatsProviderPort {

  private static final Logger log = LoggerFactory.getLogger(GitHubApiAdapter.class);

  /**
   * O nome das tres instancias do Resilience4j, que e o mesmo nome nas tres.
   *
   * <p>Constante, e nao string repetida: o nome liga a anotacao ao bloco do {@code application.yml}
   * por texto, e texto que nao casa nao da erro - o Resilience4j cria uma instancia com os valores
   * padrao dele e segue. O disjuntor existiria com outra configuracao que ninguem escolheu.
   */
  private static final String INSTANCIA = "github";

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
  @CircuitBreaker(name = INSTANCIA)
  @Retry(name = INSTANCIA, fallbackMethod = "retratoVazio")
  @Bulkhead(name = INSTANCIA)
  public GitHubStats fetchStats(String username) {
    return montarRetrato(username);
  }

  /**
   * O reaquecimento, que difere do anterior em uma anotacao e em tudo.
   *
   * <p><strong>{@code @CachePut} no lugar de {@code @Cacheable}</strong>: o metodo executa
   * <em>sempre</em> e o resultado substitui a entrada. E o que faz o agendado de cinco horas
   * significar alguma coisa - com {@code @Cacheable} ele encontraria a entrada de seis horas ainda
   * viva, receberia um acerto e nao buscaria nada.
   *
   * <p><strong>Substituir depois, em vez de invalidar antes.</strong> Um {@code @CacheEvict}
   * seguido de busca deixaria o cache vazio durante a tentativa, e uma falha nesse intervalo
   * apagaria o retrato anterior - exatamente o que a cadeia do ADR-0008 existe para evitar. Aqui a
   * entrada velha so sai quando ha uma nova, e o {@code unless} garante que retrato vazio nunca
   * substitua um retrato bom.
   */
  @Override
  @CachePut(cacheNames = CacheConfig.GITHUB_STATS, key = "#username", unless = "#result.isEmpty()")
  @CircuitBreaker(name = INSTANCIA)
  @Retry(name = INSTANCIA, fallbackMethod = "retratoVazio")
  @Bulkhead(name = INSTANCIA)
  public GitHubStats refreshStats(String username) {
    return montarRetrato(username);
  }

  /** O retrato em si, sem cache e sem resiliencia - os dois moram nas anotacoes acima. */
  private GitHubStats montarRetrato(String username) {
    GitHubUserResponse user = carregarPerfil(username);
    List<GitHubRepositoryResponse> repositorios = carregarRepositorios(username);

    return new GitHubStats(
        user.login(),
        user.publicRepos(),
        carregarContribuicoes(username),
        carregarLinguagens(user.login(), repositorios),
        repositorios.stream().map(mapper::toSummary).toList());
  }

  /**
   * O ultimo degrau da cadeia de fallback do ADR-0008.
   *
   * <p>Os degraus anteriores acontecem antes de chegar aqui: cache valido devolve sem executar
   * nada, e a retentativa cobre a falha passageira. Este metodo e o que sobra quando tudo falhou -
   * e o que transforma "o GitHub caiu" em "a secao aparece vazia", em vez de em erro na pagina.
   *
   * <p><strong>Recebe {@code Throwable}, e nao {@code Exception}</strong>, porque e assim que o
   * Resilience4j casa o fallback: uma assinatura mais estreita nao e encontrada, o aspecto lanca a
   * excecao original e a protecao inteira vira decoracao - silenciosamente.
   *
   * <p>O log diz o tipo e o texto da causa, e nao a pilha: em operacao normal esta linha e o unico
   * sinal de que o GitHub caiu, e ela precisa caber num alerta. A pilha reapareceria a cada
   * reaquecimento, quatro vezes por dia.
   */
  GitHubStats retratoVazio(String username, Throwable causa) {
    log.warn("GitHub indisponivel para {}, devolvendo retrato vazio. causa={}", username, causa);
    return GitHubStats.empty(username);
  }

  private GitHubUserResponse carregarPerfil(String username) {
    return rest.get().uri("/users/{username}", username).retrieve().body(GitHubUserResponse.class);
  }

  /**
   * Um repositorio conta como projeto da vitrine?
   *
   * <p>Tres exclusoes, por tres motivos diferentes:
   *
   * <ul>
   *   <li><strong>fork</strong> nao e codigo escrito pela pessoa, e deixa-lo entrar faria a
   *       distribuicao de linguagens do perfil somar a de outra gente;
   *   <li><strong>arquivado</strong> e dela, mas nao representa o que ela mantem hoje, e ocuparia
   *       lugar de quem representa;
   *   <li><strong>o repositorio de perfil</strong> - aquele cujo nome e igual ao do usuario - nao e
   *       projeto nenhum: e a convencao do GitHub para o README que aparece no topo do perfil.
   *       Medido, ele chegava em <em>primeiro</em> na ordem de destaque, porque tem uma estrela e o
   *       criterio comeca por estrelas.
   * </ul>
   *
   * <p>Metodo estatico e visivel no pacote de proposito: as tres regras sao editoriais e mereciam
   * teste antes do commit 43, que e quem traz o WireMock e exercita a chamada HTTP inteira.
   */
  static boolean contaComoProjeto(GitHubRepositoryResponse repositorio, String username) {
    return !repositorio.fork()
        && !repositorio.archived()
        && !repositorio.name().equalsIgnoreCase(username);
  }

  /**
   * Os repositorios do perfil, sem os que nao contam - ver {@link #contaComoProjeto}.
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
        .filter(repositorio -> contaComoProjeto(repositorio, username))
        .toList();
  }

  /**
   * As linguagens de cada repositorio, uma requisicao por repositorio.
   *
   * <p>E a parte cara do retrato, e por isso o limite e configuravel e menor que o numero de
   * repositorios carregados. A alternativa barata seria contar o campo {@code language} de cada
   * repositorio - uma requisicao no total -, mas ele so diz a linguagem predominante: um projeto
   * Java com metade de TypeScript apareceria como Java puro, e o grafico viraria uma contagem de
   * repositorios disfarcada de distribuicao de codigo.
   *
   * <p><strong>O adaptador coleta e nao soma.</strong> Quem combina os mapas e {@link
   * LanguageUsage#averagingByRepository(List)}, porque "cada repositorio pesa igual" e regra sobre
   * como o portfolio se descreve - e nao detalhe de como o GitHub entrega o dado.
   */
  private List<LanguageUsage> carregarLinguagens(
      String owner, List<GitHubRepositoryResponse> repositorios) {
    List<Map<String, Long>> porRepositorio = new ArrayList<>();

    for (GitHubRepositoryResponse repositorio :
        repositorios.stream().limit(properties.repositoriesForLanguages()).toList()) {
      Map<String, Long> bytes =
          rest.get()
              .uri("/repos/{owner}/{repo}/languages", owner, repositorio.name())
              .retrieve()
              .body(LANGUAGES);

      if (bytes != null) {
        porRepositorio.add(bytes);
      }
    }

    return LanguageUsage.averagingByRepository(porRepositorio);
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
