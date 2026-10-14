package dev.crystofer.portfolio.shared.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Os idiomas em que o conteudo do portfolio existe (ADR-0013).
 *
 * <p><strong>Enum, e nao {@link java.util.Locale}.</strong> Um {@code Locale} aceita qualquer
 * idioma do mundo, e o conteudo existe em dois. Receber um tipo que admite {@code fr-FR} obrigaria
 * todo ponto do codigo a perguntar "e se vier outro?" - com o enum, a pergunta e respondida uma
 * vez, na borda HTTP, e daqui para dentro so circula idioma que existe.
 *
 * <p>Acrescentar um idioma e acrescentar uma constante aqui e ampliar o {@code CHECK} das tabelas
 * de traducao. As duas mudancas andam juntas, e nenhuma das duas acontece sem decisao.
 *
 * <p>Mora em {@code shared/domain}, como {@link Slug} e {@link EmailAddress}: e vocabulario comum a
 * todos os modulos de conteudo, e so depende de {@code java..}.
 */
public enum ContentLanguage {
  PT_BR("pt-BR"),
  EN_US("en-US");

  private final String tag;

  ContentLanguage(String tag) {
    this.tag = tag;
  }

  /**
   * O idioma do conteudo original, guardado nas tabelas de sempre. E o que se entrega quando nao ha
   * pedido, quando o pedido nao corresponde a nada, e quando falta traducao.
   */
  public static ContentLanguage padrao() {
    return PT_BR;
  }

  /** A tag BCP 47, que e o que o HTTP e o banco conhecem: {@code pt-BR}, {@code en-US}. */
  public String tag() {
    return tag;
  }

  /** O conteudo neste idioma esta nas tabelas originais, sem traducao a procurar. */
  public boolean isPadrao() {
    return this == padrao();
  }

  /** As tags de todos os idiomas, na ordem de declaracao - o padrao primeiro. */
  public static List<String> tags() {
    return Arrays.stream(values()).map(ContentLanguage::tag).toList();
  }

  /**
   * O idioma de uma tag exata, sem diferenciar maiusculas, ou vazio se nao for um dos previstos.
   */
  public static Optional<ContentLanguage> daTag(String tag) {
    return Arrays.stream(values()).filter(idioma -> idioma.tag.equalsIgnoreCase(tag)).findFirst();
  }
}
