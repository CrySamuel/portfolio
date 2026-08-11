package dev.crystofer.portfolio.shared.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;

import dev.crystofer.portfolio.shared.error.ProblemTypes;
import tools.jackson.databind.ObjectMapper;

/**
 * Exige a chave compartilhada entre o BFF e a API (secao 2.4, ADR-0005).
 *
 * <p>A API fica numa URL publica da internet, e o navegador nunca a chama - quem chama e o servidor
 * do Next, que ja tem onde guardar um segredo. Sem esta verificacao, qualquer pessoa que
 * descobrisse o endereco do Render leria os endpoints, e mais tarde escreveria neles pelo
 * formulario de contato.
 *
 * <p><strong>Nao e autenticacao de usuario.</strong> Nao ha usuarios neste sistema. E autenticacao
 * de <em>chamador</em>: prova que a requisicao veio do nosso BFF, e nao de um script qualquer. Por
 * isso um filtro de 60 linhas resolve, e trazer Spring Security para isto seria arrastar cadeia de
 * filtros, contexto de seguranca e configuracao para um {@code if}.
 *
 * <p>A comparacao usa {@link MessageDigest#isEqual} e nao {@code equals}. Comparacao de String sai
 * no primeiro caractere diferente, e o tempo de resposta passa a contar quantos caracteres iniciais
 * estao certos - com requisicoes suficientes, a chave se descobre um caractere por vez. O custo de
 * evitar isso e uma chamada de metodo.
 */
public class ServiceKeyAuthFilter extends OncePerRequestFilter {

  /** O nome do cabecalho, tambem usado pelo cliente TS e pelos testes. */
  public static final String HEADER = "X-Service-Key";

  private final byte[] chaveEsperada;
  private final ObjectMapper objectMapper;

  public ServiceKeyAuthFilter(String chaveEsperada, ObjectMapper objectMapper) {
    this.chaveEsperada = chaveEsperada.getBytes(StandardCharsets.UTF_8);
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, jakarta.servlet.ServletException {

    if (chaveConfere(request.getHeader(HEADER))) {
      chain.doFilter(request, response);
      return;
    }

    recusar(response);
  }

  private boolean chaveConfere(String apresentada) {
    if (apresentada == null) return false;
    return MessageDigest.isEqual(chaveEsperada, apresentada.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * A mesma forma de erro do resto da API, montada a mao.
   *
   * <p>O filtro roda antes do Spring MVC, entao o {@code GlobalExceptionHandler} nao alcanca esta
   * resposta - lancar excecao aqui produziria a pagina de erro branca do container. Escrever o
   * {@code ProblemDetail} direto mantem {@code application/problem+json} em toda resposta de erro
   * da API, que e o que permite ao cliente ter um unico ramo de tratamento.
   *
   * <p>O corpo nao distingue "sem cabecalho" de "chave errada". A diferenca so seria util para quem
   * esta tentando adivinhar.
   */
  private void recusar(HttpServletResponse response) throws IOException {
    var problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED, "Chave de servico ausente ou invalida");
    problem.setTitle("Nao autorizado");
    problem.setType(ProblemTypes.de("unauthorized"));

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
