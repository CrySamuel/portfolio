package dev.crystofer.portfolio.shared.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Endereco de e-mail valido.
 *
 * <p>Existe para que o compilador diferencie um e-mail de qualquer outra String (secao 13.5). Um
 * metodo que recebe {@code EmailAddress} nao aceita por engano um nome, uma URL ou o proximo campo
 * do formulario - trocar dois parametros String de lugar e um bug que compila.
 *
 * <p>A validacao acontece na construcao e nao tem como ser pulada: se a instancia existe, o valor e
 * estruturalmente valido. E o que dispensa revalidar em cada camada.
 */
public record EmailAddress(String value) {

  /** Limite do RFC 5321 para o caminho de retorno. */
  private static final int MAX_LENGTH = 254;

  /**
   * Validacao estrutural, deliberadamente pragmatica: um trecho local sem espaco nem arroba, uma
   * arroba, um dominio com pelo menos um ponto e um TLD de duas letras ou mais.
   *
   * <p>Nao e o RFC 5322 completo, e nao deveria ser. A gramatica real aceita coisas que nenhum
   * provedor entrega (comentarios entre parenteses, aspas no trecho local) e a expressao que a
   * cobre e famosa por ser ilegivel. A unica prova definitiva de que um e-mail existe e enviar uma
   * mensagem para ele - o que este projeto faz no fluxo de contato. Aqui o objetivo e barrar o erro
   * de digitacao e o lixo obvio.
   */
  private static final Pattern STRUCTURE =
      Pattern.compile("^[^\\s@]+@[^\\s@.]+(\\.[^\\s@.]+)*\\.[A-Za-z]{2,}$");

  public EmailAddress {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("E-mail nao pode ser vazio");
    }

    // Normaliza antes de validar e de guardar. Dois cadastros do mesmo endereco
    // escrito com caixas diferentes precisam ser o mesmo valor - inclusive para
    // o equals do record, que compara a String.
    //
    // Locale.ROOT e obrigatorio: em turco, "I".toLowerCase() vira "i sem ponto"
    // e o endereco muda. O bug so aparece na maquina de quem tem esse locale.
    value = value.trim().toLowerCase(Locale.ROOT);

    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException(
          "E-mail excede " + MAX_LENGTH + " caracteres: " + value.length());
    }
    if (!STRUCTURE.matcher(value).matches()) {
      throw new IllegalArgumentException("E-mail com formato invalido");
    }
  }

  /** Fabrica nomeada, para leitura: {@code EmailAddress.of(texto)}. */
  public static EmailAddress of(String value) {
    return new EmailAddress(value);
  }

  /**
   * Forma segura para log: {@code c***@gmail.com}.
   *
   * <p>A secao 2.4 exige que o e-mail nunca seja registrado em claro. Manter o dominio permite
   * diagnosticar problema de entregabilidade sem guardar o endereco de ninguem.
   */
  public String masked() {
    int arroba = value.indexOf('@');
    return value.charAt(0) + "***" + value.substring(arroba);
  }

  /**
   * Mascarado de proposito, contrariando o padrao de record.
   *
   * <p>O {@code toString} gerado por um record imprime os componentes, entao {@code
   * log.info("...{}", email)} vazaria o endereco em claro - e vazaria em silencio, porque ninguem
   * escreve essa linha achando que esta registrando PII. Invertendo o padrao, o caminho preguicoso
   * vira o caminho seguro, e quem realmente precisa do valor tem de pedir por {@link #value()}, que
   * e explicito o bastante para aparecer numa revisao.
   */
  @Override
  public String toString() {
    return masked();
  }
}
