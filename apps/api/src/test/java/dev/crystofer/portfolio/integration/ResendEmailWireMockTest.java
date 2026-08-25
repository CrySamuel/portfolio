package dev.crystofer.portfolio.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import dev.crystofer.portfolio.contact.domain.model.ContactMessage;
import dev.crystofer.portfolio.contact.domain.port.out.SendContactEmailPort;
import dev.crystofer.portfolio.shared.domain.EmailAddress;

/**
 * O adaptador do provedor de e-mail, contra um dublê que erra sob comando.
 *
 * <p><strong>Existe porque a credencial de verdade nao pode ser usada em teste, e nem
 * deveria.</strong> Um teste que envia e-mail de verdade gasta cota, depende de rede e enche a
 * caixa de entrada de alguem a cada execucao da suite. O que precisa ser provado aqui e outra
 * coisa: que o adaptador monta a requisicao certa e que <em>falha</em> quando o provedor recusa - e
 * nenhum dos dois se reproduz de forma deterministica contra a API real.
 *
 * <p>A chave e falsa e o endereco aponta para o dublê, e e isso que faz o {@code EmailConfig}
 * escolher o adaptador do provedor em vez do de log - o mesmo caminho que producao segue.
 *
 * <p>O servidor sobe uma vez por JVM, em bloco estatico, e nunca e parado a mao (secao 4.21).
 */
class ResendEmailWireMockTest extends AbstractIntegrationTest {

  private static final String ENVIO = "/emails";

  /**
   * HTTP/2 desligado no dublê, e a razao apareceu medindo.
   *
   * <p>Com ele ligado, o cliente do JDK negocia h2c e o POST com corpo termina em {@code
   * IOException: Received RST_STREAM: Stream cancelled} - os tres cenarios de sucesso falham, e os
   * seis de recusa <strong>passam pelo motivo errado</strong>, porque o erro de transporte tambem e
   * {@code RuntimeException}. Teste que passa por acidente e pior que teste que falha.
   *
   * <p>Quem cede e o dublê, e nao a aplicacao: o provedor de verdade fala HTTP/2 e nao ha razao
   * para o cliente de producao desistir disso por causa de um servidor de teste.
   */
  private static final WireMockServer PROVEDOR =
      new WireMockServer(WireMockConfiguration.options().dynamicPort().http2PlainDisabled(true));

  static {
    PROVEDOR.start();
  }

  @DynamicPropertySource
  static void apontarParaODuble(DynamicPropertyRegistry registry) {
    registry.add("portfolio.email.base-url", PROVEDOR::baseUrl);
    // Chave falsa, e a presenca dela e o que importa: sem chave o EmailConfig
    // escolheria o adaptador de log, e este teste exercitaria outra classe.
    registry.add("portfolio.email.api-key", () -> "re_chave_de_teste");
    registry.add("portfolio.email.recipient", () -> "dono@exemplo.com");
    registry.add("portfolio.email.sender", () -> "portfolio@exemplo.com");
  }

  @Autowired private SendContactEmailPort adaptador;

  @BeforeEach
  void limparODuble() {
    PROVEDOR.resetAll();
  }

  private static ContactMessage mensagem(String nome) {
    return ContactMessage.received(
        nome,
        new EmailAddress("fulana@exemplo.com"),
        "Vaga backend",
        "Vi o portfolio e queria conversar.",
        null,
        null);
  }

  /**
   * O caminho feliz, e o que ele afirma e o formato da requisicao.
   *
   * <p><strong>O {@code reply_to} e o visitante e o {@code from} nao.</strong> E a assercao que
   * protege a entregabilidade: pôr o e-mail do visitante como remetente seria falsificacao - o
   * dominio de envio nao pertence a ele -, e SPF e DKIM reprovariam a mensagem em spam ou na
   * recusa. Um teste que so conferisse "chamou o provedor" deixaria isso passar.
   */
  @Test
  @DisplayName("deve enviar com o visitante no reply-to e o remetente proprio no from")
  void shouldSend_withVisitorAsReplyTo() {
    PROVEDOR.stubFor(post(urlPathEqualTo(ENVIO)).willReturn(aResponse().withStatus(200)));

    assertThatNoException().isThrownBy(() -> adaptador.send(mensagem("Fulana")));

    var enviadas = PROVEDOR.findAll(postRequestedFor(urlPathEqualTo(ENVIO)));
    assertThat(enviadas).hasSize(1);

    String corpo = enviadas.get(0).getBodyAsString();
    assertThat(corpo).contains("\"reply_to\":\"fulana@exemplo.com\"");
    assertThat(corpo).contains("\"from\":\"portfolio@exemplo.com\"");
    assertThat(corpo).contains("\"to\":\"dono@exemplo.com\"");

    // O texto do visitante vai no corpo, e nao no assunto.
    assertThat(corpo).contains("Vi o portfolio e queria conversar.");
  }

  /**
   * <strong>Injecao de cabecalho.</strong> Um nome com quebra de linha nao pode partir o assunto em
   * dois - cabecalho de e-mail usa a quebra como separador, e um segundo cabecalho forjado ali
   * acrescentaria destinatario oculto a uma mensagem que sai em nome do dono.
   *
   * <p>O dominio nao recusa a quebra de linha, e nao deveria: isso e regra do transporte, e nao do
   * conteudo. Quem tem de conhece-la e o adaptador, e e por isso que o teste vive aqui.
   */
  @Test
  @DisplayName("deve neutralizar quebra de linha no nome, que iria para o assunto")
  void shouldNeutralize_headerInjection() {
    PROVEDOR.stubFor(post(urlPathEqualTo(ENVIO)).willReturn(aResponse().withStatus(200)));

    adaptador.send(mensagem("Fulana\nBcc: invasor@exemplo.com"));

    String corpo =
        PROVEDOR.findAll(postRequestedFor(urlPathEqualTo(ENVIO))).get(0).getBodyAsString();

    // O JSON escaparia a quebra como \n; o que se exige e que ela nao exista no
    // assunto de forma alguma - nem crua, nem escapada.
    int inicio = corpo.indexOf("\"subject\":\"");
    String assunto = corpo.substring(inicio, corpo.indexOf('"', inicio + 11));
    assertThat(assunto).doesNotContain("\\n").doesNotContain("\\r");
  }

  /**
   * As respostas que o provedor realmente da quando recusa.
   *
   * <p><strong>401 e o caso mais provavel, e nao o 500.</strong> Chave revogada, chave trocada sem
   * atualizar o painel, chave com permissao insuficiente - todos chegam como 401. O 429 e cota. Os
   * dois precisam <em>lancar</em>, porque e a excecao que faz o caso de uso gravar {@code FAILED} e
   * o reprocessamento tentar de novo mais tarde.
   *
   * <p>Um adaptador que engolisse a falha marcaria a mensagem como entregue sem ela ter saido - o
   * unico desfecho que esta cadeia inteira existe para impedir.
   */
  @ParameterizedTest(name = "status {0} deve lancar")
  @ValueSource(ints = {401, 403, 422, 429, 500, 503})
  @DisplayName("deve lancar quando o provedor recusa, para que o desfecho vire FAILED")
  void shouldThrow_whenProviderRejects(int status) {
    PROVEDOR.stubFor(post(urlPathEqualTo(ENVIO)).willReturn(aResponse().withStatus(status)));

    assertThatThrownBy(() -> adaptador.send(mensagem("Fulana")))
        .isInstanceOf(RuntimeException.class);
  }

  /**
   * A credencial vai no cabecalho, e o teste existe porque o esquecimento e silencioso.
   *
   * <p>Sem o {@code Authorization}, o provedor responde 401 e a mensagem vira {@code FAILED} - o
   * sintoma seria identico ao de uma chave errada, e a investigacao comecaria no painel do provedor
   * em vez de no codigo.
   */
  @Test
  @DisplayName("deve mandar a credencial no cabecalho de autorizacao")
  void shouldSend_authorizationHeader() {
    PROVEDOR.stubFor(post(urlPathEqualTo(ENVIO)).willReturn(aResponse().withStatus(200)));

    adaptador.send(mensagem("Fulana"));

    assertThat(
            PROVEDOR
                .findAll(postRequestedFor(urlPathEqualTo(ENVIO)))
                .get(0)
                .getHeader("Authorization"))
        .isEqualTo("Bearer re_chave_de_teste");
  }
}
