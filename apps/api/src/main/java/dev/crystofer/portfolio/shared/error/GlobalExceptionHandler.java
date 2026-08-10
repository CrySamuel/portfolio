package dev.crystofer.portfolio.shared.error;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz excecao em resposta, em um lugar so.
 *
 * <p>Formato RFC 9457 (Problem Details), pelo {@link ProblemDetail} nativo do Spring 6 - sem DTO de
 * erro proprio. A vantagem de usar o padrao nao e estetica: {@code application/problem+json} tem
 * media type registrado, entao cliente e ferramenta sabem interpretar sem ler documentacao.
 *
 * <p>Sem este ponto unico, cada controlador inventaria o proprio formato de erro e o cliente
 * precisaria de um ramo de codigo por endpoint.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * URI relativa de proposito.
   *
   * <p>O RFC 9457 aceita referencia relativa em {@code type}, e o dominio deste portfolio ainda nao
   * foi escolhido - ele e pre-requisito do commit 23. Fixar {@code https://portfolio.dev/...} agora
   * seria publicar um endereco que nao controlamos, e trocar depois quebraria o contrato de quem ja
   * consumisse. Vira absoluta quando o dominio existir.
   */
  private static final String TYPE_PREFIX = "/errors/";

  @ExceptionHandler(ResourceNotFoundException.class)
  ProblemDetail handleResourceNotFound(ResourceNotFoundException exception) {
    // WARN e nao ERROR: 404 e resposta prevista do protocolo, nao falha do
    // servidor. Subir o nivel aqui treinaria a equipe a ignorar ERROR.
    log.warn("Recurso ausente: {}", exception.resource(), exception);

    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    problem.setTitle("Recurso nao encontrado");
    problem.setType(URI.create(TYPE_PREFIX + "resource-not-found"));
    return problem;
  }

  /**
   * Rede de seguranca para o que nao foi previsto.
   *
   * <p>O corpo e deliberadamente generico. A mensagem de uma excecao inesperada costuma carregar
   * caminho de arquivo, trecho de SQL ou nome de classe interna - informacao util para quem sonda a
   * aplicacao e inutil para quem so queria os dados. O detalhe real fica no log, com o stack trace
   * inteiro.
   */
  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception exception) {
    log.error("Falha nao tratada", exception);

    var problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisicao");
    problem.setTitle("Erro interno");
    problem.setType(URI.create(TYPE_PREFIX + "internal-error"));
    return problem;
  }
}
