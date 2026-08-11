package dev.crystofer.portfolio.shared.error;

import java.net.URI;

/**
 * O {@code type} das respostas de erro, montado num lugar so.
 *
 * <p>A URI e relativa por enquanto. O RFC 9457 aceita referencia relativa, e o dominio deste
 * portfolio so passa a existir no commit 23 - fixar {@code https://portfolio.dev/...} antes disso
 * seria publicar um endereco que nao controlamos, e troca-lo depois quebraria quem ja consumisse.
 *
 * <p>Esta classe existe justamente porque essa troca vai acontecer. O prefixo era constante privada
 * do {@link GlobalExceptionHandler}; quando o filtro de chave de servico passou a responder erro
 * tambem - e ele nao passa pelo tratador, porque roda antes do Spring MVC -, uma segunda copia
 * apareceria. Duas copias de um valor que sabidamente vai mudar divergem no dia da mudanca.
 */
public final class ProblemTypes {

  private static final String PREFIXO = "/errors/";

  private ProblemTypes() {}

  /** {@code de("resource-not-found")} devolve {@code /errors/resource-not-found}. */
  public static URI de(String slug) {
    return URI.create(PREFIXO + slug);
  }
}
