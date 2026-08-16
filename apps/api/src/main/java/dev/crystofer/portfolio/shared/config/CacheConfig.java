package dev.crystofer.portfolio.shared.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * O cache de aplicacao - o nivel L2 da secao 3.9.
 *
 * <p>Uma unica regiao por enquanto, e ela guarda o retrato do GitHub. As leituras do proprio banco
 * <strong>nao</strong> sao cacheadas aqui de proposito: elas ja tem duas camadas na frente - o HTML
 * pre-renderizado na CDN e o cache de {@code fetch} do Next -, entao um terceiro nivel adicionaria
 * invalidacao a resolver sem tirar carga de ninguem.
 *
 * <p><strong>Seis horas, e o numero vem do outro lado.</strong> A cota do GitHub e por hora, e o
 * conteudo muda em ritmo de dias; com esse prazo, um perfil consumido o dia inteiro custa quatro
 * reaquecimentos. O reaquecimento agendado que evita o visitante encontrar a janela expirada e do
 * commit 42.
 *
 * <p>As estatisticas ficam ativadas <strong>por tempo de escrita</strong>, e nao por ultimo acesso:
 * dado velho de seis horas e aceitavel, mas dado velho de seis horas <em>que se renova a cada
 * visita</em> nunca seria atualizado num site de trafego constante.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  /** O nome e usado no {@code @Cacheable} do adaptador e nas metricas do actuator. */
  public static final String GITHUB_STATS = "github-stats";

  private static final Duration TTL = Duration.ofHours(6);

  /**
   * Um perfil, uma entrada. O teto existe para que um dia com muitas chaves - o que so aconteceria
   * por engano, ja que o nome de usuario e configuracao - nao vire memoria retida numa instancia de
   * 512 MB.
   */
  private static final int MAX_ENTRIES = 16;

  @Bean
  CacheManager cacheManager() {
    // Declarar o nome no construtor fecha a criacao dinamica de regioes. Com ela
    // aberta, um @Cacheable com o nome errado de digitacao passaria a cachear
    // numa regiao propria - sem TTL, sem teto e sem metrica -, e nada avisaria:
    // a chamada continuaria respondendo certo.
    var manager = new CaffeineCacheManager(GITHUB_STATS);
    manager.setCaffeine(
        Caffeine.newBuilder()
            .expireAfterWrite(TTL)
            .maximumSize(MAX_ENTRIES)
            // Sem isto o /actuator/metrics nao tem o que publicar: o Caffeine so
            // conta acertos e erros quando a coleta e pedida. E a Definition of
            // Done do MVP 4 que exige o hit ratio observavel.
            .recordStats());

    // Nulo nao entra no cache. Guardar "nao ha resposta" por seis horas e o
    // oposto do que a cadeia de fallback do ADR-0008 quer: uma falha passageira
    // ficaria congelada, e o reaquecimento acharia a entrada preenchida.
    manager.setAllowNullValues(false);
    return manager;
  }
}
