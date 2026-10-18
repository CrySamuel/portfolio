package dev.crystofer.portfolio.contact.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.crystofer.portfolio.contact.domain.model.ContactMessage;
import dev.crystofer.portfolio.shared.domain.EmailAddress;

/**
 * Os invariantes de {@link ContactMessageReceivedEvent}.
 *
 * <p>O identificador e o que deixa o ouvinte gravar o desfecho do envio na linha certa. Um evento
 * com identificador zero chegaria ao ouvinte sem erro nenhum, e o {@code SENT} ou o {@code FAILED}
 * iria para linha nenhuma - a mensagem ficaria {@code PENDING} para sempre, e o reprocessamento a
 * mandaria de novo a cada passagem. Recusar na construcao poe a falha onde o defeito esta.
 */
class ContactMessageReceivedEventTest {

  private static final ContactMessage MENSAGEM =
      ContactMessage.received(
          "Fulana", new EmailAddress("fulana@exemplo.com"), "Vaga", "Ola!", null, null);

  @Test
  @DisplayName("deve carregar o identificador e o conteudo juntos")
  void shouldCarry_idAndContent() {
    var evento = new ContactMessageReceivedEvent(42L, MENSAGEM);

    assertThat(evento.messageId()).isEqualTo(42L);
    assertThat(evento.message()).isSameAs(MENSAGEM);
  }

  @Test
  @DisplayName("deve recusar evento sem mensagem")
  void shouldReject_nullMessage() {
    assertThatThrownBy(() -> new ContactMessageReceivedEvent(42L, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Mensagem");
  }

  @ParameterizedTest(name = "identificador invalido: {0}")
  @ValueSource(longs = {0L, -1L})
  @DisplayName("deve recusar identificador que nao aponta para linha gravada")
  void shouldReject_idThatIsNotARow(long id) {
    assertThatThrownBy(() -> new ContactMessageReceivedEvent(id, MENSAGEM))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Identificador");
  }
}
