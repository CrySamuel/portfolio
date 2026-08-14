package dev.crystofer.portfolio.projects.domain.model;

import java.util.Optional;

import dev.crystofer.portfolio.shared.domain.Slug;

/**
 * Uma tecnologia declarada por um projeto.
 *
 * <p>Record pelo mesmo motivo de {@code Experience}: e um valor lido, nunca alterado em memoria. O
 * conteudo entra por migracao (ADR-0004).
 *
 * <p>O nome e o slug sao campos separados, e nao um derivado do outro. O nome e exibido como esta
 * escrito - "Spring Boot", "PostgreSQL" -, porque marca escrita errada e ruido para quem avalia; o
 * slug vai para a query string do filtro, que precisa ser compartilhavel. Derivar o segundo do
 * primeiro e a geracao automatica que {@link Slug} recusa.
 *
 * @param name como a tecnologia se escreve, com a capitalizacao da marca
 * @param slug identificador usado no filtro da listagem
 * @param category familia a que ela pertence
 * @param iconSlug icone do sprite; {@code null} enquanto nao houver sprite proprio
 */
public record Technology(String name, Slug slug, TechnologyCategory category, String iconSlug) {

  // Espelham os limites das colunas em V4__create_project_tables.sql.
  private static final int MAX_NAME_LENGTH = 60;
  private static final int MAX_ICON_SLUG_LENGTH = 60;

  public Technology {
    name = requireText(name);
    if (slug == null) {
      throw new IllegalArgumentException("Slug da tecnologia e obrigatorio");
    }
    if (category == null) {
      throw new IllegalArgumentException("Categoria da tecnologia e obrigatoria");
    }
    iconSlug = normalizeIconSlug(iconSlug);
  }

  /**
   * Icone, quando houver.
   *
   * <p>Provavelmente nao havera tao cedo: a divida registrada no MVP 2 e que logos de linguagens e
   * frameworks sao marcas registradas, cada projeto com politica propria de uso, e desenhar
   * aproximacoes ficaria pior do que nao ter icone.
   */
  public Optional<String> findIconSlug() {
    return Optional.ofNullable(iconSlug);
  }

  private static String requireText(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Nome da tecnologia e obrigatorio");
    }
    String trimmed = value.trim();
    if (trimmed.length() > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException(
          "Nome da tecnologia excede " + MAX_NAME_LENGTH + " caracteres: " + trimmed.length());
    }
    return trimmed;
  }

  /**
   * Ausente e valido; presente e em branco, nao.
   *
   * <p>String vazia e nulo significariam a mesma coisa - "sem icone" - por dois caminhos, e o
   * mapeamento teria de tratar os dois em todo lugar que tocasse o campo. Basta um lugar esquecido
   * para a tela pedir um simbolo de nome vazio ao sprite. E a mesma razao do {@code DEFAULT '[]'}
   * em {@code experience.highlights}.
   */
  private static String normalizeIconSlug(String iconSlug) {
    if (iconSlug == null) {
      return null;
    }
    if (iconSlug.isBlank()) {
      throw new IllegalArgumentException("Icone em branco; use null quando nao houver");
    }
    String trimmed = iconSlug.trim();
    if (trimmed.length() > MAX_ICON_SLUG_LENGTH) {
      throw new IllegalArgumentException(
          "Icone excede " + MAX_ICON_SLUG_LENGTH + " caracteres: " + trimmed.length());
    }
    return trimmed;
  }
}
