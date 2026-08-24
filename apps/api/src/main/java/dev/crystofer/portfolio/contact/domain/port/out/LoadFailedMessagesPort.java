package dev.crystofer.portfolio.contact.domain.port.out;

import java.util.List;

import dev.crystofer.portfolio.contact.domain.model.StoredContactMessage;

/**
 * Porta de saida: as mensagens cuja entrega falhou.
 *
 * <p>E a consulta que da sentido ao indice parcial da {@code V5} - o unico indice explicito deste
 * schema existe exatamente para ela.
 */
public interface LoadFailedMessagesPort {

  /**
   * As mais antigas primeiro, ate o limite pedido.
   *
   * <p><strong>Ordem e limite fazem parte do contrato, e nao sao detalhe do adaptador.</strong> As
   * mais antigas primeiro porque uma mensagem parada ha dias importa mais que uma de agora; o
   * limite porque um acumulo de centenas depois de uma queda longa do provedor viraria uma rajada
   * de chamadas no minuto em que ele voltasse - e derrubaria a cota justamente na volta.
   *
   * @param limit quantas trazer, no maximo
   * @return lista possivelmente vazia, nunca nula
   */
  List<StoredContactMessage> loadFailed(int limit);
}
