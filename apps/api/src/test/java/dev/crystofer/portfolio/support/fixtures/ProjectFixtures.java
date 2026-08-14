package dev.crystofer.portfolio.support.fixtures;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Estado conhecido das tabelas de projeto para os testes de integracao.
 *
 * <p>Diferente de {@link ProfileFixtures}, {@link ExperienceFixtures} e {@link SkillFixtures}, aqui
 * nao ha seed de producao a restaurar: o conteudo dos projetos chega num commit proprio, depois do
 * endpoint. As tabelas nascem vazias no container, entao basta devolve-las vazias.
 *
 * <p>O que este arquivo escreve e cenario de teste, e nao conteudo publicado. Afirmar sobre dado
 * real transformaria "o dono ajustou um texto" em build vermelho.
 */
public final class ProjectFixtures {

  private static final String INSERIR_PROJETO =
      """
      INSERT INTO project (slug, title, summary, problem, solution, outcome,
                           repo_url, live_url, featured, display_order, published_at)
      VALUES (?, ?, 'Resumo do card.', 'O problema.', 'A solucao.', 'O resultado.',
              'https://github.com/exemplo/repo', NULL, ?, ?, DATE '2026-03-24')
      RETURNING id
      """;

  private ProjectFixtures() {}

  /**
   * Apaga so {@code project} e {@code technology}.
   *
   * <p>Os vinculos e as metricas vao junto pelo {@code ON DELETE CASCADE}, e a dependencia e
   * deliberada - se o cascade sumir numa migracao futura, a limpeza falha e a suite denuncia. E a
   * mesma escolha de {@link SkillFixtures}, com a consequencia ja medida: quebrar o cascade derruba
   * a classe inteira em vez de reprovar um teste.
   *
   * <p>A ordem importa. {@code project} primeiro, porque o {@code RESTRICT} do vinculo recusaria
   * apagar uma tecnologia ainda referenciada.
   */
  public static void empty(JdbcTemplate jdbcTemplate) {
    jdbcTemplate.update("DELETE FROM project");
    jdbcTemplate.update("DELETE FROM technology");
  }

  /**
   * Um projeto com as tecnologias e as metricas informadas.
   *
   * <p>As tecnologias sao criadas com nome e slug derivados do indice, e nao de um catalogo fixo,
   * para que o mesmo metodo sirva a cenarios com quantidades diferentes sem colidir nas chaves
   * unicas.
   *
   * @return o id gerado do projeto
   */
  public static long insertProject(
      JdbcTemplate jdbcTemplate,
      String slug,
      String title,
      boolean featured,
      int displayOrder,
      int technologies,
      int metrics) {
    Long projectId =
        jdbcTemplate.queryForObject(
            INSERIR_PROJETO, Long.class, slug, title, featured, (short) displayOrder);

    for (int i = 0; i < technologies; i++) {
      String techSlug = slug + "-tech-" + i;
      Long technologyId =
          jdbcTemplate.queryForObject(
              """
              INSERT INTO technology (name, slug, category) VALUES (?, ?, 'language') RETURNING id
              """,
              Long.class,
              "Tech " + slug + " " + i,
              techSlug);
      jdbcTemplate.update(
          "INSERT INTO project_tech (project_id, technology_id) VALUES (?, ?)",
          projectId,
          technologyId);
    }

    for (int i = 0; i < metrics; i++) {
      jdbcTemplate.update(
          "INSERT INTO project_metric (project_id, label, value, display_order) VALUES (?, ?, ?, ?)",
          projectId,
          "Metrica " + i,
          i + "0ms",
          (short) i);
    }

    return projectId;
  }
}
