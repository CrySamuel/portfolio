package dev.crystofer.portfolio.profile.domain.model;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * As competencias agrupadas, na ordem em que a secao as exibe.
 *
 * <p>Existe pela mesma razao que {@link Timeline}: para que a ordem seja verdade por construcao. A
 * secao 16 do plano lista tres tipos para este commit e nao menciona este quarto, mas o titulo do
 * commit 31 e "grouped by category" e a F05 diz que <em>agrupamento e regra de negocio, nao
 * formatacao</em> - e regra de negocio mora no dominio, com um tipo que a carregue.
 *
 * <p>A alternativa seria devolver {@code List<SkillCategory>} e ordenar no servico. Funciona
 * enquanto houver um chamador so; o modo de falhar e que o segundo chamador esquece, e ninguem
 * percebe porque a lista continua vindo cheia.
 *
 * <p><strong>Categoria vazia nao entra.</strong> Um cabecalho sem competencia abaixo e ruido na
 * tela e um item a mais para o leitor de tela anunciar sem conteudo. Filtrar aqui, e nao no
 * componente, mantem a decisao num lugar so.
 *
 * @param categories categorias nao vazias, em ordem de exibicao
 */
public record SkillCatalog(List<SkillCategory> categories) {

  /**
   * Ordem editorial, com o nome desempatando.
   *
   * <p>O {@code displayOrder} vem da coluna porque nao ha nada no dado de onde deduzi-lo. O nome
   * fecha a ordem: duas categorias com o mesmo numero sairiam em ordem indefinida, e o seed nao
   * impede numeros repetidos.
   */
  private static final Comparator<SkillCategory> POR_ORDEM_EDITORIAL =
      Comparator.comparingInt(SkillCategory::displayOrder).thenComparing(SkillCategory::name);

  public SkillCatalog {
    categories = order(categories);
  }

  /** Catalogo sem nenhuma categoria - estado legitimo enquanto nao houver seed. */
  public static SkillCatalog empty() {
    return new SkillCatalog(List.of());
  }

  public boolean isEmpty() {
    return categories.isEmpty();
  }

  /** Quantas competencias ha ao todo, somando as categorias. */
  public int totalSkills() {
    return categories.stream().mapToInt(category -> category.skills().size()).sum();
  }

  private static List<SkillCategory> order(List<SkillCategory> categories) {
    if (categories == null) {
      throw new IllegalArgumentException(
          "Lista de categorias e obrigatoria; use SkillCatalog.empty() se vazia");
    }
    Set<String> vistos = new HashSet<>();
    for (SkillCategory category : categories) {
      if (!vistos.add(category.name())) {
        throw new IllegalArgumentException("Categoria repetida: " + category.name());
      }
    }
    return categories.stream()
        .filter(category -> !category.isEmpty())
        .sorted(POR_ORDEM_EDITORIAL)
        .toList();
  }
}
