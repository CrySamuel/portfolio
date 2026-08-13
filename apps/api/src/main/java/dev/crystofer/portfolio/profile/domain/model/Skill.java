package dev.crystofer.portfolio.profile.domain.model;

import java.util.Optional;

/**
 * Uma competencia tecnica.
 *
 * <p>Record pelo mesmo motivo dos demais modelos: valor lido, nunca alterado em memoria.
 *
 * @param name como a competencia se chama
 * @param proficiency o nivel, sempre do enum e nunca texto solto
 * @param yearsOfExperience anos declarados; {@code null} quando nao ha numero honesto a declarar
 */
public record Skill(String name, Proficiency proficiency, Integer yearsOfExperience) {

  /** Espelha o limite da coluna em V3__create_skill_tables.sql. */
  private static final int MAX_NAME_LENGTH = 60;

  public Skill {
    name = requireText(name);
    if (proficiency == null) {
      throw new IllegalArgumentException("Nivel de proficiencia e obrigatorio para " + name);
    }
    requireYears(yearsOfExperience, name);
  }

  /**
   * Anos de experiencia, quando declarados.
   *
   * <p>{@code Optional} na leitura e {@code Integer} no componente: a ausencia e um estado real -
   * ninguem sabe ha quantos anos usa Git - e quem consome precisa ser obrigado a tratar isso.
   */
  public Optional<Integer> findYearsOfExperience() {
    return Optional.ofNullable(yearsOfExperience);
  }

  private static String requireText(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Nome da competencia e obrigatorio");
    }
    String trimmed = value.trim();
    if (trimmed.length() > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException(
          "Nome da competencia excede " + MAX_NAME_LENGTH + " caracteres: " + trimmed.length());
    }
    return trimmed;
  }

  /** O mesmo limite do {@code skill_years_ck}: zero vale, negativo nao existe. */
  private static void requireYears(Integer years, String name) {
    if (years != null && years < 0) {
      throw new IllegalArgumentException("Anos de experiencia negativos em " + name + ": " + years);
    }
  }
}
