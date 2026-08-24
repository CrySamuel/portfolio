package dev.crystofer.portfolio.contact.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.crystofer.portfolio.contact.domain.model.StoredContactMessage;
import dev.crystofer.portfolio.contact.domain.port.in.DeliverContactEmailUseCase;
import dev.crystofer.portfolio.contact.domain.port.in.RetryFailedEmailsUseCase;
import dev.crystofer.portfolio.contact.domain.port.out.LoadFailedMessagesPort;

/**
 * Reprocessa o que ficou por entregar.
 *
 * <p><strong>Bean separado do {@code ContactEmailService}, e a separacao nao e organizacional - e o
 * que faz a transacao existir.</strong> Com os dois metodos na mesma classe, {@code retryFailed}
 * chamaria {@code deliver} por {@code this}, e autoinvocacao <em>nao passa pelo proxy do
 * Spring</em>: o {@code @Transactional} de {@code deliver} seria ignorado, e a promessa de uma
 * transacao por mensagem viraria comentario mentindo sobre o codigo. Chamando pela interface, a
 * chamada atravessa o proxy e cada entrega ganha a sua.
 *
 * <p><strong>Uma transacao por mensagem, e nao uma para o lote.</strong> Com uma so, a falha de
 * gravacao na decima desfaria as nove ja entregues - que seriam reenviadas na passagem seguinte,
 * com o dono recebendo tudo duas vezes. O e-mail ja saiu; desfazer o registro dele nao desfaz o
 * envio.
 *
 * <p>O laco nao para no primeiro erro. Uma mensagem que falha de novo nao deve impedir as
 * seguintes: elas podem ter falhado por outro motivo, e a fila existe justamente porque as falhas
 * sao independentes.
 */
@Service
class FailedEmailRetryService implements RetryFailedEmailsUseCase {

  private static final Logger log = LoggerFactory.getLogger(FailedEmailRetryService.class);

  /**
   * Quantas o reprocessamento tenta por passagem.
   *
   * <p>Um acumulo grande depois de uma queda longa viraria uma rajada no minuto em que o provedor
   * voltasse, e derrubaria a cota justamente na volta. Vinte por passagem, com o job rodando de
   * poucos em poucos minutos, drena o acumulo sem pico.
   */
  private static final int LOTE = 20;

  private final DeliverContactEmailUseCase entrega;
  private final LoadFailedMessagesPort falhas;

  FailedEmailRetryService(DeliverContactEmailUseCase entrega, LoadFailedMessagesPort falhas) {
    this.entrega = entrega;
    this.falhas = falhas;
  }

  @Override
  public int retryFailed() {
    var pendentes = falhas.loadFailed(LOTE);
    if (pendentes.isEmpty()) {
      return 0;
    }

    int entregues = 0;
    for (StoredContactMessage pendente : pendentes) {
      if (entrega.deliver(pendente)) {
        entregues++;
      }
    }

    log.info("Reprocessamento: {} de {} entregues", entregues, pendentes.size());
    return entregues;
  }
}
