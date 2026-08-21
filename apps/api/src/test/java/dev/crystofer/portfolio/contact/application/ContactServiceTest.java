package dev.crystofer.portfolio.contact.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import dev.crystofer.portfolio.contact.domain.event.ContactMessageReceivedEvent;
import dev.crystofer.portfolio.contact.domain.model.ContactMessage;
import dev.crystofer.portfolio.contact.domain.port.out.SaveContactMessagePort;
import dev.crystofer.portfolio.shared.domain.EmailAddress;

/**
 * O que o {@code ContactService} garante, que e essencialmente uma ordem.
 *
 * <p>Gravar antes de anunciar e a regra que faz nenhuma mensagem se perder, e uma regra de ordem so
 * e verificavel observando a ordem - conferir que as duas coisas aconteceram deixaria passar
 * exatamente a inversao que importa.
 */
class ContactServiceTest {

  private final SaveContactMessagePort repositorio = mock(SaveContactMessagePort.class);
  private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
  private final ContactService servico = new ContactService(repositorio, eventos);

  private static final ContactMessage MENSAGEM =
      ContactMessage.received(
          "Fulana", new EmailAddress("fulana@exemplo.com"), "Vaga backend", "Ola!", null, null);

  /**
   * A gravacao vem primeiro, e o evento depois.
   *
   * <p>Na ordem inversa o ouvinte poderia enviar o e-mail antes de a linha existir, e uma falha na
   * gravacao deixaria uma mensagem entregue que o sistema nao consegue mostrar a ninguem - o pior
   * dos dois mundos, porque o remetente recebeu resposta e o dono nao tem o registro.
   */
  @Test
  @DisplayName("deve gravar antes de publicar o evento")
  void shouldSave_beforePublishing() {
    when(repositorio.save(MENSAGEM)).thenReturn(42L);

    servico.submit(MENSAGEM);

    var ordem = inOrder(repositorio, eventos);
    ordem.verify(repositorio).save(MENSAGEM);
    ordem.verify(eventos).publishEvent(any(ContactMessageReceivedEvent.class));
  }

  /**
   * O evento carrega o identificador que o banco atribuiu.
   *
   * <p>E o que liga a resposta dada ao visitante a linha gravada. Sem ele, o ouvinte teria de
   * procurar a mensagem de volta por algum criterio - e duas mensagens iguais no mesmo minuto sao
   * possiveis, entao o criterio seria um chute.
   */
  @Test
  @DisplayName("deve publicar o evento com o identificador atribuido e a mensagem")
  void shouldPublish_withAssignedId() {
    when(repositorio.save(MENSAGEM)).thenReturn(42L);

    long id = servico.submit(MENSAGEM);

    var capturado = ArgumentCaptor.forClass(ContactMessageReceivedEvent.class);
    verify(eventos).publishEvent(capturado.capture());

    assertThat(id).isEqualTo(42L);
    assertThat(capturado.getValue().messageId()).isEqualTo(42L);
    assertThat(capturado.getValue().message()).isEqualTo(MENSAGEM);
  }

  /**
   * Gravacao que falha nao anuncia nada.
   *
   * <p>O evento significa "a mensagem esta guardada". Publica-lo depois de uma falha de escrita
   * faria o ouvinte enviar um e-mail sobre uma mensagem que nao existe, e o visitante receberia
   * erro - duas versoes contraditorias do mesmo fato.
   */
  @Test
  @DisplayName("nao deve publicar evento quando a gravacao falha")
  void shouldNotPublish_whenSaveFails() {
    when(repositorio.save(MENSAGEM)).thenThrow(new IllegalStateException("banco fora"));

    assertThatThrownBy(() -> servico.submit(MENSAGEM)).isInstanceOf(IllegalStateException.class);

    verify(eventos, never()).publishEvent(any(ContactMessageReceivedEvent.class));
  }
}
