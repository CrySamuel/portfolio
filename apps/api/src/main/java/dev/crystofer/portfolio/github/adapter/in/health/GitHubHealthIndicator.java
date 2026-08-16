package dev.crystofer.portfolio.github.adapter.in.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * O estado do circuito do GitHub, exposto como indicador de saude.
 *
 * <p><strong>Nunca reprova o health check, e essa e a decisao central.</strong> O GitHub e uma
 * dependencia opcional: o site funciona inteiro sem ele, com a secao vazia. Se este indicador
 * respondesse {@code DOWN}, o {@code /actuator/health} inteiro cairia, a plataforma concluiria que
 * a aplicacao esta doente e a reiniciaria - derrubando o portfolio por causa de um servico de
 * terceiro que ele foi desenhado para sobreviver sem.
 *
 * <p>O que ele faz e informar. O estado do circuito e o numero de falhas vao no detalhe, para que a
 * pergunta "o GitHub esta respondendo?" tenha resposta sem ninguem ler log - e o alerta, se um dia
 * existir, vem das metricas do Prometheus, que e onde alerta deve morar.
 *
 * <p>Vive no modulo {@code github}, e nao em {@code shared} como a secao 16 do plano escreve. A
 * regra de fronteira do ArchUnit permite qualquer modulo depender de {@code shared} e proibe o
 * contrario: um indicador em {@code shared} que conhece o circuito do GitHub inverteria a seta e
 * reprovaria o build. E um adaptador de entrada como qualquer outro - quem pergunta e o actuator.
 */
@Component("github")
class GitHubHealthIndicator implements HealthIndicator {

  private final CircuitBreakerRegistry registry;

  GitHubHealthIndicator(CircuitBreakerRegistry registry) {
    this.registry = registry;
  }

  @Override
  public Health health() {
    CircuitBreaker circuito = registry.circuitBreaker("github");
    var metricas = circuito.getMetrics();

    return Health.up()
        .withDetail("circuito", circuito.getState().name())
        .withDetail("taxaDeFalha", metricas.getFailureRate())
        .withDetail("chamadasComFalha", metricas.getNumberOfFailedCalls())
        .withDetail("chamadasComSucesso", metricas.getNumberOfSuccessfulCalls())
        .build();
  }
}
