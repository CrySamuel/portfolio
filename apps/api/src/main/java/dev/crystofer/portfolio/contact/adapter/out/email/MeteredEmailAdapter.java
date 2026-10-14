package dev.crystofer.portfolio.contact.adapter.out.email;

import dev.crystofer.portfolio.contact.domain.model.ContactMessage;
import dev.crystofer.portfolio.contact.domain.port.out.SendContactEmailPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Conta cada tentativa de envio, por adaptador, desfecho e causa.
 *
 * <p><strong>Existe porque a primeira falha real so apareceu no log.</strong> O primeiro envio de
 * producao foi recusado pelo provedor quatro vezes seguidas, e de fora so se via que a mensagem
 * estava gravada e que o status dela mudava a cada rodada do reenvio - o motivo, um 403, estava no
 * log da plataforma. Com este medidor a mesma pergunta se responde pelo {@code
 * /actuator/prometheus}: a serie {@code outcome="failed"} aparece, e o tag {@code exception} diz
 * {@code Forbidden}.
 *
 * <p><strong>Decorador, e nao contador dentro do caso de uso.</strong> Quem sabe se o envio saiu e
 * o adaptador; o caso de uso so reage a excecao. Embrulhar a porta aqui, no {@code EmailConfig},
 * deixa a camada de aplicacao como estava e conta exatamente a fronteira com o provedor.
 *
 * <p><strong>O adaptador de log tambem e contado, e com o proprio nome.</strong> O "sucesso" dele e
 * a mensagem escrita no log em vez de enviada - o modo de falhar que o {@code EmailConfig} avisa no
 * boot. Uma serie com {@code adapter="log"} em producao e o mesmo aviso, visivel sem abrir o log.
 *
 * <p><strong>A falha e relancada sem alteracao.</strong> E a excecao que faz o caso de uso gravar
 * {@code FAILED} e o reprocessamento tentar de novo; um decorador que a engolisse marcaria como
 * entregue o que nao saiu - o unico desfecho que esta cadeia existe para impedir.
 */
final class MeteredEmailAdapter implements SendContactEmailPort {

  static final String MEDIDOR = "contact.email.deliveries";

  private final SendContactEmailPort alvo;
  private final String adaptador;
  private final MeterRegistry medidores;

  MeteredEmailAdapter(SendContactEmailPort alvo, String adaptador, MeterRegistry medidores) {
    this.alvo = alvo;
    this.adaptador = adaptador;
    this.medidores = medidores;

    // A serie de sucesso nasce com zero, no boot. Sem isso, nenhuma serie e
    // ambiguo - nada foi enviado, ou o medidor nao esta ligado? -, e um medidor
    // que nunca saiu de zero ja produziu um diagnostico errado neste projeto.
    contador("sent", "none");
  }

  @Override
  public void send(ContactMessage message) {
    try {
      alvo.send(message);
    } catch (RuntimeException falha) {
      contador("failed", causa(falha)).increment();
      throw falha;
    }

    contador("sent", "none").increment();
  }

  private Counter contador(String desfecho, String excecao) {
    return Counter.builder(MEDIDOR)
        .description("Tentativas de envio da notificacao de contato")
        .tag("adapter", adaptador)
        .tag("outcome", desfecho)
        .tag("exception", excecao)
        .register(medidores);
  }

  /**
   * O nome simples da excecao, que e o que distingue as causas que importam: {@code Forbidden} e
   * {@code Unauthorized} sao configuracao, {@code ResourceAccessException} e rede. O status HTTP
   * cru diria menos, e a mensagem inteira explodiria a cardinalidade do medidor.
   */
  private static String causa(RuntimeException falha) {
    String nome = falha.getClass().getSimpleName();
    return nome.isEmpty() ? falha.getClass().getName() : nome;
  }
}
