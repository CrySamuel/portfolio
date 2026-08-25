package dev.crystofer.portfolio.shared.error;

/**
 * O remetente excedeu o numero de mensagens permitido na janela.
 *
 * <p>Carrega os segundos que faltam para a proxima ficha porque o {@code Retry-After} da resposta
 * precisa deles - e quem sabe o numero e o limitador, la na borda, nao o tratador de erro.
 */
public class RateLimitExceededException extends RuntimeException {

  private final long retryAfterSeconds;

  public RateLimitExceededException(long retryAfterSeconds) {
    super("Limite de mensagens excedido. Tente novamente em " + retryAfterSeconds + "s.");
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public long retryAfterSeconds() {
    return retryAfterSeconds;
  }
}
