package dev.crystofer.portfolio.contact.domain.port.in;

import dev.crystofer.portfolio.contact.domain.model.ContactMessage;

/**
 * Porta de entrada: receber uma mensagem do formulario de contato.
 *
 * <p><strong>E a primeira porta de escrita do sistema.</strong> As outras tres leem conteudo que
 * entrou por migracao; esta aceita dado de quem o portfolio nao conhece. Tudo o que o MVP 5 traz de
 * diferente - limite de taxa, antispam, entrega assincrona - existe por causa dessa inversao.
 *
 * <p><strong>Devolve o identificador, e nao void.</strong> Void bastaria para o controlador, que
 * responde 202 sem corpo. O identificador existe porque e ele que liga a resposta dada ao visitante
 * a linha que ficou no banco: sem ele, investigar "a mensagem de ontem nao chegou" comeca por
 * adivinhar qual das linhas era.
 */
public interface SubmitContactMessageUseCase {

  /**
   * Persiste a mensagem e anuncia que ela chegou.
   *
   * <p><strong>Nao envia o e-mail.</strong> O envio acontece depois do commit, em quem escuta o
   * evento - ver {@code ContactMessageReceivedEvent}. Quem chama esta porta pode responder ao
   * visitante assim que ela retorna, porque a partir dai a mensagem nao se perde mais.
   *
   * @param message a mensagem recebida, ja validada pelo dominio
   * @return o identificador da linha gravada
   */
  long submit(ContactMessage message);
}
