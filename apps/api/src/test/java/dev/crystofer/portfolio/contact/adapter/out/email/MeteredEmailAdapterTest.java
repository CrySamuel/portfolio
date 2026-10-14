package dev.crystofer.portfolio.contact.adapter.out.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.crystofer.portfolio.contact.domain.model.ContactMessage;
import dev.crystofer.portfolio.contact.domain.port.out.SendContactEmailPort;
import dev.crystofer.portfolio.shared.domain.EmailAddress;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * O decorador que conta os envios, sem Spring e sem provedor.
 *
 * <p>O que se prova aqui e o contrato com quem esta em volta, e nao a contagem em si: a falha sobe
 * intacta para o caso de uso - e e ela que grava {@code FAILED} -, e a serie de sucesso existe
 * antes do primeiro envio, para que zero seja uma leitura e nao uma ausencia.
 */
class MeteredEmailAdapterTest {

  private final MeterRegistry medidores = new SimpleMeterRegistry();

  private static ContactMessage mensagem() {
    return ContactMessage.received(
        "Fulana",
        new EmailAddress("fulana@exemplo.com"),
        "Vaga backend",
        "Vi o portfolio e queria conversar.",
        null,
        null);
  }

  private Counter serie(String desfecho, String excecao) {
    return medidores
        .find(MeteredEmailAdapter.MEDIDOR)
        .tag("adapter", "resend")
        .tag("outcome", desfecho)
        .tag("exception", excecao)
        .counter();
  }

  @Test
  @DisplayName("deve publicar a serie de sucesso com zero antes de qualquer envio")
  void shouldRegisterSuccessSeries_beforeAnySend() {
    new MeteredEmailAdapter(ignorada -> {}, "resend", medidores);

    assertThat(serie("sent", "none"))
        .as("sem a serie, zero envios e medidor desligado ficam indistinguiveis")
        .isNotNull();
    assertThat(serie("sent", "none").count()).isZero();
  }

  @Test
  @DisplayName("deve contar o envio que saiu")
  void shouldCountSuccess_whenProviderAccepts() {
    var adaptador = new MeteredEmailAdapter(ignorada -> {}, "resend", medidores);

    adaptador.send(mensagem());

    assertThat(serie("sent", "none").count()).isEqualTo(1.0);
    assertThat(medidores.find(MeteredEmailAdapter.MEDIDOR).tag("outcome", "failed").counter())
        .isNull();
  }

  /**
   * A recusa e contada com o nome da excecao, e a <strong>mesma</strong> excecao sobe.
   *
   * <p>{@code isSameAs}, e nao apenas o tipo: um decorador que embrulhasse a falha noutra excecao
   * continuaria fazendo o caso de uso gravar {@code FAILED}, mas apagaria a causa do log - que e
   * onde o corpo da resposta do provedor aparece.
   */
  @Test
  @DisplayName("deve contar a recusa com o nome da causa e relancar a mesma excecao")
  void shouldCountFailure_andRethrowTheSameException() {
    var recusa = new Recusa();
    SendContactEmailPort provedorQueRecusa =
        ignorada -> {
          throw recusa;
        };
    var adaptador = new MeteredEmailAdapter(provedorQueRecusa, "resend", medidores);

    assertThatThrownBy(() -> adaptador.send(mensagem())).isSameAs(recusa);

    assertThat(serie("failed", "Recusa").count()).isEqualTo(1.0);
    assertThat(serie("sent", "none").count()).isZero();
  }

  @Test
  @DisplayName("deve identificar o adaptador de log pelo proprio nome")
  void shouldTagTheAdapter() {
    var adaptador = new MeteredEmailAdapter(ignorada -> {}, "log", medidores);

    adaptador.send(mensagem());

    assertThat(
            medidores
                .find(MeteredEmailAdapter.MEDIDOR)
                .tag("adapter", "log")
                .tag("outcome", "sent")
                .counter()
                .count())
        .isEqualTo(1.0);
  }

  /**
   * Uma falha com nome proprio, como as do RestClient - {@code Forbidden}, {@code Unauthorized}.
   */
  private static final class Recusa extends RuntimeException {
    Recusa() {
      super("403 Forbidden");
    }
  }
}
