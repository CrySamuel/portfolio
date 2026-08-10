package dev.crystofer.portfolio.profile.domain.port.out;

import java.util.Optional;

import dev.crystofer.portfolio.profile.domain.model.Profile;

/**
 * Porta de saida: carregar o perfil de onde quer que ele esteja guardado.
 *
 * <p>A interface mora no dominio e o adaptador JPA do commit 18 a implementa. E a inversao de
 * dependencia da secao 3.4 na pratica: a seta aponta para dentro, e persistencia vira detalhe
 * substituivel em vez de fundacao.
 *
 * <p>Nao ha aqui nenhuma palavra de banco de dados - nem "repository", nem "entity", nem "query". O
 * nome da porta descreve o que o dominio precisa, nao como sera atendido. Se amanha o perfil vier
 * de um arquivo ou de outro servico, esta interface continua igual.
 *
 * <p>Porta pequena, com um metodo (secao 13.2, Interface Segregation). Escrita nao entra: o
 * conteudo e alterado por migracao, nunca pela aplicacao, entao um {@code SaveProfilePort} seria
 * uma porta sem chamador - e uma promessa falsa sobre o que o sistema faz.
 */
public interface LoadProfilePort {

  /**
   * @return o perfil, ou vazio se ainda nao houver linha na origem
   */
  Optional<Profile> loadProfile();
}
