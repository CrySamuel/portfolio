package dev.crystofer.portfolio.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * O que a rede de seguranca ainda precisa pegar.
 *
 * <p>Companheiro do {@code HttpErrorContractTest}: aquele prova que erro de cliente parou de sair
 * como 500; este prova que erro de servidor <strong>continua</strong> saindo. A correcao daquele
 * defeito foi estreitar o alcance do catch-all, e estreitar demais o deixaria sem funcao - falha
 * que nenhum teste de rota pegaria, porque rota nenhuma lanca excecao inesperada de proposito.
 *
 * <p>Sem Spring: os tratadores sao metodos comuns, e chama-los direto custa milissegundos. Um
 * {@code @SpringBootTest} so para isto acrescentaria um contexto novo a suite pelo unico motivo de
 * verificar o valor de retorno de dois metodos.
 */
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("excecao inesperada continua virando 500 generico")
  void shouldStillReturnServerError_whenExceptionIsUnexpected() {
    var problem = handler.handleUnexpected(new IllegalStateException("pool esgotado"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(problem.getTitle()).isEqualTo("Erro interno");
    assertThat(problem.getType()).isEqualTo(URI.create("/errors/internal-error"));
  }

  /**
   * O corpo do 500 nao repassa a mensagem original.
   *
   * <p>Mensagem de excecao carrega caminho de arquivo, trecho de SQL e nome de classe interna. Esta
   * assercao e o que impede alguem de "melhorar" o detalhe generico colocando {@code
   * exception.getMessage()} nele.
   */
  @Test
  @DisplayName("o 500 nao vaza a mensagem da excecao")
  void shouldNotLeakExceptionMessage() {
    var segredo = "FATAL: senha do usuario portfolio em jdbc:postgresql://host/db";

    var problem = handler.handleUnexpected(new IllegalStateException(segredo));

    assertThat(problem.getDetail()).isEqualTo("Erro interno ao processar a requisicao");
    assertThat(problem.getDetail()).doesNotContain("jdbc", "senha", "FATAL");
  }

  @Test
  @DisplayName("o 404 de dominio segue com status, titulo e type proprios")
  void shouldKeepDomainNotFound() {
    var problem = handler.handleResourceNotFound(new ResourceNotFoundException("Profile", "1"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getTitle()).isEqualTo("Recurso nao encontrado");
    assertThat(problem.getType()).isEqualTo(URI.create("/errors/resource-not-found"));
  }
}
