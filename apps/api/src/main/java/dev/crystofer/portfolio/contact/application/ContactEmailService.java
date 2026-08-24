package dev.crystofer.portfolio.contact.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.crystofer.portfolio.contact.domain.model.EmailStatus;
import dev.crystofer.portfolio.contact.domain.model.StoredContactMessage;
import dev.crystofer.portfolio.contact.domain.port.in.DeliverContactEmailUseCase;
import dev.crystofer.portfolio.contact.domain.port.out.SendContactEmailPort;
import dev.crystofer.portfolio.contact.domain.port.out.UpdateEmailStatusPort;

/**
 * Entrega a notificacao e grava o que aconteceu.
 *
 * <p><strong>Este e o unico lugar do sistema onde um {@code catch} largo esta certo</strong>, e
 * vale dizer por que, porque o projeto recusou o mesmo padrao no adaptador do GitHub. La o {@code
 * catch} era andaime escondendo a ausencia de resiliencia. Aqui ele <em>e</em> a regra: qualquer
 * falha de envio - do provedor, da rede, de um NPE no template - precisa virar {@code FAILED}
 * gravado, e nao excecao subindo. Quem chama nao pode fazer nada com a excecao: sao um ouvinte de
 * evento e um job agendado, os dois rodando sem ninguem esperando resposta.
 *
 * <p><strong>Falhar em gravar o desfecho e diferente de falhar em enviar.</strong> A gravacao fica
 * fora do {@code catch}: se o banco cair no meio, a excecao sobe e a linha continua no estado
 * anterior - que e {@code PENDING} ou {@code FAILED}, os dois recuperaveis pelo reprocessamento.
 * Engolir esse erro tambem produziria o unico desfecho inaceitavel: uma mensagem entregue e marcada
 * como falha, ou o contrario.
 *
 * <p>O reprocessamento nao para no primeiro erro. Uma mensagem que falha de novo nao deve impedir
 * as seguintes - elas podem ter falhado por outro motivo, e a fila existe justamente porque falhas
 * sao independentes.
 */
@Service
class ContactEmailService implements DeliverContactEmailUseCase {

  private static final Logger log = LoggerFactory.getLogger(ContactEmailService.class);

  private final SendContactEmailPort email;
  private final UpdateEmailStatusPort status;

  ContactEmailService(SendContactEmailPort email, UpdateEmailStatusPort status) {
    this.email = email;
    this.status = status;
  }

  @Override
  @Transactional
  public boolean deliver(StoredContactMessage stored) {
    EmailStatus desfecho;

    try {
      email.send(stored.message());
      desfecho = EmailStatus.SENT;
    } catch (RuntimeException falha) {
      // O texto da causa, e nao a pilha: em operacao normal esta linha e o unico
      // sinal de que o provedor caiu, e ela precisa caber num alerta. A pilha
      // reapareceria a cada mensagem enquanto durasse a queda.
      log.warn("Falha ao enviar a mensagem {}. causa={}", stored.id(), String.valueOf(falha));
      desfecho = EmailStatus.FAILED;
    }

    status.updateStatus(stored.id(), desfecho);
    return desfecho == EmailStatus.SENT;
  }
}
