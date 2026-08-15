package dev.crystofer.portfolio.github.domain.model;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Um repositorio publico, no recorte que a secao de estatisticas exibe.
 *
 * <p>Chama-se <em>summary</em> e nao <em>repository</em> porque e menos do que um repositorio: o
 * que cabe num card. O que o GitHub devolve tem dezenas de campos - permissoes, forks, licenca,
 * topicos, URLs de API - e trazer todos para o dominio significaria mante-los quando a API deles
 * mudar, sem que nada aqui os use.
 *
 * <p><strong>Descricao e linguagem principal podem faltar, e faltam de verdade.</strong>
 * Repositorio sem descricao e comum, e repositorio so com arquivos de configuracao nao tem
 * linguagem detectada. Exigir os dois faria o adaptador escolher entre inventar texto e descartar o
 * repositorio.
 *
 * @param name nome do repositorio, sem o dono
 * @param description descricao curta; {@code null} quando nao ha
 * @param url endereco publico, sempre em https
 * @param primaryLanguage linguagem predominante; {@code null} quando o GitHub nao detecta nenhuma
 * @param stars estrelas recebidas, nunca negativo
 * @param lastPushedAt data do ultimo push, que e o que distingue projeto vivo de arquivado
 */
public record RepositorySummary(
    String name,
    String description,
    String url,
    String primaryLanguage,
    int stars,
    LocalDate lastPushedAt) {

  /** Limite do proprio GitHub para nome de repositorio. */
  private static final int MAX_NAME_LENGTH = 100;

  private static final int MAX_DESCRIPTION_LENGTH = 350;

  private static final String REQUIRED_SCHEME = "https://";

  public RepositorySummary {
    name = requireText(name, "Nome do repositorio", MAX_NAME_LENGTH);
    description = optionalText(description, "Descricao do repositorio", MAX_DESCRIPTION_LENGTH);
    url = requireText(url, "Endereco do repositorio", Integer.MAX_VALUE);
    if (!url.startsWith(REQUIRED_SCHEME)) {
      throw new IllegalArgumentException(
          "Endereco do repositorio precisa comecar com " + REQUIRED_SCHEME);
    }
    primaryLanguage = optionalText(primaryLanguage, "Linguagem principal", 60);
    if (stars < 0) {
      throw new IllegalArgumentException("Estrelas nao podem ser negativas: " + stars);
    }
    // A data e exigida porque e por ela que os repositorios sao desempatados, e
    // uma ordenacao que dependa de campo opcional passa a ter dois
    // comportamentos. A API do GitHub sempre a devolve - repositorio criado e
    // nunca empurrado traz a data de criacao. Se um dia vier nula, o adaptador
    // falha e o fallback do ADR-0008 entrega o cache: e o comportamento certo
    // para resposta malformada, e ele ja tem teste previsto no commit 43.
    if (lastPushedAt == null) {
      throw new IllegalArgumentException("Data do ultimo push e obrigatoria");
    }
  }

  /** Descricao, quando ha - o card decide entre exibir a linha ou fecha-la. */
  public Optional<String> findDescription() {
    return Optional.ofNullable(description);
  }

  /** Linguagem principal, quando o GitHub detecta alguma. */
  public Optional<String> findPrimaryLanguage() {
    return Optional.ofNullable(primaryLanguage);
  }

  private static String requireText(String value, String field, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " e obrigatorio");
    }
    String trimmed = value.trim();
    if (trimmed.length() > maxLength) {
      throw new IllegalArgumentException(
          field + " excede " + maxLength + " caracteres: " + trimmed.length());
    }
    return trimmed;
  }

  /**
   * Ausente e valido; em branco nao.
   *
   * <p>A distincao repete a de {@code Project}: {@code null} diz "nao ha", e uma string vazia diz a
   * mesma coisa por um segundo caminho. Com dois jeitos de representar ausencia, metade do codigo
   * confere um e metade confere o outro.
   */
  private static String optionalText(String value, String field, int maxLength) {
    if (value == null) {
      return null;
    }
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " em branco; use null quando nao houver");
    }
    String trimmed = value.trim();
    if (trimmed.length() > maxLength) {
      throw new IllegalArgumentException(
          field + " excede " + maxLength + " caracteres: " + trimmed.length());
    }
    return trimmed;
  }
}
