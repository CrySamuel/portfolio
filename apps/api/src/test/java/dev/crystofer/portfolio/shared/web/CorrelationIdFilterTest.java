package dev.crystofer.portfolio.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private static final String UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

  private final CorrelationIdFilter filtro = new CorrelationIdFilter();

  /** O valor que estava no MDC enquanto a requisicao passava pelo resto da cadeia. */
  private String noMdcDuranteACadeia(
      MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
    var visto = new AtomicReference<String>();
    filtro.doFilter(
        request, response, (req, res) -> visto.set(MDC.get(CorrelationIdFilter.MDC_KEY)));
    return visto.get();
  }

  @Test
  @DisplayName("usa o identificador que o site mandou, no log e na resposta")
  void shouldKeepTheIncomingId() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.HEADER, "3f1c9a2e-7b44-4c1d-9e0a-5b6c7d8e9f01");
    var response = new MockHttpServletResponse();

    assertThat(noMdcDuranteACadeia(request, response))
        .isEqualTo("3f1c9a2e-7b44-4c1d-9e0a-5b6c7d8e9f01");
    assertThat(response.getHeader(CorrelationIdFilter.HEADER))
        .isEqualTo("3f1c9a2e-7b44-4c1d-9e0a-5b6c7d8e9f01");
  }

  @Test
  @DisplayName("sem cabecalho, gera um UUID - toda requisicao sai com um")
  void shouldGenerateAnId_whenNoneArrives() throws Exception {
    var response = new MockHttpServletResponse();

    String gerado = noMdcDuranteACadeia(new MockHttpServletRequest(), response);

    assertThat(gerado).matches(UUID);
    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(gerado);
  }

  /**
   * O valor vai para o log. Quebra de linha forjaria uma linha inteira; texto longo afogaria o
   * registro; e o resto do formato nao tem por que aceitar.
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "curto",
        "id-com\nlinha-forjada",
        "id com espaco 123",
        "<script>alert(1)</script>",
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      })
  @DisplayName("troca por um UUID o valor fora do formato de identificador")
  void shouldReplaceMalformedIds(String recebido) throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.HEADER, recebido);

    assertThat(noMdcDuranteACadeia(request, new MockHttpServletResponse())).matches(UUID);
  }

  @Test
  @DisplayName("limpa o MDC no fim, mesmo quando a cadeia lanca")
  void shouldClearTheMdc_evenWhenTheChainThrows() {
    try {
      filtro.doFilter(
          new MockHttpServletRequest(),
          new MockHttpServletResponse(),
          (req, res) -> {
            throw new IllegalStateException("falha no meio da requisicao");
          });
    } catch (Exception esperado) {
      // a excecao sobe; o que importa e o estado da thread depois dela
    }

    assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
  }
}
