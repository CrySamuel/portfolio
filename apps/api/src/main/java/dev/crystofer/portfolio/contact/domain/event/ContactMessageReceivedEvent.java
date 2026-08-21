package dev.crystofer.portfolio.contact.domain.event;

import dev.crystofer.portfolio.contact.domain.model.ContactMessage;

/**
 * A mensagem foi recebida e persistida.
 *
 * <p><strong>O evento e o que separa receber de entregar</strong>, e essa separacao e a razao de
 * nenhuma mensagem se perder. Quem publica ja gravou; quem escuta tenta enviar. Se o envio falhar,
 * a linha continua no banco com {@code FAILED} e o reprocessamento a encontra - o visitante nunca
 * ve a diferenca, porque para ele a resposta ja foi dada.
 *
 * <p><strong>Na ordem inversa - enviar e depois gravar - a falha custa a mensagem.</strong> E a
 * ordem que parece natural, porque o e-mail e o objetivo e o banco parece o detalhe. E o inverso: o
 * banco e o unico lugar de onde a mensagem pode ser recuperada.
 *
 * <p>O identificador vem junto do conteudo de proposito. Quem escuta precisa dos dois: do conteudo
 * para montar o e-mail, e do identificador para gravar o desfecho na linha certa - sem ele, o
 * ouvinte teria de procurar a mensagem de volta por algum criterio, e duas mensagens iguais no
 * mesmo minuto sao possiveis.
 *
 * <p>E um record no pacote {@code domain}, entao nao conhece Spring. Quem o publica e a camada de
 * aplicacao; o mecanismo de publicacao e detalhe dela, e trocar de mecanismo nao toca este arquivo.
 *
 * @param messageId identificador da linha gravada
 * @param message o conteudo recebido
 */
public record ContactMessageReceivedEvent(long messageId, ContactMessage message) {

  public ContactMessageReceivedEvent {
    if (message == null) {
      throw new IllegalArgumentException("Mensagem e obrigatoria");
    }
    if (messageId <= 0) {
      throw new IllegalArgumentException("Identificador da mensagem e obrigatorio");
    }
  }
}
