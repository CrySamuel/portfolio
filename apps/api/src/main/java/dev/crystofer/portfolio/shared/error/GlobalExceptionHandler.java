package dev.crystofer.portfolio.shared.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduz excecao em resposta, em um lugar so.
 *
 * <p>Formato RFC 9457 (Problem Details), pelo {@link ProblemDetail} nativo do Spring 6 - sem DTO de
 * erro proprio. A vantagem de usar o padrao nao e estetica: {@code application/problem+json} tem
 * media type registrado, entao cliente e ferramenta sabem interpretar sem ler documentacao.
 *
 * <p>Sem este ponto unico, cada controlador inventaria o proprio formato de erro e o cliente
 * precisaria de um ramo de codigo por endpoint.
 *
 * <p><strong>Por que estende {@link ResponseEntityExceptionHandler}.</strong> Sem essa heranca, o
 * {@code @ExceptionHandler(Exception.class)} logo abaixo era o unico tratador de tudo que nao fosse
 * {@link ResourceNotFoundException} - e "tudo" inclui as excecoes que o proprio Spring lanca para
 * sinalizar erro do <em>cliente</em>: rota inexistente ({@code NoResourceFoundException}), metodo
 * nao suportado, corpo malformado, media type errado. Todas saiam como 500.
 *
 * <p>O defeito foi encontrado em producao, no primeiro deploy, e nao em teste: o unico 404
 * exercitado era o de dominio, que tem tratador proprio e sempre funcionou. O 404 do framework
 * nunca tinha sido pedido a aplicacao.
 *
 * <p>A classe base ja traz um {@code @ExceptionHandler} para cada uma dessas excecoes, com o status
 * correto e corpo em {@code ProblemDetail}. Como o Spring escolhe sempre o tratador mais
 * especifico, a heranca nao rouba nada do que esta escrito aqui - ela apenas devolve ao catch-all o
 * papel que o Javadoc dele sempre alegou ter: cobrir o que nao foi previsto, e nada mais.
 *
 * <p>Ganho que vem junto: o {@code POST /contact} do MVP 5 precisa de 400 na falha de Bean
 * Validation e 415 no media type errado. Os dois passam a sair certos sem codigo novo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  ProblemDetail handleResourceNotFound(ResourceNotFoundException exception) {
    // WARN e nao ERROR: 404 e resposta prevista do protocolo, nao falha do
    // servidor. Subir o nivel aqui treinaria a equipe a ignorar ERROR.
    log.warn("Recurso ausente: {}", exception.resource(), exception);

    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    problem.setTitle("Recurso nao encontrado");
    problem.setType(ProblemTypes.de("resource-not-found"));
    return problem;
  }

  /**
   * Rede de seguranca para o que nao foi previsto.
   *
   * <p>O corpo e deliberadamente generico. A mensagem de uma excecao inesperada costuma carregar
   * caminho de arquivo, trecho de SQL ou nome de classe interna - informacao util para quem sonda a
   * aplicacao e inutil para quem so queria os dados. O detalhe real fica no log, com o stack trace
   * inteiro.
   *
   * <p>{@code ERROR} aqui e correto, e e por isso que a heranca da classe importa: com as excecoes
   * de erro do cliente caindo neste metodo, cada URL digitada errado virava uma linha {@code ERROR}
   * com stack trace - ruido que treina qualquer pessoa a ignorar o nivel, e que num plano gratuito
   * ainda consome cota de log. Agora so chega aqui o que de fato merece o alarme.
   */
  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception exception) {
    log.error("Falha nao tratada", exception);

    var problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisicao");
    problem.setTitle("Erro interno");
    problem.setType(ProblemTypes.de("internal-error"));
    return problem;
  }
}
