package dev.crystofer.portfolio.github.domain.model;

/**
 * Quanto codigo de uma linguagem existe nos repositorios publicos.
 *
 * <p><strong>Guarda bytes, e nao porcentagem.</strong> Porcentagem so existe em relacao a um
 * conjunto: tres linguagens com 90% cada e um estado impossivel que um registro isolado nao tem
 * como recusar. Com bytes, cada linha e verdadeira sozinha e a fatia e calculada por {@link
 * GitHubStats}, que e quem conhece o total. E a mesma razao pela qual {@code ProjectMetric} guarda
 * texto - a representacao honesta e a que nao permite escrever o impossivel.
 *
 * <p>O numero vem de {@code GET /repos/{owner}/{repo}/languages}, que devolve bytes por linguagem,
 * e atravessa o dominio sem conversao. Transformar em porcentagem no adaptador jogaria fora a
 * informacao que permite somar repositorios.
 *
 * @param name nome como o GitHub o escreve, com a capitalizacao da marca
 * @param bytes bytes de codigo naquela linguagem, sempre positivo
 */
public record LanguageUsage(String name, long bytes) {

  /** O maior nome de linguagem que o GitHub reconhece hoje tem menos que isto, com folga. */
  private static final int MAX_NAME_LENGTH = 60;

  public LanguageUsage {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Nome da linguagem e obrigatorio");
    }
    name = name.trim();
    if (name.length() > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException(
          "Nome da linguagem excede " + MAX_NAME_LENGTH + " caracteres: " + name.length());
    }
    // Zero bytes nao e "pouco uso", e ausencia: o GitHub simplesmente nao lista a
    // linguagem nesse caso. Aceitar zero deixaria entrar uma fatia que nunca
    // aparece no grafico e que ainda assim ocupa uma legenda.
    if (bytes <= 0) {
      throw new IllegalArgumentException("Bytes da linguagem precisam ser positivos: " + bytes);
    }
  }

  /**
   * A fatia desta linguagem num total, em porcentagem.
   *
   * <p>Devolve o valor exato, sem arredondar. Arredondamento e decisao de apresentacao - e uma que
   * nao fecha em 100% sozinha, entao quem desenha o grafico precisa escolher onde absorver a
   * diferenca. O dominio nao deve escolher por ele.
   *
   * @param totalBytes soma dos bytes de todas as linguagens; zero devolve zero em vez de dividir
   */
  public double shareOf(long totalBytes) {
    if (totalBytes <= 0) {
      return 0;
    }
    return bytes * 100.0 / totalBytes;
  }
}
