package dev.crystofer.portfolio.github.adapter.in.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

class GitHubHealthIndicatorTest {

  private final CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
  private final GitHubHealthIndicator indicator = new GitHubHealthIndicator(registry);

  @Test
  @DisplayName("deve informar o estado do circuito fechado")
  void shouldReport_closedCircuit() {
    var health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("circuito", "CLOSED");
  }

  /**
   * A decisao central deste indicador: circuito aberto <strong>nao</strong> derruba o health check.
   *
   * <p>O GitHub e dependencia opcional - o site funciona inteiro sem ele, com a secao vazia. Um
   * {@code DOWN} aqui derrubaria o {@code /actuator/health} inteiro, a plataforma concluiria que a
   * aplicacao esta doente e a reiniciaria: o portfolio sairia do ar por causa de um servico de
   * terceiro que ele foi desenhado para sobreviver sem.
   */
  @Test
  @DisplayName("deve seguir UP mesmo com o circuito aberto")
  void shouldStayUp_whenCircuitIsOpen() {
    registry.circuitBreaker("github").transitionToOpenState();

    var health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("circuito", "OPEN");
  }

  @Test
  @DisplayName("deve publicar os contadores que respondem se o GitHub esta respondendo")
  void shouldPublish_counters() {
    assertThat(indicator.health().getDetails())
        .containsKeys("taxaDeFalha", "chamadasComFalha", "chamadasComSucesso");
  }
}
