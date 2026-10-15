package dev.crystofer.portfolio.shared.error;

import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import dev.crystofer.portfolio.shared.web.CorrelationIdFilter;

/**
 * Poe o {@code correlationId} em todo corpo de erro, como a secao 3.8 do plano mostra.
 *
 * <p>Os {@code ProblemDetail} nascem em varios lugares: nos tratadores proprios do {@link
 * GlobalExceptionHandler}, nos que ele herda do Spring (o 400 de validacao, o 405) e no caminho de
 * cada controlador. Este e o ponto por onde todos passam antes de virar JSON - um {@code
 * ResponseBodyAdvice} -, e por isso o campo e acrescentado aqui, e nao em cada um. A unica excecao
 * e a recusa da chave de servico, que acontece num filtro, antes do Spring MVC, e poe o campo por
 * conta propria.
 *
 * <p>Quem le e quem chamou: com o identificador no corpo, a mensagem de erro que o site recebe ja
 * diz qual linha procurar no log da API.
 */
@RestControllerAdvice
class CorrelationIdProblemAdvice implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    if (body instanceof ProblemDetail problem) {
      String id = MDC.get(CorrelationIdFilter.MDC_KEY);
      if (id != null) problem.setProperty(CorrelationIdFilter.MDC_KEY, id);
    }
    return body;
  }
}
