package dev.crystofer.portfolio.shared.domain;

import java.util.regex.Pattern;

/**
 * Identificador legivel que aparece na URL publica.
 *
 * <p>Existe pela mesma razao de {@link EmailAddress}: para que o compilador diferencie um slug de
 * qualquer outra String (secao 13.5). Num record de projeto com titulo, resumo e slug lado a lado,
 * trocar dois parametros de lugar e um bug que compila - e o sintoma seria a URL virar o titulo.
 *
 * <p>A validacao acontece na construcao e nao tem como ser pulada: se a instancia existe, o valor
 * cabe numa URL. E o mesmo formato do {@code project_slug_format_ck} e do {@code
 * technology_slug_format_ck} na {@code V4__create_project_tables}, e a duplicacao e deliberada - o
 * conteudo entra por migracao (ADR-0004), sem passar por aqui, entao nenhuma das duas guardas
 * dispensa a outra.
 *
 * <p><strong>Nao ha fabrica que gere slug a partir de um titulo</strong>, e a ausencia e uma
 * recusa. A secao 3.8 trata a URL como contrato publico, o que significa que ela e escolhida e
 * depois nao muda; derivar de titulo faz o contrato acompanhar a edicao do texto em silencio. E
 * geracao ingenua produz exatamente os valores que o formato recusa - {@code music--style} de um
 * titulo com dois separadores seguidos, {@code music-} de um que termina em pontuacao.
 */
public record Slug(String value) {

  /** Espelha o limite de {@code project.slug} em V4__create_project_tables.sql. */
  private static final int MAX_LENGTH = 80;

  /**
   * Minusculas, digitos e hifen como separador interno.
   *
   * <p>Sem maiuscula porque URL e comparada byte a byte por proxy e cache, e {@code /FinAI} e
   * {@code /finai} seriam dois enderecos para a mesma pagina - duas entradas de CDN e duas linhas
   * no relatorio de acesso. Sem acentuacao e sem espaco porque os dois viram escape percentual, que
   * e ilegivel no lugar onde o slug existe para ser lido.
   *
   * <p>O grupo repetido, e nao {@code [a-z0-9-]+}, e o que impede hifen no inicio, no fim e
   * duplicado. Sao os tres casos que geracao automatica produz.
   */
  private static final Pattern FORMAT = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

  public Slug {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Slug e obrigatorio");
    }

    // Trim, e nao normalizacao. Espaco nas pontas e erro de digitacao no seed e
    // nao muda o valor pretendido; ja trocar espaco interno por hifen mudaria, e
    // e a geracao automatica que o javadoc da classe recusa.
    value = value.trim();

    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException(
          "Slug excede " + MAX_LENGTH + " caracteres: " + value.length());
    }
    if (!FORMAT.matcher(value).matches()) {
      throw new IllegalArgumentException(
          "Slug fora do formato (minusculas, digitos e hifen entre eles): " + value);
    }
  }

  /** Fabrica nomeada, para leitura: {@code Slug.of(texto)}. */
  public static Slug of(String value) {
    return new Slug(value);
  }

  /**
   * O proprio valor, contrariando o padrao de record - e pelo motivo oposto ao de {@link
   * EmailAddress}.
   *
   * <p>La o {@code toString} gerado vazaria PII, entao foi mascarado para tornar seguro o caminho
   * preguicoso. Aqui ele produziria {@code Slug[value=finai]}, e a linha preguicosa e {@code
   * "/projetos/" + slug} - que compila, monta uma URL quebrada e so aparece quando alguem clica. A
   * escolha e a mesma nas duas classes: fazer o caminho descuidado dar o resultado certo.
   */
  @Override
  public String toString() {
    return value;
  }
}
