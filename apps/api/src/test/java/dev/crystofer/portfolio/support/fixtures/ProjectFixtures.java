package dev.crystofer.portfolio.support.fixtures;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Estado conhecido das tabelas de projeto para os testes de integracao.
 *
 * <p>O gatilho e o mesmo dos outros tres: enquanto as tabelas nasciam vazias no container, bastava
 * limpar o que cada teste sujava. Com o {@code R__seed_projects} aplicado pelo Flyway, elas ja
 * chegam com os dois projetos reais - e qualquer teste que conte linhas passa a depender de um
 * estado que nao controla.
 *
 * <p>O que este arquivo escreve e cenario de teste, e nao conteudo publicado. Afirmar sobre dado
 * real transformaria "o dono ajustou um texto" em build vermelho.
 */
public final class ProjectFixtures {

  private static final String SEED_SCRIPT = "db/migration/R__seed_projects.sql";

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
   * Reaplica o seed de producao, o mesmo arquivo que o Flyway executa.
   *
   * <p>O {@code EncodedResource} com UTF-8 explicito repete a escolha do {@code flyway.encoding}:
   * em Windows o charset padrao da JVM nao e UTF-8, e ler o arquivo como CP1252 gravaria mojibake
   * sem erro nenhum - e este seed tem acentuacao em toda a narrativa.
   */
  public static void reapplySeed(DataSource dataSource) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      ScriptUtils.executeSqlScript(
          connection,
          new EncodedResource(new ClassPathResource(SEED_SCRIPT), StandardCharsets.UTF_8));
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

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
