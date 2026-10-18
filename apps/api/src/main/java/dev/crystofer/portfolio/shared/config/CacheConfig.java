package dev.crystofer.portfolio.shared.config;

import java.time.Duration;
import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * O cache de aplicacao - o nivel L2 da secao 3.9.
 *
 * <p>Duas familias de regioes: o retrato do GitHub, e o conteudo do banco (secao 10.5 do plano).
 *
 * <p><strong>O conteudo entrou depois, e por medicao.</strong> A primeira versao deixava as
 * leituras do banco sem cache, com o argumento de que elas ja tem duas camadas na frente - o HTML
 * na CDN e o cache de {@code fetch} do Next - e de que um terceiro nivel nao tiraria carga de
 * ninguem. Para o visitante continua verdade. Mas o teste de carga do plano (100 clientes por 2
 * minutos, {@code infra/k6}) mede a API sozinha, e nela cada leitura ia ao Neon por um pool de
 * cinco conexoes, numa instancia de CPU minima: p95 de 23,6 s, saturada em ~10 pedidos por segundo.
 *
 * <p><strong>Invalidar e o problema que nao existe aqui.</strong> O conteudo so muda por migracao
 * Flyway, que roda na subida da aplicacao - e a subida comeca com o cache vazio. Os dez minutos sao
 * rede de seguranca, e nao o mecanismo de atualizacao.
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
/*
 * A ordem do @EnableCaching e o que poe o cache **por fora** do disjuntor.
 *
 * <p>Em AOP do Spring, menor numero significa mais externo. Com a maior
 * precedencia possivel, o interceptador de cache envolve os aspectos do
 * Resilience4j: um acerto de cache devolve o valor sem executar o metodo, e
 * portanto sem passar pelo circuito.
 *
 * <p>Invertido, seis horas de respostas cacheadas seriam contadas como sucesso
 * pelo disjuntor e limpariam a estatistica de falha dele - o circuito ficaria
 * fechado sobre um GitHub que caiu, e a primeira chamada real depois da
 * expiracao esperaria o timeout inteiro para descobrir isso.
 */
@Configuration
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class CacheConfig {

  /** O nome e usado no {@code @Cacheable} do adaptador e nas metricas do actuator. */
  public static final String GITHUB_STATS = "github-stats";

  /** Uma regiao por porta de leitura do banco: metrica de acerto separada para cada uma. */
  public static final String PROFILE = "profile";

  public static final String EXPERIENCES = "experiences";
  public static final String SKILLS = "skills";
  public static final String PROJECTS = "projects";
  public static final String PROJECT = "project";

  private static final Duration TTL = Duration.ofHours(6);

  /** Os dez minutos da secao 10.5 do plano. */
  private static final Duration TTL_DO_CONTEUDO = Duration.ofMinutes(10);

  /**
   * A chave e o idioma (no projeto, o slug e o idioma): hoje sao no maximo uma dezena de entradas
   * por regiao. O teto e para o dia em que algo gerar chaves sem querer, e slug inexistente nao
   * conta - resposta vazia nao entra no cache.
   */
  private static final int MAX_ENTRIES_DO_CONTEUDO = 64;

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

    // Nulo nao entra no cache. Guardar "nao ha resposta" por seis horas e o
    // oposto do que a cadeia de fallback do ADR-0008 quer: uma falha passageira
    // ficaria congelada, e o reaquecimento acharia a entrada preenchida.
    //
    // Antes de registrar as regioes do conteudo, e a ordem importa: a regiao
    // registrada a parte copia a opcao no momento do registro, e mudar a opcao
    // depois so alcanca as regioes comuns.
    manager.setAllowNullValues(false);
    manager.setCaffeine(
        Caffeine.newBuilder()
            .expireAfterWrite(TTL)
            .maximumSize(MAX_ENTRIES)
            // Sem isto o /actuator/metrics nao tem o que publicar: o Caffeine so
            // conta acertos e erros quando a coleta e pedida. E a Definition of
            // Done do MVP 4 que exige o hit ratio observavel.
            .recordStats());

    for (var regiao : List.of(PROFILE, EXPERIENCES, SKILLS, PROJECTS, PROJECT)) {
      manager.registerCustomCache(
          regiao,
          Caffeine.newBuilder()
              .expireAfterWrite(TTL_DO_CONTEUDO)
              .maximumSize(MAX_ENTRIES_DO_CONTEUDO)
              .recordStats()
              .build());
    }
    return manager;
  }
}
