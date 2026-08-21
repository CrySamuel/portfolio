package dev.crystofer.portfolio.contact.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.crystofer.portfolio.contact.domain.event.ContactMessageReceivedEvent;
import dev.crystofer.portfolio.contact.domain.model.ContactMessage;
import dev.crystofer.portfolio.contact.domain.port.in.SubmitContactMessageUseCase;
import dev.crystofer.portfolio.contact.domain.port.out.SaveContactMessagePort;

/**
 * Recebe a mensagem de contato: grava primeiro, anuncia depois.
 *
 * <p><strong>A ordem e a regra de negocio inteira deste servico.</strong> Gravar antes de anunciar
 * e o que faz nenhuma mensagem se perder quando o provedor de e-mail esta fora do ar - a linha
 * existe, com estado {@code PENDING}, e o reprocessamento a encontra. Na ordem inversa, uma falha
 * de envio levaria junto a unica copia da mensagem.
 *
 * <p><strong>Este e o primeiro servico transacional do sistema.</strong> Os outros quatro so leem,
 * e leitura nao precisa de transacao explicita. Aqui ela existe por causa do que vem no commit 47:
 * o ouvinte do evento envia o e-mail em {@code AFTER_COMMIT}, e sem transacao nao ha <em>after
 * commit</em> - o ouvinte dispararia junto com a publicacao, antes de a linha estar de fato
 * gravada, e um erro posterior desfaria a gravacao deixando o e-mail ja enviado.
 *
 * <p><strong>O servico nao envia e-mail e nao sabe que existe e-mail.</strong> Ele publica um fato
 * - "a mensagem chegou" - e quem se interessa reage. E o que permite acrescentar uma segunda reacao
 * (um webhook, uma metrica, uma notificacao no Telegram) sem tocar aqui, e e a forma de comunicacao
 * que a regra de fronteira do ArchUnit deixa aberta entre modulos.
 *
 * <p>Nao ha {@code try} em volta da gravacao, e a ausencia e deliberada. Escrita que falha precisa
 * chegar ao visitante como erro: uma confirmacao dada sobre uma mensagem que nao foi gravada e pior
 * que o erro, porque ele nao tentaria de novo.
 */
@Service
class ContactService implements SubmitContactMessageUseCase {

  private final SaveContactMessagePort repositorio;
  private final ApplicationEventPublisher eventos;

  ContactService(SaveContactMessagePort repositorio, ApplicationEventPublisher eventos) {
    this.repositorio = repositorio;
    this.eventos = eventos;
  }

  @Override
  @Transactional
  public long submit(ContactMessage message) {
    long id = repositorio.save(message);

    // Publicado dentro da transacao, entregue depois dela. O ouvinte do commit
    // 47 usa @TransactionalEventListener(AFTER_COMMIT), entao esta chamada nao
    // dispara envio nenhum agora - ela apenas registra o fato para quando o
    // commit acontecer. Publicar aqui e nao depois do metodo e o que garante
    // que uma excecao no fim do metodo cancele as duas coisas juntas.
    eventos.publishEvent(new ContactMessageReceivedEvent(id, message));

    return id;
  }
}
