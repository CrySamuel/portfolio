package dev.crystofer.portfolio.contact.domain.port.out;

import dev.crystofer.portfolio.contact.domain.model.ContactMessage;

/**
 * Porta de saida: notificar o dono de que chegou uma mensagem.
 *
 * <p><strong>Esta porta lanca quando falha, e a escolha e o oposto da do GitHub.</strong> La o
 * contrato promete sempre devolver estatisticas, porque indisponibilidade e um estado previsto que
 * o visitante nao precisa conhecer. Aqui a falha precisa subir: e ela que decide entre gravar
 * {@code SENT} e {@code FAILED}, e um fallback silencioso marcaria como enviada uma mensagem que
 * nunca saiu - o pior desfecho possivel, porque o dono nunca saberia que existiu.
 *
 * <p>Nao ha retentativa aqui dentro. Ela e do job de reprocessamento, que tenta de novo minutos
 * depois com a linha ja gravada - insistir em rajada dentro da mesma chamada gastaria a cota do
 * provedor no exato momento em que ele esta com problema.
 */
public interface SendContactEmailPort {

  /**
   * @param message a mensagem a notificar; o remetente vira {@code Reply-To}
   * @throws RuntimeException quando o provedor recusa ou nao responde
   */
  void send(ContactMessage message);
}
