package dev.crystofer.portfolio.shared.error;

/**
 * Recurso pedido que nao existe.
 *
 * <p>Sem nenhuma anotacao de framework - nem {@code @ResponseStatus}. A tentacao e marcar a excecao
 * com o status HTTP e deixar o Spring resolver, e o custo disso e que a camada de aplicacao, que a
 * lanca, passaria a declarar semantica de HTTP. Quem traduz excecao em resposta e o {@link
 * GlobalExceptionHandler}, num lugar so.
 *
 * <p>Consequencia pratica: esta excecao serve igualmente a um adaptador que nao seja web - um
 * agendador, uma CLI - sem arrastar significado de protocolo para onde ele nao vale.
 */
public class ResourceNotFoundException extends RuntimeException {

  private final String resource;

  public ResourceNotFoundException(String resource, String detail) {
    super(detail);
    this.resource = resource;
  }

  /** Nome do recurso ausente, usado para compor o {@code type} do Problem Details. */
  public String resource() {
    return resource;
  }
}
