package dev.crystofer.portfolio.contact.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.crystofer.portfolio.shared.domain.EmailAddress;

/**
 * Os invariantes de {@link ContactMessage}.
 *
 * <p>Eles repetem o que a {@code V5__create_contact_message_table} ja garante, e a repeticao e o
 * ponto: o banco recusa por ultimo, com uma violacao de restricao que nao diz nada ao visitante;
 * este tipo recusa primeiro, nomeando o campo. Um teste para cada lado porque cobrir so um deixaria
 * o outro livre para regredir - e a regressao seria invisivel enquanto o caminho normal
 * funcionasse.
 */
class ContactMessageTest {

  private static final EmailAddress EMAIL = new EmailAddress("fulana@exemplo.com");
  private static final String HASH = "a".repeat(64);

  @Test
  @DisplayName("deve nascer PENDING pela fabrica de recebimento")
  void shouldBeBorn_pending() {
    var mensagem = ContactMessage.received("Fulana", EMAIL, "Vaga", "Ola!", HASH, "curl/8");

    assertThat(mensagem.status()).isEqualTo(EmailStatus.PENDING);
  }

  /**
   * O estado de nascimento nao e parametro, e e isso que o teste protege.
   *
   * <p>Se a fabrica aceitasse o estado, uma chamada distraida gravaria a mensagem como SENT sem
   * nunca ter enviado nada - e o defeito apareceria como ausencia de e-mail, sem erro em lugar
   * nenhum. O construtor canonico continua aberto porque o adaptador que le do banco precisa dele.
   */
  @Test
  @DisplayName("deve permitir outro estado pelo construtor canonico, para quem le do banco")
  void shouldAllowOtherStatus_viaCanonicalConstructor() {
    var mensagem =
        new ContactMessage("Fulana", EMAIL, "Vaga", "Ola!", HASH, "curl/8", EmailStatus.FAILED);

    assertThat(mensagem.status()).isEqualTo(EmailStatus.FAILED);
  }

  @Test
  @DisplayName("deve produzir um valor novo ao trocar o estado, sem alterar o original")
  void shouldReturnNewValue_whenChangingStatus() {
    var original = ContactMessage.received("Fulana", EMAIL, "Vaga", "Ola!", HASH, "curl/8");

    var enviada = original.withStatus(EmailStatus.SENT);

    assertThat(enviada.status()).isEqualTo(EmailStatus.SENT);
    assertThat(original.status()).isEqualTo(EmailStatus.PENDING);
    assertThat(enviada.message()).isEqualTo(original.message());
  }

  @ParameterizedTest(name = "nome invalido: [{0}]")
  @ValueSource(strings = {"", "   ", "\t\n"})
  @DisplayName("deve recusar nome vazio ou so com espacos")
  void shouldReject_blankName(String nome) {
    assertThatThrownBy(() -> ContactMessage.received(nome, EMAIL, "Vaga", "Ola!", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Nome");
  }

  @Test
  @DisplayName("deve recusar mensagem acima de 5000 caracteres e aceitar exatamente 5000")
  void shouldEnforce_messageLimit() {
    assertThatThrownBy(
            () -> ContactMessage.received("Fulana", EMAIL, "Vaga", "x".repeat(5_001), null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Mensagem");

    assertThat(ContactMessage.received("Fulana", EMAIL, "Vaga", "x".repeat(5_000), null, null))
        .isNotNull();
  }

  /**
   * Os campos sao aparados antes de guardados.
   *
   * <p>Sem isso, " Fulana " e "Fulana" seriam nomes diferentes no banco, e o limite de tamanho
   * contaria espacos que nao sao conteudo - uma mensagem de 5000 caracteres com um espaco no fim
   * seria recusada por 5001.
   */
  @Test
  @DisplayName("deve aparar os campos de texto")
  void shouldTrim_textFields() {
    var mensagem = ContactMessage.received("  Fulana  ", EMAIL, "  Vaga  ", "  Ola!  ", null, null);

    assertThat(mensagem.name()).isEqualTo("Fulana");
    assertThat(mensagem.subject()).isEqualTo("Vaga");
    assertThat(mensagem.message()).isEqualTo("Ola!");
  }

  /**
   * Ausencia e string em branco viram a mesma coisa, e ela e {@code null}.
   *
   * <p>A coluna e nulavel; guardar {@code ""} ali afirmaria que o cliente mandou um user agent
   * vazio, o que nao aconteceu. A distincao importa numa auditoria, que e o unico uso desses dois
   * campos.
   */
  @Test
  @DisplayName("deve converter hash e user agent em branco para nulo")
  void shouldNormalizeBlank_toNull() {
    var mensagem = ContactMessage.received("Fulana", EMAIL, "Vaga", "Ola!", "   ", "  ");

    assertThat(mensagem.ipHash()).isNull();
    assertThat(mensagem.userAgent()).isNull();
  }

  /**
   * O hash e conferido no formato, e o motivo esta no tipo da coluna.
   *
   * <p>{@code CHAR(64)} no PostgreSQL <strong>preenche com espacos</strong> o que for mais curto.
   * Um hash truncado entraria e voltaria diferente do que foi gravado, e a comparacao entre
   * remetentes passaria a falhar em silencio - so perceptivel numa auditoria, que e justamente
   * quando ninguem quer descobrir isso.
   */
  /**
   * Os quatro jeitos de o hash estar errado, e {@code @MethodSource} porque valor de anotacao
   * precisa ser constante de compilacao - {@code "a".repeat(63)} nao e.
   */
  static Stream<Arguments> hashesInvalidos() {
    return Stream.of(
        arguments("curto demais", "abc"),
        arguments("nao hexadecimal", "z".repeat(64)),
        arguments("um caractere a menos", "a".repeat(63)),
        arguments("um caractere a mais", "a".repeat(65)));
  }

  @ParameterizedTest(name = "hash invalido: {0}")
  @MethodSource("hashesInvalidos")
  @DisplayName("deve recusar hash que nao seja 64 caracteres hexadecimais")
  void shouldReject_malformedHash(String caso, String hash) {
    assertThatThrownBy(() -> ContactMessage.received("Fulana", EMAIL, "Vaga", "Ola!", hash, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Hash de IP");
  }

  @Test
  @DisplayName("deve aceitar hash hexadecimal em maiusculas")
  void shouldAccept_uppercaseHash() {
    assertThat(ContactMessage.received("Fulana", EMAIL, "Vaga", "Ola!", "A".repeat(64), null))
        .isNotNull();
  }

  @Test
  @DisplayName("deve recusar e-mail e estado nulos")
  void shouldReject_nullEmailAndStatus() {
    assertThatThrownBy(() -> ContactMessage.received("Fulana", null, "Vaga", "Ola!", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("E-mail");

    assertThatThrownBy(() -> new ContactMessage("Fulana", EMAIL, "Vaga", "Ola!", null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Estado de entrega");
  }
}
