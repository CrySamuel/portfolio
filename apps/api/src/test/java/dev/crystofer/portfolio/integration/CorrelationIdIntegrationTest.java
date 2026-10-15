package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import dev.crystofer.portfolio.shared.web.CorrelationIdFilter;
import dev.crystofer.portfolio.shared.web.ServiceKeyAuthFilter;

/**
 * O identificador de correlacao com o servidor de verdade: o filtro registrado na ordem certa, o
 * cabecalho de volta e o campo no corpo do erro - inclusive no 401, que nasce antes do Spring MVC.
 */
class CorrelationIdIntegrationTest extends AbstractIntegrationTest {

  private static final String ID = "5d2e8b1a-0c3f-4a7e-9b6d-1e2f3a4b5c6d";

  private final BasicJsonTester json = new BasicJsonTester(getClass());

  private ResponseEntity<String> get(String caminho, boolean comChave) {
    var cabecalhos = new HttpHeaders();
    cabecalhos.set(CorrelationIdFilter.HEADER, ID);
    if (comChave) cabecalhos.set(ServiceKeyAuthFilter.HEADER, CHAVE_DE_SERVICO);
    return restTemplate.exchange(
        caminho, HttpMethod.GET, new HttpEntity<>(cabecalhos), String.class);
  }

  @Test
  @DisplayName("devolve no cabecalho o identificador que recebeu")
  void shouldEchoTheIdInTheResponseHeader() {
    var response = get("/api/v1/profile", true);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getFirst(CorrelationIdFilter.HEADER)).isEqualTo(ID);
  }

  @Test
  @DisplayName("poe o identificador no corpo do erro de dominio")
  void shouldPutTheIdInTheProblemDetail() {
    var response = get("/api/v1/projects/nao-existe", true);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(json.from(response.getBody()))
        .extractingJsonPathStringValue("$.correlationId")
        .isEqualTo(ID);
  }

  @Test
  @DisplayName("poe o identificador ate na recusa da chave, que nasce antes do Spring MVC")
  void shouldPutTheIdInTheUnauthorizedResponse() {
    var response = get("/api/v1/profile", false);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getHeaders().getFirst(CorrelationIdFilter.HEADER)).isEqualTo(ID);
    assertThat(json.from(response.getBody()))
        .extractingJsonPathStringValue("$.correlationId")
        .isEqualTo(ID);
  }
}
