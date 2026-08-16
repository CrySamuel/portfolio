package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import dev.crystofer.portfolio.github.domain.port.out.GitHubStatsProviderPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * O disjuntor age de verdade - e nao apenas existe.
 *
 * <p><strong>Este teste existe por uma duvida concreta.</strong> O Resilience4j mais novo publicado
 * e o {@code resilience4j-spring-boot3}, feito para o Spring Boot 3, e esta aplicacao roda o Boot
 * 4. O contexto subir nao prova nada: se a autoconfiguracao nao registrasse os aspectos, as
 * anotacoes do adaptador ficariam ali documentando uma protecao inexistente, e a unica forma de
 * descobrir seria em producao, no dia em que o GitHub caisse. Anotacao que nao intercepta e a pior
 * das guardas.
 *
 * <p>O GitHub e apontado para uma porta onde nao ha ninguem, entao toda chamada falha por conexao
 * recusada - falha de rede de verdade, e nao um dublê que devolve excecao. Os cenarios ricos - 403
 * de cota, 500, timeout, resposta malformada e a volta do circuito para fechado - sao do commit 43,
 * com WireMock.
 */
@TestPropertySource(
    properties = {
      "portfolio.github.base-url=http://localhost:1",
      "portfolio.github.connect-timeout=200ms",
      "portfolio.github.read-timeout=200ms",
      // A espera entre tentativas nao e o que este teste mede, e tres segundos
      // de backoff por chamada tornariam a suite lenta sem provar nada a mais.
      "resilience4j.retry.instances.github.wait-duration=1ms"
    })
class GitHubResilienceIntegrationTest extends AbstractIntegrationTest {

  private static final String USUARIO = "CrySamuel";

  @Autowired private GitHubStatsProviderPort provider;

  @Autowired private CircuitBreakerRegistry registry;

  @BeforeEach
  void fecharCircuito() {
    // Cada teste comeca do zero: o registro e um bean de aplicacao, entao o
    // estado sobreviveria de um metodo para o outro e a ordem passaria a
    // importar - que e a armadilha da secao 4.21 noutra roupa.
    registry.circuitBreaker("github").reset();
  }

  /**
   * A promessa da porta: sempre devolve estatisticas.
   *
   * <p>Sem o {@code fallbackMethod} ligado, a excecao de conexao subiria e este teste falharia com
   * erro - que e exatamente o que aconteceria com o visitante.
   */
  @Test
  @DisplayName("deve devolver o retrato vazio quando o GitHub nao responde")
  void shouldFallBack_whenGitHubIsUnreachable() {
    var stats = provider.fetchStats(USUARIO);

    assertThat(stats.isEmpty()).isTrue();
    assertThat(stats.username()).isEqualTo(USUARIO);
  }

  /**
   * O disjuntor abre depois do limiar, e e isto que prova que o aspecto intercepta.
   *
   * <p>Com a retentativa por fora do disjuntor, cada chamada da porta vira tres passagens pelo
   * circuito; cinco chamadas passam com folga do minimo de cinco amostras com 50% de falha.
   */
  @Test
  @DisplayName("deve abrir o circuito depois das falhas seguidas")
  void shouldOpenCircuit_afterRepeatedFailures() {
    CircuitBreaker circuito = registry.circuitBreaker("github");
    assertThat(circuito.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

    for (int tentativa = 0; tentativa < 5; tentativa++) {
      provider.fetchStats(USUARIO);
    }

    assertThat(circuito.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThat(circuito.getMetrics().getNumberOfFailedCalls()).isPositive();
  }

  /**
   * Circuito aberto continua devolvendo o retrato vazio, e sem tocar a rede.
   *
   * <p>E o ponto do disjuntor: depois que o GitHub demonstrou estar fora, insistir custa espera ao
   * visitante e cota a quem ja esta em apuros. A chamada falha na hora, pelo proprio circuito.
   */
  @Test
  @DisplayName("deve seguir devolvendo o retrato vazio com o circuito aberto")
  void shouldKeepFallingBack_whenCircuitIsOpen() {
    CircuitBreaker circuito = registry.circuitBreaker("github");
    circuito.transitionToOpenState();

    var stats = provider.fetchStats(USUARIO);

    assertThat(stats.isEmpty()).isTrue();
    assertThat(circuito.getState()).isEqualTo(CircuitBreaker.State.OPEN);
  }
}
