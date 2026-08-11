package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import dev.crystofer.portfolio.shared.web.ServiceKeyAuthFilter;

/**
 * A porta da API, testada dos dois lados.
 *
 * <p>Este arquivo e a guarda da secao 2.4: a partir do commit 23 a API tem endereco publico na
 * internet, e sem a chave qualquer pessoa que o descobrisse leria os endpoints - e, no MVP 5,
 * escreveria neles pelo formulario de contato.
 *
 * <p>Tao importante quanto provar que a porta fecha e provar <strong>o que ela nao fecha</strong>.
 * O {@code /actuator/health} precisa continuar aberto porque e por ele que o Render decide se o
 * servico subiu: protege-lo faria a plataforma concluir que a aplicacao morreu e reinicia-la em
 * loop - uma falha que so apareceria em producao, no dia do deploy.
 */
class ServiceKeyAuthFilterTest extends AbstractIntegrationTest {

  private static final String PROTEGIDO = "/api/v1/profile";

  private final BasicJsonTester json = new BasicJsonTester(getClass());

  @Test
  @DisplayName("deve recusar em problem+json quando nao vem cabecalho nenhum")
  void shouldRejectWithProblemDetail_whenHeaderIsMissing() {
    ResponseEntity<String> response = restTemplate.getForEntity(PROTEGIDO, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getHeaders().getContentType())
        .isNotNull()
        .satisfies(
            type -> assertThat(type.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue());

    String body = response.getBody();
    assertThat(json.from(body)).extractingJsonPathNumberValue("$.status").isEqualTo(401);
    assertThat(json.from(body))
        .extractingJsonPathStringValue("$.type")
        .isEqualTo("/errors/unauthorized");

    // O corpo do erro nao pode carregar o conteudo que ele acabou de negar.
    assertThat(body).doesNotContain("fullName");
  }

  /**
   * Chave errada responde igual a chave ausente.
   *
   * <p>Distinguir as duas coisas na resposta so ajudaria quem esta tentando adivinhar: saber que o
   * cabecalho foi reconhecido e o valor recusado ja e meia informacao.
   */
  @Test
  @DisplayName("deve recusar a chave errada com a mesma resposta da chave ausente")
  void shouldRejectWithTheSameResponse_whenKeyIsWrong() {
    ResponseEntity<String> response = comChave("chave-errada-mas-do-mesmo-tamanho-ok");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(json.from(response.getBody()))
        .extractingJsonPathStringValue("$.detail")
        .isEqualTo("Chave de servico ausente ou invalida");
  }

  @Test
  @DisplayName("deve deixar passar a chave correta")
  void shouldAllow_whenKeyMatches() {
    ResponseEntity<String> response = comChave(CHAVE_DE_SERVICO);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(json.from(response.getBody()))
        .extractingJsonPathStringValue("$.fullName")
        .isNotNull();
  }

  /**
   * O que fica de fora do filtro, e por que cada um.
   *
   * <p>{@code /actuator/health} e a sonda da plataforma. {@code /v3/api-docs} e publico porque a
   * Definition of Done do MVP 1 pede Swagger acessivel - documentacao aberta com dados fechados e a
   * combinacao certa para um portfolio: quem avalia o repositorio le o contrato sem credencial, e
   * ninguem le o conteudo sem a chave.
   */
  @Test
  @DisplayName("saude e documentacao continuam publicas, sem chave")
  void shouldKeepHealthAndDocsPublic() {
    assertThat(restTemplate.getForEntity("/actuator/health", String.class).getStatusCode())
        .isEqualTo(HttpStatus.OK);
    assertThat(restTemplate.getForEntity("/v3/api-docs", String.class).getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  private ResponseEntity<String> comChave(String chave) {
    var headers = new HttpHeaders();
    headers.set(ServiceKeyAuthFilter.HEADER, chave);
    return restTemplate.exchange(
        PROTEGIDO, HttpMethod.GET, new HttpEntity<>(headers), String.class);
  }
}
