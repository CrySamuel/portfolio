package dev.crystofer.portfolio.github.domain.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * O quanto uma linguagem aparece nos repositorios publicos.
 *
 * <p><strong>O numero e peso, e nao bytes - e a diferenca foi medida.</strong> A primeira versao
 * somava os bytes de todos os repositorios, que e o que o GitHub oferece pronto. O resultado real
 * do perfil: <em>93,7% Python contra 3,2% Java</em>, porque um unico repositorio de estudo com
 * dependencias versionadas dentro dele carregava 17,8 MB - <strong>94,8% de todo o codigo
 * somado</strong>. O grafico media a biblioteca de terceiro que estava dentro daquele repositorio.
 *
 * <p>Com peso, <strong>cada repositorio distribui a mesma quantidade</strong> entre as linguagens
 * dele, proporcional aos bytes <em>daquele</em> repositorio. O mesmo perfil passa a 46% Python e
 * 36% Java, e o grafico responde "em que linguagens essa pessoa trabalha" em vez de "onde estao os
 * bytes". Repositorio grande deixa de decidir sozinho, sem lista de excecao para manter.
 *
 * <p>Continua sendo um inteiro somavel, e nao uma porcentagem guardada. Porcentagem so existe em
 * relacao a um conjunto: tres linguagens com 90% cada e um estado impossivel que um registro
 * isolado nao teria como recusar. A fatia sai de {@link #shareOf(long)} contra o total do agregado.
 *
 * @param name nome como o GitHub o escreve, com a capitalizacao da marca
 * @param weight peso acumulado, sempre positivo
 */
public record LanguageUsage(String name, long weight) {

  /** O maior nome de linguagem que o GitHub reconhece hoje tem menos que isto, com folga. */
  private static final int MAX_NAME_LENGTH = 60;

  /**
   * Quanto cada repositorio tem para distribuir.
   *
   * <p>E o que torna "cada repositorio pesa igual" uma conta de inteiros: um milhao por
   * repositorio, repartido na proporcao dos bytes dele. A escala e grande o bastante para que uma
   * linguagem com 0,1% de um repositorio ainda sobreviva ao arredondamento.
   */
  private static final long PESO_POR_REPOSITORIO = 1_000_000L;

  public LanguageUsage {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Nome da linguagem e obrigatorio");
    }
    name = name.trim();
    if (name.length() > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException(
          "Nome da linguagem excede " + MAX_NAME_LENGTH + " caracteres: " + name.length());
    }
    // Peso zero nao e pouco uso, e ausencia: o GitHub nao lista a linguagem
    // nesse caso. Aceitar zero deixaria entrar uma fatia que nunca aparece no
    // grafico e que ainda assim ocupa uma legenda.
    if (weight <= 0) {
      throw new IllegalArgumentException("Peso da linguagem precisa ser positivo: " + weight);
    }
  }

  /**
   * A mistura de linguagens do perfil, com cada repositorio pesando igual.
   *
   * <p>Vive no dominio, e nao no adaptador, porque "todo projeto conta o mesmo" e uma regra sobre
   * como o portfolio se descreve - nao um detalhe de como o GitHub entrega o dado. Somar no
   * adaptador tornaria a regra invisivel para quem le o modelo, e impossivel de testar sem HTTP.
   *
   * <p><strong>Repositorio sem linguagem nenhuma nao entra na media.</strong> E o caso real do
   * repositorio de perfil - aquele cujo nome e igual ao do usuario, que so tem um README: o GitHub
   * nao conta Markdown como linguagem, entao ele soma zero bytes. Divide-lo seria dividir por zero;
   * conta-lo como repositorio vazio diluiria todas as fatias por nada.
   *
   * @param bytesPorRepositorio um mapa de linguagem para bytes por repositorio, como a API devolve
   */
  public static List<LanguageUsage> averagingByRepository(
      List<Map<String, Long>> bytesPorRepositorio) {
    if (bytesPorRepositorio == null) {
      throw new IllegalArgumentException("Lista de repositorios e obrigatoria; use List.of()");
    }

    Map<String, Long> acumulado = new LinkedHashMap<>();

    for (Map<String, Long> repositorio : bytesPorRepositorio) {
      long total = somar(repositorio);
      if (total <= 0) {
        continue;
      }
      repositorio.forEach(
          (linguagem, bytes) -> {
            if (bytes != null && bytes > 0) {
              acumulado.merge(
                  linguagem, Math.round(PESO_POR_REPOSITORIO * (double) bytes / total), Long::sum);
            }
          });
    }

    // Zero acontece quando a linguagem e residual demais para sobreviver ao
    // arredondamento em qualquer repositorio. Descartar e o certo: o dominio
    // recusa peso zero, e uma fatia invisivel so ocuparia legenda.
    return acumulado.entrySet().stream()
        .filter(entrada -> entrada.getValue() > 0)
        .map(entrada -> new LanguageUsage(entrada.getKey(), entrada.getValue()))
        .toList();
  }

  /**
   * A fatia desta linguagem num total, em porcentagem.
   *
   * <p>Devolve o valor exato, sem arredondar. Arredondamento e decisao de apresentacao - e uma que
   * nao fecha em 100% sozinha, entao quem desenha o grafico precisa escolher onde absorver a
   * diferenca. O dominio nao deve escolher por ele.
   *
   * @param totalWeight soma dos pesos de todas as linguagens; zero devolve zero em vez de dividir
   */
  public double shareOf(long totalWeight) {
    if (totalWeight <= 0) {
      return 0;
    }
    return weight * 100.0 / totalWeight;
  }

  private static long somar(Map<String, Long> repositorio) {
    return repositorio.values().stream()
        .filter(bytes -> bytes != null && bytes > 0)
        .mapToLong(Long::longValue)
        .sum();
  }
}
