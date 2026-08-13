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
 * Estado conhecido da tabela {@code experience} para os testes de integracao.
 *
 * <p><strong>Existe a partir do momento em que a timeline passou a ter conteudo.</strong> Ate o
 * seed entrar, a tabela nascia vazia no container e os testes podiam apenas limpar o que sujavam.
 * Com o {@code R__seed_experience} aplicado pelo Flyway, o container ja comeca com as duas posicoes
 * reais - e qualquer teste que conte linhas ou espere tabela vazia passa a depender de um estado
 * que nao controla.
 *
 * <p>Dai o par: {@link #empty} no inicio de cada teste, {@link #reapplySeed} no fim. O teste
 * escreve o proprio cenario e devolve o banco ao padrao, sem depender da ordem de execucao - a
 * licao que ja custou um CI vermelho neste projeto.
 *
 * <p>Nao ha constantes de conteudo aqui, ao contrario de {@link ProfileFixtures}, e a diferenca e
 * proposital: as assercoes sobre a timeline sao sobre dados que o proprio teste escreve. Afirmar
 * sobre o conteudo publicado transformaria "o dono corrigiu a descricao de um cargo" em build
 * vermelho.
 */
public final class ExperienceFixtures {

  private static final String SEED_SCRIPT = "db/migration/R__seed_experience.sql";

  private ExperienceFixtures() {}

  /** Timeline sem nenhuma passagem - o cenario da secao Sobre vazia. */
  public static void empty(JdbcTemplate jdbcTemplate) {
    jdbcTemplate.update("DELETE FROM experience");
  }

  /**
   * Reaplica o seed de producao, o mesmo arquivo que o Flyway executa.
   *
   * <p>Serve a dois propositos: devolver o banco ao estado padrao depois de cada teste, e permitir
   * afirmar sobre a idempotencia que o {@code R__} promete no proprio cabecalho.
   *
   * <p>O {@code EncodedResource} com UTF-8 explicito repete a escolha do {@code flyway.encoding}:
   * em Windows o charset padrao da JVM nao e UTF-8, e ler o arquivo como CP1252 gravaria mojibake
   * sem erro nenhum.
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
