package dev.crystofer.portfolio.contact.adapter.in.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import dev.crystofer.portfolio.contact.domain.event.ContactMessageReceivedEvent;
import dev.crystofer.portfolio.contact.domain.model.StoredContactMessage;
import dev.crystofer.portfolio.contact.domain.port.in.DeliverContactEmailUseCase;

/**
 * Dispara a notificacao depois que a mensagem esta gravada.
 *
 * <p><strong>E um adaptador de entrada</strong>, como o controlador e o agendador - a diferenca e
 * quem dispara. La e uma requisicao HTTP e o relogio; aqui e um evento de dominio. Por isso ele
 * fala com o caso de uso e nao orquestra nada: a decisao entre {@code SENT} e {@code FAILED} e
 * regra, e regra nao mora em adaptador.
 *
 * <p><strong>{@code AFTER_COMMIT} e o coracao do desenho.</strong> Um {@code @EventListener} comum
 * roda no instante da publicacao, dentro da transacao - o e-mail sairia antes de a linha estar
 * gravada, e um erro posterior desfaria a gravacao deixando a notificacao ja entregue. O dono leria
 * sobre uma mensagem que o sistema nao tem.
 *
 * <p><strong>{@code AFTER_COMMIT} e nao {@code BEFORE_COMMIT}</strong> pela mesma razao levada ao
 * limite: antes do commit, a gravacao ainda pode falhar.
 *
 * <p><strong>{@code @Async} porque o visitante nao tem o que esperar.</strong> A resposta dele ja
 * esta decidida quando a transacao confirma - a mensagem esta guardada, e e isso que o formulario
 * promete. Segurar a requisicao ate o provedor de e-mail responder faria o tempo de resposta do
 * portfolio depender de um terceiro, e uma lentidao dele viraria formulario travado.
 *
 * <p><strong>As duas anotacoes sao inseparaveis, e isso foi medido.</strong> Sem {@code @Async}, o
 * ouvinte roda na thread da requisicao dentro do callback de {@code AFTER_COMMIT} - onde a
 * sincronizacao ainda esta ativa e a transacao esta encerrando. O {@code @Transactional} do caso de
 * uso nao consegue abrir a dele ali, e o {@code UPDATE} do desfecho falha com {@code
 * TransactionRequiredException}. Tres dos quatro testes de entrega reprovam assim.
 *
 * <p>Ou seja: o {@code @Async} nao esta aqui por desempenho, ainda que o desempenho seja um efeito
 * bem-vindo. Ele esta aqui porque e o que da a entrega uma thread sem transacao a que se juntar -
 * e, com ela, uma transacao propria para gravar {@code SENT} ou {@code FAILED}.
 */
@Component
class ContactEmailListener {

  private final DeliverContactEmailUseCase entrega;

  ContactEmailListener(DeliverContactEmailUseCase entrega) {
    this.entrega = entrega;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void aoReceberMensagem(ContactMessageReceivedEvent evento) {
    entrega.deliver(new StoredContactMessage(evento.messageId(), evento.message()));
  }
}
