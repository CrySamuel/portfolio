package dev.crystofer.portfolio.contact.domain.port.in;

/**
 * Porta de entrada: reprocessar o que ficou por entregar.
 *
 * <p>E a metade que transforma "a mensagem esta gravada" em "a mensagem chega". Sem ela, uma queda
 * do provedor de e-mail deixaria a linha em {@code FAILED} para sempre - tecnicamente nao perdida,
 * praticamente invisivel.
 */
public interface RetryFailedEmailsUseCase {

  /**
   * @return quantas foram entregues nesta passagem
   */
  int retryFailed();
}
