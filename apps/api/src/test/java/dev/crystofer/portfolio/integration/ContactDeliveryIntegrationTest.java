package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import dev.crystofer.portfolio.contact.domain.model.ContactMessage;
import dev.crystofer.portfolio.contact.domain.port.in.RetryFailedEmailsUseCase;
import dev.crystofer.portfolio.contact.domain.port.in.SubmitContactMessageUseCase;
import dev.crystofer.portfolio.contact.domain.port.out.SendContactEmailPort;
import dev.crystofer.portfolio.shared.domain.EmailAddress;

/**
 * A promessa central do MVP 5, contra o Postgres de verdade: <strong>nenhuma mensagem se
 * perde</strong>.
 *
 * <p>Os testes de unidade provam as pecas; este prova a costura, que e onde a promessa mora. A
 * gravacao, o evento depois do commit, a execucao assincrona e o reprocessamento so existem juntos
 * - e cada um deles, isolado, passaria num teste que nao diz nada sobre o conjunto.
 *
 * <p><strong>O provedor de e-mail e o unico dublê.</strong> Tudo o mais e real: o banco, a
 * transacao, o publicador de eventos e o executor assincrono. Substituir o provedor e o que permite
 * escolher o desfecho - e a escolha do desfecho e justamente o que os cenarios abaixo variam.
 *
 * <p>⚠️ Esta classe declara um bean substituto, entao ela monta um contexto proprio do Spring - o
 * mesmo custo ja registrado para as duas classes do GitHub. E o preco de poder falhar sob comando.
 */
class ContactDeliveryIntegrationTest extends AbstractIntegrationTest {

  @MockitoBean private SendContactEmailPort provedor;

  @Autowired private SubmitContactMessageUseCase envio;

  @Autowired private RetryFailedEmailsUseCase reprocessamento;

  @Autowired private TransactionTemplate transacao;

  private static ContactMessage mensagem() {
    return ContactMessage.received(
        "Fulana",
        new EmailAddress("fulana@exemplo.com"),
        "Vaga backend",
        "Vi o portfolio e queria conversar.",
        null,
        "curl/8");
  }

  @BeforeEach
  void partirDeTabelaVazia() {
    jdbcTemplate.update("DELETE FROM contact_message");
  }

  @AfterEach
  void limpar() {
    jdbcTemplate.update("DELETE FROM contact_message");
  }

  private String estadoDaUnicaLinha() {
    return jdbcTemplate.queryForObject("SELECT email_status FROM contact_message", String.class);
  }

  /**
   * O caminho feliz, ponta a ponta.
   *
   * <p>A espera e por condicao e nao por sleep: o envio roda em outra thread, e prazo fixo em
   * maquina lenta e a receita do teste que falha uma vez a cada vinte sem ninguem saber por que.
   */
  @Test
  @DisplayName("deve gravar, notificar e marcar como enviada")
  void shouldStoreAndDeliver() {
    doNothing().when(provedor).send(any());

    long id = envio.submit(mensagem());

    assertThat(id).isPositive();
    await().atMost(Duration.ofSeconds(5)).until(() -> "SENT".equals(estadoDaUnicaLinha()));
    verify(provedor).send(any());
  }

  /**
   * <strong>O cenario que da nome ao MVP.</strong> O provedor recusa, e a mensagem continua inteira
   * no banco - e o unico efeito visivel e o estado.
   *
   * <p>E o item da Definition of Done que exige testar desligando o provedor. Sem ele, "nenhuma
   * mensagem se perde" seria uma afirmacao sobre codigo que ninguem exercitou no caso que importa.
   */
  @Test
  @DisplayName("deve preservar a mensagem quando o provedor de e-mail falha")
  void shouldKeepMessage_whenProviderFails() {
    doThrow(new IllegalStateException("provedor fora")).when(provedor).send(any());

    envio.submit(mensagem());

    await().atMost(Duration.ofSeconds(5)).until(() -> "FAILED".equals(estadoDaUnicaLinha()));

    var linha =
        jdbcTemplate.queryForMap("SELECT name, email, subject, message FROM contact_message");
    assertThat(linha.get("name")).isEqualTo("Fulana");
    assertThat(linha.get("email")).isEqualTo("fulana@exemplo.com");
    assertThat(linha.get("message")).isEqualTo("Vi o portfolio e queria conversar.");
  }

  /**
   * A volta: o provedor se recupera e o reprocessamento entrega o que ficou para tras.
   *
   * <p>Provar que a mensagem sobrevive a falha e metade; sem esta outra metade, ela sobreviveria
   * para sempre no banco sem nunca chegar a ninguem.
   */
  @Test
  @DisplayName("deve entregar no reprocessamento depois que o provedor volta")
  void shouldDeliverOnRetry_afterProviderRecovers() {
    doThrow(new IllegalStateException("provedor fora")).when(provedor).send(any());
    envio.submit(mensagem());
    await().atMost(Duration.ofSeconds(5)).until(() -> "FAILED".equals(estadoDaUnicaLinha()));

    doNothing().when(provedor).send(any());
    int entregues = reprocessamento.retryFailed();

    assertThat(entregues).isEqualTo(1);
    assertThat(estadoDaUnicaLinha()).isEqualTo("SENT");
  }

  /**
   * <strong>Transacao desfeita nao manda e-mail.</strong>
   *
   * <p>E o teste que prova o {@code AFTER_COMMIT}, e ele nao tem substituto: com um
   * {@code @EventListener} comum o envio aconteceria no instante da publicacao, dentro da
   * transacao, e este cenario entregaria uma notificacao sobre uma mensagem que o banco nao tem. Os
   * outros tres testes passariam identicos - eles nunca desfazem nada.
   */
  @Test
  @DisplayName("nao deve notificar quando a transacao e desfeita")
  void shouldNotDeliver_whenTransactionRollsBack() {
    assertThatThrownBy(
            () ->
                transacao.executeWithoutResult(
                    status -> {
                      envio.submit(mensagem());
                      throw new IllegalStateException("erro depois de gravar");
                    }))
        .isInstanceOf(IllegalStateException.class);

    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM contact_message", Integer.class))
        .isZero();
    verify(provedor, never()).send(any());
  }
}
