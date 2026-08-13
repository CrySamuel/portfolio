package dev.crystofer.portfolio.profile.domain.port.out;

import java.util.List;

import dev.crystofer.portfolio.profile.domain.model.Experience;

/**
 * Porta de saida: carregar as passagens de onde quer que estejam guardadas.
 *
 * <p>Devolve {@code List}, e nao {@link dev.crystofer.portfolio.profile.domain.model.Timeline}, e a
 * escolha e proposital. A porta descreve o que a origem tem a oferecer - um conjunto de passagens,
 * na ordem em que ela as entregar. Quem transforma isso em timeline ordenada e a camada de
 * aplicacao, num lugar so.
 *
 * <p>Se a porta ja devolvesse {@code Timeline}, cada adaptador futuro passaria a ter uma chance de
 * montar a ordem por conta propria, e a garantia deixaria de ter dono unico. Um adaptador de
 * arquivo ou de outro servico devolveria o que tivesse, do jeito que tivesse - e e exatamente isso
 * que a porta deve permitir.
 *
 * <p>Escrita nao entra, pelo mesmo motivo de {@link LoadProfilePort}: o conteudo e alterado por
 * migracao, nunca pela aplicacao.
 */
public interface LoadExperiencePort {

  /**
   * @return as passagens registradas, possivelmente vazia
   */
  List<Experience> loadExperiences();
}
