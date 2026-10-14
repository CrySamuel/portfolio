package dev.crystofer.portfolio.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;

import dev.crystofer.portfolio.shared.domain.ContentLanguage;

/**
 * A leitura do {@code Accept-Language}, sem Spring.
 *
 * <p>Os cabecalhos sao os que navegadores de verdade mandam, e cada caso nomeia o que esta em jogo.
 * O que se prova aqui e a decisao, e nao a JDK: a parte da RFC 4647 que vem pronta esta coberta na
 * medida em que a decisao depende dela - pesos, curinga e casamento por prefixo.
 */
class LanguageNegotiationTest {

  @ParameterizedTest(name = "\"{0}\" -> {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        // O pedido exato de cada idioma.
        "en-US                       | EN_US",
        "pt-BR                       | PT_BR",
        // Sem regiao: e o que o Accept-Language mais curto manda.
        "en                          | EN_US",
        "pt                          | PT_BR",
        // A caixa nao muda nada - a tag BCP 47 nao diferencia maiusculas.
        "EN-us                       | EN_US",
        // O navegador em ingles do Chrome, com o peso decrescente de sempre.
        "en-US,en;q=0.9              | EN_US",
        // O peso decide, e nao a ordem em que as preferencias aparecem.
        "pt-BR;q=0.5,en-US;q=0.9     | EN_US",
        // Regiao que nao existe aqui: o idioma vale mais que a regiao.
        "en-GB                       | EN_US",
        "pt-PT                       | PT_BR",
        // A primeira preferencia nao existe, a segunda existe.
        "fr-FR,fr;q=0.9,en;q=0.8     | EN_US",
        // Nenhum idioma suportado: o padrao, e nao erro.
        "fr-FR                       | PT_BR",
        "de,ja;q=0.5                 | PT_BR",
        // O curinga aceita qualquer um, e o primeiro e o padrao.
        "*                           | PT_BR",
      })
  @DisplayName("deve escolher o idioma mais preferido que existe")
  void shouldChooseTheMostPreferredSupportedLanguage(String cabecalho, ContentLanguage esperado) {
    assertThat(LanguageNegotiation.negociar(cabecalho)).isEqualTo(esperado);
  }

  /**
   * Peso zero significa "nao aceito", e e a unica forma de o cliente recusar um idioma. Quem manda
   * {@code en;q=0} esta dizendo que nao quer ingles - entregar ingles por ser o unico que casou
   * seria ler o cabecalho ao contrario.
   */
  @Test
  @DisplayName("deve respeitar o idioma recusado com peso zero")
  void shouldHonorZeroWeight_asRefusal() {
    assertThat(LanguageNegotiation.negociar("en;q=0,fr")).isEqualTo(ContentLanguage.PT_BR);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  @DisplayName("deve entregar o padrao quando nao ha pedido")
  void shouldFallBackToDefault_whenThereIsNoPreference(String cabecalho) {
    assertThat(LanguageNegotiation.negociar(cabecalho)).isEqualTo(ContentLanguage.PT_BR);
  }

  /**
   * Cabecalho malformado e erro do cliente, e mesmo assim vira 200 em portugues. Um 400 aqui
   * puniria o visitante por um defeito do navegador ou do proxy no caminho - e o conteudo existe
   * para ele de qualquer forma.
   */
  @ParameterizedTest
  @ValueSource(strings = {"en-US;q=abc", ";;;", "en_US", "en-US;q=2"})
  @DisplayName("deve entregar o padrao quando o cabecalho e malformado")
  void shouldFallBackToDefault_whenHeaderIsMalformed(String cabecalho) {
    assertThat(LanguageNegotiation.negociar(cabecalho)).isEqualTo(ContentLanguage.PT_BR);
  }

  @Test
  @DisplayName("deve declarar o idioma entregue e variar pelo pedido")
  void shouldDeclareLanguage_andVaryByRequest() {
    var resposta =
        LanguageNegotiation.responder(ContentLanguage.EN_US, CacheControl.noCache(), "corpo");

    assertThat(resposta.getHeaders().getFirst(HttpHeaders.CONTENT_LANGUAGE)).isEqualTo("en-US");
    assertThat(resposta.getHeaders().getVary()).containsExactly(HttpHeaders.ACCEPT_LANGUAGE);
    assertThat(resposta.getHeaders().getCacheControl()).isEqualTo("no-cache");
    assertThat(resposta.getBody()).isEqualTo("corpo");
  }
}
