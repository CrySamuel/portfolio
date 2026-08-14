package dev.crystofer.portfolio.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * O ETag das respostas de leitura (secao 3.8).
 *
 * <p>Divida vencida: o commit 19 adiou o ETag para ca porque ele e filtro de aplicacao inteira, e
 * nao decisao de um endpoint - o {@code Cache-Control} entrou la porque e por rota e faz parte
 * daquele contrato, mas o ETag so faz sentido registrado uma vez, valendo para tudo.
 *
 * <p><strong>Shallow, e nao deep.</strong> O filtro calcula o resumo do corpo ja serializado e
 * devolve 304 quando o {@code If-None-Match} bate. A economia e de <em>banda</em>, nao de trabalho:
 * a consulta ao banco acontece de qualquer forma. Um ETag profundo - derivado de {@code
 * updated_at}, por exemplo - economizaria tambem o trabalho, e foi recusado porque exigiria uma
 * consulta propria de versao por endpoint, com o risco de a versao e o corpo discordarem.
 *
 * <p>Para este projeto a escolha e confortavel: o visitante nunca chega aqui, porque a Vercel serve
 * HTML pre-renderizado; quem faz estas requisicoes e a revalidacao do ISR, em segundo plano, e o
 * que ela ganha com 304 e nao rebaixar o cache por um corpo identico.
 */
@Configuration
public class HttpCacheConfig {

  /**
   * Registrado como bean simples, sem {@code FilterRegistrationBean}.
   *
   * <p>O Spring Boot registra qualquer bean do tipo {@code Filter} com a menor precedencia, o que
   * coloca este filtro por ultimo na cadeia - encostado no servlet. E onde ele deve ficar: assim
   * ele envolve a resposta do controlador, e nao as respostas que o filtro de chave de servico
   * produz antes do roteamento. Gerar ETag para um 401 seria trabalho para uma resposta que ninguem
   * revalida.
   */
  @Bean
  public ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
    return new ShallowEtagHeaderFilter();
  }
}
