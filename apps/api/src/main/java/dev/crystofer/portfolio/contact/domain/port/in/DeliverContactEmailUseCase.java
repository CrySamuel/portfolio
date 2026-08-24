package dev.crystofer.portfolio.contact.domain.port.in;

import dev.crystofer.portfolio.contact.domain.model.StoredContactMessage;

/**
 * Porta de entrada: tentar entregar a notificacao de uma mensagem ja gravada.
 *
 * <p><strong>Nao lanca, e a diferenca em relacao a porta de saida e o ponto do desenho.</strong> A
 * porta de saida lanca para que a falha seja distinguivel; esta a absorve e a converte em estado
 * gravado. Quem chama - o ouvinte do evento e o job de reprocessamento - nao tem o que fazer com
 * uma excecao: os dois rodam sem ninguem esperando, e uma excecao ali viraria apenas ruido no log
 * ou, pior, uma thread de agendador morta.
 */
public interface DeliverContactEmailUseCase {

  /**
   * Tenta enviar e grava o desfecho.
   *
   * @param stored a mensagem gravada, com o identificador
   * @return {@code true} se o provedor aceitou
   */
  boolean deliver(StoredContactMessage stored);
}
