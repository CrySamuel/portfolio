package dev.crystofer.portfolio.profile.domain.model;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Um agrupamento de competencias, sempre ordenado por dentro.
 *
 * <p>Como {@link Timeline} e como {@link Profile}, a ordem e verdade por construcao e nao promessa
 * que cada consulta precise lembrar de cumprir.
 *
 * <p><strong>Duas ordens diferentes convivem aqui, e a distincao e o ponto do commit.</strong> A
 * ordem das competencias <em>dentro</em> da categoria sai do proprio dado - nivel primeiro, nome
 * para desempatar -, entao e calculada. Ja a ordem <em>entre</em> categorias nao tem de onde ser
 * deduzida: que "Linguagens" venha antes de "Bancos de Dados" e escolha editorial, e por isso viaja
 * em {@code displayOrder}, guardado na coluna.
 *
 * @param name o cabecalho que a secao exibe
 * @param displayOrder posicao entre as categorias; o {@link SkillCatalog} ordena por ele
 * @param skills as competencias, da mais dominada para a menos
 */
public record SkillCategory(String name, int displayOrder, List<Skill> skills) {

  /** Espelha o limite da coluna em V3__create_skill_tables.sql. */
  private static final int MAX_NAME_LENGTH = 60;

  /**
   * Nivel decrescente, nome para desempatar.
   *
   * <p>O nivel primeiro porque a secao existe para o recrutador escanear: o que a pessoa domina
   * aparece no topo do grupo. O nome depois torna a ordem total - sem ele, duas competencias de
   * mesmo nivel sairiam em ordem indefinida e a pagina mudaria de aparencia entre dois deploys.
   *
   * <p>O nivel compara pela ordem de declaracao do enum, entao {@code ADVANCED} vem antes.
   */
  private static final Comparator<Skill> POR_NIVEL_E_NOME =
      Comparator.comparing(Skill::proficiency).reversed().thenComparing(Skill::name);

  public SkillCategory {
    name = requireText(name);
    skills = order(skills);
  }

  /** Se a categoria nao tem nenhuma competencia - cabecalho sozinho nao vai para a tela. */
  public boolean isEmpty() {
    return skills.isEmpty();
  }

  private static String requireText(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Nome da categoria e obrigatorio");
    }
    String trimmed = value.trim();
    if (trimmed.length() > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException(
          "Nome da categoria excede " + MAX_NAME_LENGTH + " caracteres: " + trimmed.length());
    }
    return trimmed;
  }

  /**
   * Copia defensiva, unicidade e ordenacao.
   *
   * <p>A unicidade repete a constraint {@code skill_category_id_name_uk} pela razao de sempre: o
   * dominio nao pode depender de o banco estar correto para estar correto.
   */
  private static List<Skill> order(List<Skill> skills) {
    if (skills == null) {
      throw new IllegalArgumentException("Lista de competencias e obrigatoria; use List.of()");
    }
    Set<String> vistos = new HashSet<>();
    for (Skill skill : skills) {
      if (!vistos.add(skill.name())) {
        throw new IllegalArgumentException("Competencia repetida na categoria: " + skill.name());
      }
    }
    return skills.stream().sorted(POR_NIVEL_E_NOME).toList();
  }
}
