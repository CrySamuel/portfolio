package dev.crystofer.portfolio.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import dev.crystofer.portfolio.github.domain.model.GitHubStats;
import dev.crystofer.portfolio.github.domain.model.RepositorySummary;
import dev.crystofer.portfolio.github.domain.port.out.GitHubStatsProviderPort;
import dev.crystofer.portfolio.shared.config.CacheConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * A integracao com o GitHub sob as falhas que o GitHub realmente comete.
 *
 * <p>O teste anterior provou que os aspectos interceptam apontando para uma porta vazia - uma falha
 * so, de conexao. Aqui a origem responde, e responde <em>mal</em>: 403 de cota, 500, demora alem do
 * prazo e corpo que nao vira dominio. Sao os modos de falha que o ADR-0008 nomeia, e nenhum deles
 * se reproduz de forma deterministica contra a API de verdade.
 *
 * <p><strong>O servidor sobe uma vez por JVM, num bloco estatico, e nunca e parado a mao.</strong>
 * E a mesma decisao do Postgres em {@link AbstractIntegrationTest} e pelo mesmo motivo: com ciclo
 * de vida por classe, o contexto do Spring ficaria em cache apontando para uma porta ja morta, e a
 * falha dependeria da ordem de execucao (secao 4.21).
 *
 * <p><strong>O cache e esvaziado antes de cada teste, e sem isso metade deles seria
 * mentira.</strong> A chave e o nome de usuario, entao o retrato guardado pelo primeiro teste
 * responderia a todos os seguintes sem tocar o dublê - e um cenario de falha passaria por nunca ter
 * sido exercido.
 */
class GitHubWireMockTest extends AbstractIntegrationTest {

  private static final String USUARIO = "CrySamuel";

  private static final String PERFIL = "/users/" + USUARIO;

  private static final WireMockServer GITHUB =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());

  static {
    GITHUB.start();
  }

  /**
   * Aponta a aplicacao para o dublê e encurta os prazos.
   *
   * <p>Os numeros de producao - tres segundos de leitura, meio segundo entre tentativas, um minuto
   * de circuito aberto - tornariam esta classe lenta demais para rodar a cada commit. O que se mede
   * aqui e o <em>comportamento</em>; a duracao de cada espera e configuracao, e ja esta declarada
   * no {@code application.yml}.
   */
  @DynamicPropertySource
  static void apontarParaODuble(DynamicPropertyRegistry registry) {
    registry.add("portfolio.github.base-url", GITHUB::baseUrl);
    registry.add("portfolio.github.read-timeout", () -> "300ms");
    registry.add("resilience4j.retry.instances.github.wait-duration", () -> "1ms");
    registry.add(
        "resilience4j.circuitbreaker.instances.github.wait-duration-in-open-state", () -> "200ms");
    registry.add(
        "resilience4j.circuitbreaker.instances.github.permitted-number-of-calls-in-half-open-state",
        () -> "1");
  }

  @Autowired private GitHubStatsProviderPort provider;

  @Autowired private CircuitBreakerRegistry registry;

  @Autowired private CacheManager cacheManager;

  @BeforeEach
  void limparEstado() {
    GITHUB.resetAll();
    registry.circuitBreaker("github").reset();
    cacheManager.getCache(CacheConfig.GITHUB_STATS).clear();
  }

  /**
   * O caminho feliz, que tambem e o unico lugar onde a cadeia inteira e exercida por HTTP.
   *
   * <p>Os dois filtros aparecem no resultado sem serem afirmados diretamente: o fork e o
   * repositorio de perfil estao na resposta do dublê e nao chegam ao dominio.
   */
  @Test
  @DisplayName("deve montar o retrato quando o GitHub responde bem")
  void shouldBuildStats_whenGitHubAnswers() {
    stubPerfil();
    stubRepositorios();
    stubLinguagens("portfolio", "{\"Java\": 750, \"TypeScript\": 250}");
    stubLinguagens("finai", "{\"Python\": 1000}");

    GitHubStats stats = provider.fetchStats(USUARIO);

    assertThat(stats.isEmpty()).isFalse();
    assertThat(stats.publicRepositories()).isEqualTo(17);
    assertThat(stats.repositories())
        .extracting(RepositorySummary::name)
        .containsExactly("portfolio", "finai");

    // Peso por repositorio: o de Python inteiro pesa igual ao de Java com
    // TypeScript, entao Python fica com metade do total.
    long total = stats.totalLanguageWeight();
    assertThat(fatia(stats, "Python", total)).isCloseTo(50.0, within(0.01));
    assertThat(fatia(stats, "Java", total)).isCloseTo(37.5, within(0.01));
    assertThat(fatia(stats, "TypeScript", total)).isCloseTo(12.5, within(0.01));
  }

  /**
   * <strong>403, e nao 429.</strong> E assim que o GitHub avisa que a cota da hora acabou, e a
   * armadilha e essa: quem trata apenas 429 nunca ve o caso mais provavel de indisponibilidade.
   *
   * <p>O 403 conta como falha para o disjuntor e <strong>nao</strong> e retentado - insistir num
   * erro de cota so gasta a cota que ja acabou.
   */
  @Test
  @DisplayName("deve cair para o retrato vazio no 403 de cota, sem retentar")
  void shouldFallBack_onRateLimit() {
    GITHUB.stubFor(
        get(urlPathEqualTo(PERFIL))
            .willReturn(
                aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withHeader("x-ratelimit-remaining", "0")
                    .withBody("{\"message\": \"API rate limit exceeded\"}")));

    assertThat(provider.fetchStats(USUARIO).isEmpty()).isTrue();

    GITHUB.verify(1, getRequestedFor(urlPathEqualTo(PERFIL)));
    assertThat(registry.circuitBreaker("github").getMetrics().getNumberOfFailedCalls())
        .isEqualTo(1);
  }

  /** Erro do servidor e passageiro por definicao, entao a retentativa cobre - tres tentativas. */
  @Test
  @DisplayName("deve retentar tres vezes no 500 e entao devolver o retrato vazio")
  void shouldRetry_onServerError() {
    GITHUB.stubFor(get(urlPathEqualTo(PERFIL)).willReturn(aResponse().withStatus(500)));

    assertThat(provider.fetchStats(USUARIO).isEmpty()).isTrue();

    GITHUB.verify(3, getRequestedFor(urlPathEqualTo(PERFIL)));
  }

  /**
   * Demora e falha, e essa e a razao de existir o prazo de leitura.
   *
   * <p>Sem ele a chamada ficaria pendurada esperando o padrao do sistema operacional - e o
   * disjuntor nunca contaria a falha, porque a requisicao nao termina.
   */
  @Test
  @DisplayName("deve cair para o retrato vazio quando a resposta demora demais")
  void shouldFallBack_onTimeout() {
    // O resto da cadeia responde **bem**, e isso e o que da sentido ao teste. Com
    // o perfil devolvendo `{}` e nenhum dublê para o resto, o retrato sairia
    // vazio de qualquer jeito - por 404 no caminho seguinte - e o teste passaria
    // igual com o prazo de leitura removido, provando nada. Respondendo tudo, o
    // unico caminho para o vazio e a demora.
    GITHUB.stubFor(
        get(urlPathEqualTo(PERFIL))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withFixedDelay(1_500)
                    .withBody("{\"login\":\"CrySamuel\",\"public_repos\":17}")));
    stubRepositorios();
    stubLinguagens("portfolio", "{\"Java\": 100}");
    stubLinguagens("finai", "{\"Python\": 100}");

    assertThat(provider.fetchStats(USUARIO).isEmpty()).isTrue();
  }

  /**
   * Resposta malformada tambem e falha, e o caminho dela e o mais discreto.
   *
   * <p>Aqui o JSON e valido e o <em>conteudo</em> e que nao vira dominio: um repositorio sem data
   * de push. O invariante de {@code RepositorySummary} recusa, a excecao sobe pelo adaptador e o
   * fallback devolve o vazio - a alternativa seria publicar um retrato pela metade sem ninguem
   * saber.
   */
  @Test
  @DisplayName("deve cair para o retrato vazio quando a resposta nao vira dominio")
  void shouldFallBack_onMalformedResponse() {
    stubPerfil();
    GITHUB.stubFor(
        get(urlPathEqualTo(PERFIL + "/repos"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "[{\"name\":\"portfolio\","
                            + "\"html_url\":\"https://github.com/CrySamuel/portfolio\","
                            + "\"description\":null,\"language\":\"Java\","
                            + "\"stargazers_count\":0,\"pushed_at\":null,"
                            + "\"fork\":false,\"archived\":false}]")));

    assertThat(provider.fetchStats(USUARIO).isEmpty()).isTrue();
  }

  /**
   * O ciclo inteiro do disjuntor: fechado, aberto, meio-aberto, fechado de novo.
   *
   * <p>E a metade que faltava. Provar que ele abre mostra que o sistema para de insistir; provar
   * que ele fecha mostra que o sistema <em>volta</em> - sem isso, um disjuntor que abre e nunca
   * mais fecha passaria no teste e deixaria a secao vazia para sempre depois da primeira
   * instabilidade.
   */
  @Test
  @DisplayName("deve abrir o circuito, passar por meio-aberto e fechar quando o GitHub volta")
  void shouldOpenThenCloseCircuit_whenGitHubRecovers() {
    CircuitBreaker circuito = registry.circuitBreaker("github");
    GITHUB.stubFor(get(urlPathEqualTo(PERFIL)).willReturn(aResponse().withStatus(500)));

    for (int tentativa = 0; tentativa < 5; tentativa++) {
      provider.fetchStats(USUARIO);
    }
    assertThat(circuito.getState()).isEqualTo(CircuitBreaker.State.OPEN);

    GITHUB.resetAll();
    stubPerfil();
    stubRepositorios();
    stubLinguagens("portfolio", "{\"Java\": 100}");
    stubLinguagens("finai", "{\"Python\": 100}");

    // A espera e por condicao, e nao por sleep fixo: prazo curto em runner lento
    // e a receita do teste que falha uma vez a cada vinte, sem ninguem saber por
    // que.
    await()
        .atMost(Duration.ofSeconds(5))
        .until(() -> circuito.getState() == CircuitBreaker.State.HALF_OPEN);

    assertThat(provider.fetchStats(USUARIO).isEmpty()).isFalse();
    assertThat(circuito.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  private static void stubPerfil() {
    GITHUB.stubFor(
        get(urlPathEqualTo(PERFIL))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"login\":\"CrySamuel\",\"public_repos\":17}")));
  }

  /**
   * Quatro repositorios, e dois deles nao podem chegar ao dominio: o fork e o de perfil - que aqui
   * tem nove estrelas de proposito, para que ele fosse o primeiro da vitrine se o filtro falhasse.
   */
  private static void stubRepositorios() {
    GITHUB.stubFor(
        get(urlPathEqualTo(PERFIL + "/repos"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "["
                            + repositorio("portfolio", "Java", 3, "2026-08-15", false, false)
                            + ","
                            + repositorio("finai", "Python", 1, "2026-04-15", false, false)
                            + ","
                            + repositorio("CrySamuel", null, 9, "2026-08-16", false, false)
                            + ","
                            + repositorio("copia", "Go", 99, "2026-08-16", true, false)
                            + "]")));
  }

  private static String repositorio(
      String nome, String linguagem, int estrelas, String push, boolean fork, boolean arquivado) {
    return "{\"name\":\""
        + nome
        + "\",\"html_url\":\"https://github.com/CrySamuel/"
        + nome
        + "\",\"description\":null,\"language\":"
        + (linguagem == null ? "null" : "\"" + linguagem + "\"")
        + ",\"stargazers_count\":"
        + estrelas
        + ",\"pushed_at\":\""
        + push
        + "T12:00:00Z\",\"fork\":"
        + fork
        + ",\"archived\":"
        + arquivado
        + "}";
  }

  private static void stubLinguagens(String repositorio, String corpo) {
    GITHUB.stubFor(
        get(urlPathEqualTo("/repos/" + USUARIO + "/" + repositorio + "/languages"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(corpo)));
  }

  private static double fatia(GitHubStats stats, String linguagem, long total) {
    return stats.languages().stream()
        .filter(uso -> uso.name().equals(linguagem))
        .findFirst()
        .map(uso -> uso.shareOf(total))
        .orElse(0.0);
  }
}
