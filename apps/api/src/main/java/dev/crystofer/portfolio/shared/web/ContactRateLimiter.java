package dev.crystofer.portfolio.shared.web;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import dev.crystofer.portfolio.shared.config.properties.ContactProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

/**
 * Limita quantas mensagens um mesmo remetente envia por hora.
 *
 * <p><strong>Um balde por remetente, guardados num cache com expiracao.</strong> Sem a expiracao
 * isto seria um vazamento de memoria com nome bonito: cada IP visitante deixaria um objeto vivo
 * para sempre numa instancia de 512 MB. O Caffeine ja esta no projeto pelo cache do GitHub, entao a
 * estrutura nao custa dependencia nova.
 *
 * <p><strong>A janela do balde e a expiracao do cache sao independentes de proposito.</strong> O
 * balde recarrega em uma hora; a entrada some depois de duas sem uso. Fossem iguais, um remetente
 * que voltasse no minuto 59 encontraria o balde prestes a ser descartado, e o descarte devolveria
 * as cinco fichas de uma vez - o limite seria contornavel so por esperar.
 *
 * <p><strong>Recarga gradual, e nao de uma vez.</strong> {@code Bandwidth.simple} distribui a
 * reposicao ao longo da janela: quem gastou as cinco fichas recebe uma nova a cada doze minutos, em
 * vez de cinco juntas no fim da hora. Isso torna a rajada impossivel <em>e</em> o limite menos
 * punitivo para quem errou o texto e quer reenviar.
 *
 * <p>Fica em {@code shared} porque nao conhece nada de contato - recebe uma chave opaca e responde
 * sim ou nao. A regra de fronteira do ArchUnit exige isso: {@code shared} nao pode depender de
 * modulo nenhum.
 */
@Component
@EnableConfigurationProperties(ContactProperties.class)
public class ContactRateLimiter {

  /**
   * Quanto tempo um balde ocioso sobrevive.
   *
   * <p>Duas horas, e nao uma: ver acima. O dobro da janela garante que a entrada so seja descartada
   * bem depois de ela ja ter voltado a estar cheia, e ai descartar nao devolve nada.
   */
  private static final Duration OCIOSIDADE = Duration.ofHours(2);

  private final LoadingCache<String, Bucket> baldes;

  ContactRateLimiter(ContactProperties properties) {
    this.baldes =
        Caffeine.newBuilder()
            .expireAfterAccess(OCIOSIDADE.toMinutes(), TimeUnit.MINUTES)
            // Teto de entradas alem da expiracao: uma rajada de IPs distintos -
            // que e como um ataque distribuido se parece - nao pode crescer a
            // memoria sem limite so porque nenhuma entrada expirou ainda.
            .maximumSize(10_000)
            .build(chave -> novoBalde(properties.maxPerHour()));
  }

  private static Bucket novoBalde(int porHora) {
    return Bucket.builder().addLimit(Bandwidth.simple(porHora, Duration.ofHours(1))).build();
  }

  /**
   * Consome uma ficha, se houver.
   *
   * @param chave identificador opaco do remetente - aqui, o hash do IP
   * @return {@code true} quando a mensagem pode seguir
   */
  public boolean tryConsume(String chave) {
    return baldes.get(chave).tryConsume(1);
  }

  /**
   * Quanto falta para a proxima ficha, em segundos.
   *
   * <p>E o valor do cabecalho {@code Retry-After}. Devolve-lo e a diferenca entre um 429 util e um
   * 429 que so diz "nao" - com ele, quem consome sabe exatamente quando tentar de novo, e um
   * cliente automatizado para de bater na porta enquanto isso.
   */
  public long secondsUntilRefill(String chave) {
    long nanos = baldes.get(chave).estimateAbilityToConsume(1).getNanosToWaitForRefill();
    // Arredonda para cima: devolver zero convidaria a tentar de novo no mesmo
    // instante, e a tentativa seria recusada de novo.
    return Math.max(1, Duration.ofNanos(nanos).toSeconds() + 1);
  }
}
