package dev.crystofer.portfolio.contact.adapter.out.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.crystofer.portfolio.contact.domain.model.ContactMessage;
import dev.crystofer.portfolio.contact.domain.port.out.SendContactEmailPort;

/**
 * Escreve a notificacao no log, em vez de envia-la.
 *
 * <p><strong>E o que roda quando nao ha credencial configurada</strong> - em desenvolvimento e na
 * suite de testes, onde exigir segredo para a aplicacao subir tornaria o projeto impossivel de
 * clonar e rodar. Quem escolhe entre ele e o provedor de verdade e o {@link EmailConfig}.
 *
 * <p><strong>Ele reporta sucesso, e esse e o risco a ter em mente.</strong> Em producao, com este
 * adaptador ativo, toda mensagem seria marcada como entregue sem sair. E por isso o log e {@code
 * warn} e nao {@code info}: a linha existe para ser encontrada por quem procurar "por que o e-mail
 * nao chegou", e um {@code info} se perderia no volume.
 */
class LoggingEmailAdapter implements SendContactEmailPort {

  private static final Logger log = LoggerFactory.getLogger(LoggingEmailAdapter.class);

  @Override
  public void send(ContactMessage message) {
    // O corpo da mensagem nao entra no log, de proposito. Ele e conteudo que um
    // desconhecido escreveu e pode conter dado pessoal - o log da plataforma nao
    // e o lugar dele. Vao o remetente e o assunto, que bastam para saber que
    // chegou e de quem.
    log.warn(
        "E-MAIL NAO ENVIADO (adaptador de log ativo): de {} <{}>, assunto \"{}\"",
        message.name(),
        message.email().value(),
        message.subject());
  }
}
