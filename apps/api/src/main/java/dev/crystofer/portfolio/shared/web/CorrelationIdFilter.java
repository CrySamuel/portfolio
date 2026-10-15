package dev.crystofer.portfolio.shared.web;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * O identificador que liga o log do site ao log da API numa mesma requisicao (secao 3.11 do plano).
 *
 * <p>O site e a API rodam em servicos diferentes - Vercel e Render -, com logs que nao se veem.
 * Quando o envio do contato falha, o site registra o identificador que mandou; a API o pos em cada
 * linha de log daquela requisicao, e o devolve no cabecalho e no corpo do erro. Com o mesmo valor
 * nos dois lados, uma busca acha as duas metades da historia.
 *
 * <p><strong>O valor recebido so e aceito com formato de identificador</strong> - letras, digitos e
 * hifen, de 8 a 64 caracteres. Ele vai para o log, e um cabecalho com quebra de linha ou texto
 * longo forjaria linhas ou afogaria o registro. Fora do formato, ou ausente, a API gera o seu: toda
 * requisicao sai com um, inclusive a que chega sem o site no meio.
 *
 * <p>Roda antes de todo outro filtro (ver o registro em {@code SecurityConfig}), para que ate a
 * recusa da chave de servico leve o identificador.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

  /** O nome do cabecalho, nos dois sentidos; tambem usado pelo site e pelos testes. */
  public static final String HEADER = "X-Correlation-Id";

  /** A chave no MDC, que o padrao de log e o corpo do erro leem. */
  public static final String MDC_KEY = "correlationId";

  private static final Pattern FORMATO = Pattern.compile("[A-Za-z0-9-]{8,64}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    String recebido = request.getHeader(HEADER);
    String id =
        recebido != null && FORMATO.matcher(recebido).matches()
            ? recebido
            : UUID.randomUUID().toString();

    MDC.put(MDC_KEY, id);
    response.setHeader(HEADER, id);
    try {
      chain.doFilter(request, response);
    } finally {
      // A thread volta para o pool do servidor; sem a limpeza, a proxima requisicao que ela
      // atendesse herdaria o identificador desta.
      MDC.remove(MDC_KEY);
    }
  }
}
