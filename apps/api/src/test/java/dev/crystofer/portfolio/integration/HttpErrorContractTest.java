package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Erro de cliente precisa sair como erro de cliente.
 *
 * <p>Esta classe existe por um defeito encontrado em producao, no dia do primeiro deploy: toda rota
 * inexistente respondia <strong>500</strong>. O {@code @ExceptionHandler(Exception.class)} do
 * tratador global - escrito como rede de seguranca para o que nao foi previsto - capturava tambem
 * as excecoes que o proprio Spring lanca para sinalizar erro do cliente, e as reclassificava como
 * falha do servidor.
 *
 * <p>O defeito nao aparecia em teste nenhum porque o unico 404 exercitado era o do dominio, que tem
 * tratador proprio e sempre funcionou. O 404 <em>do framework</em> nunca tinha sido pedido a
 * aplicacao - guarda que nunca disparou, o padrao recorrente deste projeto.
 *
 * <p>Por que importa alem da etiqueta: 500 diz a quem consome que o servidor quebrou, entao cliente
 * bem-comportado repete a requisicao e monitoramento acorda alguem. Alem disso cada URL digitada
 * errado virava uma linha {@code ERROR} com stack trace no log - que, num plano gratuito, e cota. E
 * a secao 3.8 do plano promete resposta uniforme em RFC 9457 para <em>todos</em> os erros.
 *
 * <p>Os caminhos sao exercitados por HTTP de verdade, e nao por {@code MockMvc}: o despacho para o
 * handler de recurso estatico - que e quem lanca a excecao de rota ausente - depende do container
 * servlet, e e justamente ele que o {@code MockMvc} simula.
 */
class HttpErrorContractTest extends AbstractIntegrationTest {

  private final BasicJsonTester json = new BasicJsonTester(getClass());

  /**
   * Rota que nao existe.
   *
   * <p>O caminho fica fora de {@code /api/**} de proposito: dentro dele quem responde e o filtro da
   * chave de servico, com 401, antes de o roteamento acontecer. Aqui o assunto e o roteamento.
   */
  @Test
  @DisplayName("deve responder 404 em problem+json quando a rota nao existe")
  void shouldRespondNotFound_whenRouteDoesNotExist() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/caminho-que-nao-existe", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getHeaders().getContentType())
        .isNotNull()
        .satisfies(
            type -> assertThat(type.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue());
    assertThat(json.from(response.getBody()))
        .extractingJsonPathNumberValue("$.status")
        .isEqualTo(404);
  }

  /**
   * O mesmo vale sob um prefixo que existe.
   *
   * <p>{@code /actuator} responde de verdade, entao este caso prova que a correcao nao se limita a
   * caminhos completamente desconhecidos.
   */
  @Test
  @DisplayName("deve responder 404 sob um prefixo que existe")
  void shouldRespondNotFound_whenPathUnderKnownPrefixDoesNotExist() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/actuator/naoexiste", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  /**
   * Metodo errado em rota que existe.
   *
   * <p>{@code /actuator/health} e publico - nao passa pelo filtro da chave - e so responde GET.
   * Serve como o endpoint de metodo unico que o teste precisa sem depender de credencial.
   */
  @Test
  @DisplayName("deve responder 405 quando o metodo nao e suportado pela rota")
  void shouldRespondMethodNotAllowed_whenMethodIsNotSupported() {
    ResponseEntity<String> response =
        restTemplate.exchange("/actuator/health", HttpMethod.POST, HttpEntity.EMPTY, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
  }

  /**
   * O caminho que revelou o defeito.
   *
   * <p>{@code /swagger-ui} sem {@code .html} nao e rota registrada pelo springdoc - a UI mora em
   * {@code /swagger-ui/index.html}, e {@code /swagger-ui.html} redireciona para la. Digitar o nome
   * pela metade e o que uma pessoa faz, e a resposta honesta e 404, nao 500.
   */
  @Test
  @DisplayName("deve responder 404, e nao 500, no /swagger-ui pela metade")
  void shouldNotRespondServerError_whenSwaggerPathIsIncomplete() {
    ResponseEntity<String> response = restTemplate.getForEntity("/swagger-ui", String.class);

    assertThat(response.getStatusCode().is5xxServerError()).isFalse();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  /** A documentacao continua servida onde ela de fato mora. */
  @Test
  @DisplayName("a UI do swagger continua respondendo no caminho completo")
  void shouldKeepSwaggerUiServed() {
    assertThat(restTemplate.getForEntity("/swagger-ui/index.html", String.class).getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }
}
