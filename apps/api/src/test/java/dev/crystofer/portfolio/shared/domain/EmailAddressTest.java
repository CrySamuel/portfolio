package dev.crystofer.portfolio.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailAddressTest {

  @Test
  @DisplayName("deve normalizar caixa e espacos ao redor")
  void shouldNormalize_whenValueHasCaseAndPadding() {
    // when
    var email = EmailAddress.of("  Crystofer.Demetino@Example.COM  ");

    // then
    assertThat(email.value()).isEqualTo("crystofer.demetino@example.com");
  }

  @Test
  @DisplayName("deve considerar iguais dois enderecos escritos com caixas diferentes")
  void shouldBeEqual_whenOnlyCaseDiffers() {
    // when
    var lower = EmailAddress.of("contato@example.com");
    var upper = EmailAddress.of("CONTATO@EXAMPLE.COM");

    // then
    assertThat(lower).isEqualTo(upper);
  }

  @ParameterizedTest
  @DisplayName("deve rejeitar endereco estruturalmente invalido")
  @ValueSource(
      strings = {
        "sem-arroba.com",
        "@sem-local.com",
        "sem-dominio@",
        "sem-tld@example",
        "com espaco@example.com",
        "dois@@example.com"
      })
  void shouldReject_whenStructureIsInvalid(String invalid) {
    // when
    var thrown = catchThrowable(() -> EmailAddress.of(invalid));

    // then
    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("formato invalido");
  }

  @Test
  @DisplayName("deve rejeitar endereco vazio")
  void shouldReject_whenValueIsBlank() {
    // when
    var thrown = catchThrowable(() -> EmailAddress.of("   "));

    // then
    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nao pode ser vazio");
  }

  @Test
  @DisplayName("deve rejeitar endereco acima do limite do RFC 5321")
  void shouldReject_whenValueExceedsMaxLength() {
    // given
    var local = "a".repeat(250);

    // when
    var thrown = catchThrowable(() -> EmailAddress.of(local + "@example.com"));

    // then
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("254");
  }

  @Test
  @DisplayName("deve mascarar o endereco na representacao textual, para nao vazar PII em log")
  void shouldMask_whenConvertedToString() {
    // given
    var email = EmailAddress.of("crystofer@example.com");

    // when
    var texto = String.valueOf(email);

    // then
    assertThat(texto).isEqualTo("c***@example.com").doesNotContain("crystofer@");
  }
}
