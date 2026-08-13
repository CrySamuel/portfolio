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
 * Estado conhecido das tabelas de competencias para os testes de integracao.
 *
 * <p>Existe pela mesma razao de {@link ExperienceFixtures}, e pelo mesmo gatilho: enquanto as
 * tabelas nasciam vazias no container, bastava limpar o que cada teste sujava. Com o {@code
 * R__seed_skills} aplicado pelo Flyway, elas ja chegam com as quatro categorias reais - e qualquer
 * teste que conte linhas passa a depender de um estado que nao controla.
 *
 * <p>As assercoes dos testes sao sobre dados que eles proprios escrevem. Afirmar sobre o conteudo
 * publicado transformaria "o dono ajustou o nivel de uma competencia" em build vermelho.
 */
public final class SkillFixtures {

  private static final String SEED_SCRIPT = "db/migration/R__seed_skills.sql";

  private SkillFixtures() {}

  /**
   * Apaga so {@code skill_category}: as competencias vao junto pelo {@code ON DELETE CASCADE}.
   *
   * <p>E mais que limpeza. Se o cascade sumir numa migracao futura, sobra linha orfa e a contagem
   * dos testes denuncia.
   */
  public static void empty(JdbcTemplate jdbcTemplate) {
    jdbcTemplate.update("DELETE FROM skill_category");
  }

  /**
   * Reaplica o seed de producao, o mesmo arquivo que o Flyway executa.
   *
   * <p>O {@code EncodedResource} com UTF-8 explicito repete a escolha do {@code flyway.encoding}:
   * em Windows o charset padrao da JVM nao e UTF-8, e ler o arquivo como CP1252 gravaria mojibake
   * sem erro nenhum - e este seed tem acentuacao em nome de categoria.
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
}
