package dev.crystofer.portfolio.contact.domain.port.out;

import dev.crystofer.portfolio.contact.domain.model.EmailStatus;

/**
 * Porta de saida: gravar o desfecho da entrega.
 *
 * <p>Separada de {@link SaveContactMessagePort} de proposito. Aquela recebe o que o visitante
 * escreveu, uma vez; esta altera uma linha que ja existe, possivelmente varias vezes ao longo da
 * vida dela. Sao momentos, transacoes e riscos diferentes - juntar as duas numa interface de
 * "repositorio de mensagens" esconderia isso atras de um nome generico.
 */
public interface UpdateEmailStatusPort {

  /**
   * @param id identificador da linha
   * @param status o novo estado de entrega
   */
  void updateStatus(long id, EmailStatus status);
}
