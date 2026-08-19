package dev.crystofer.portfolio.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Publica o estado do disjuntor, da retentativa e do bulkhead como metrica.
 *
 * <p><strong>Isto deveria ser automatico, e nao e - mais uma do Boot 4.</strong> O {@code
 * resilience4j-micrometer} traz as seis autoconfiguracoes de metrica, e todas as seis carregam.
 * Todas desistem no mesmo ponto:
 *
 * <pre>
 * CircuitBreakerMetricsAutoConfiguration#taggedCircuitBreakerMetricsPublisher
 *   X &#64;ConditionalOnBean (types: io.micrometer.core.instrument.MeterRegistry)
 *     did not find any beans of type io.micrometer.core.instrument.MeterRegistry
 * </pre>
 *
 * <p>O {@code MeterRegistry} existe - o {@code /actuator/prometheus} publica 245 linhas de metrica
 * sem ele nao publicaria nenhuma. O que quebrou foi a <em>ordem</em>: aquelas autoconfiguracoes
 * declaram {@code @AutoConfigureAfter} apontando para as classes de autoconfiguracao do actuator do
 * Boot 3, e a modularizacao do Boot 4 mudou os pacotes delas. Referencia que nao resolve nao e erro
 * no {@code @AutoConfigureAfter} - e ignorada -, entao elas rodam <strong>antes</strong> do
 * registro existir e o {@code @ConditionalOnBean} nao acha nada.
 *
 * <p><strong>O modo de falhar e o que torna isto perigoso:</strong> nada reprova, nada avisa, e o
 * {@code /actuator/prometheus} responde 200 com todo o resto no lugar. A ausencia so aparece para
 * quem procura a familia {@code resilience4j_*} e nao encontra - foi assim que ela foi descoberta,
 * conferindo em producao um item da Definition of Done que estava marcado como pronto.
 *
 * <p>A saida e declarar os publicadores a mao. Como {@code MeterBinder}, eles nao dependem de ordem
 * nenhuma: o proprio Boot liga todo bean desse tipo ao registro depois que ele existe, que e
 * exatamente a garantia que o {@code @ConditionalOnBean} tentava obter e nao obteve.
 *
 * <p>Fica em {@code shared} porque nao conhece modulo algum: liga <em>todas</em> as instancias
 * registradas, e hoje ha uma so - a {@code github}.
 */
@Configuration(proxyBeanMethods = false)
class ResilienceMetricsConfig {

  /**
   * Estado do circuito, chamadas por resultado e tempo de resposta.
   *
   * <p>O medidor de estado e o que importa para operar: {@code resilience4j_circuitbreaker_state}
   * sai com uma serie por estado e vale 1 no estado corrente. E o que permite alertar em "circuito
   * aberto" sem abrir o log da plataforma.
   */
  @Bean
  MeterBinder circuitBreakerMetrics(CircuitBreakerRegistry registry) {
    return TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry);
  }

  /**
   * Tentativas por resultado.
   *
   * <p>Distingue "deu certo de primeira" de "deu certo na terceira", que sao a mesma resposta para
   * o visitante e situacoes bem diferentes para quem opera.
   */
  @Bean
  MeterBinder retryMetrics(RetryRegistry registry) {
    return TaggedRetryMetrics.ofRetryRegistry(registry);
  }

  /** Chamadas simultaneas disponiveis - o sinal de que o limite de concorrencia foi atingido. */
  @Bean
  MeterBinder bulkheadMetrics(BulkheadRegistry registry) {
    return TaggedBulkheadMetrics.ofBulkheadRegistry(registry);
  }
}
