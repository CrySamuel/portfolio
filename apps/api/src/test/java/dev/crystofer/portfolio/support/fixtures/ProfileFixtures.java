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
 * Estado de banco conhecido para os testes de integracao.
 *
 * <p><strong>Por que o teste nao usa o conteudo real.</strong> O container aplica {@code
 * R__seed_profile.sql}, entao o perfil publicado do portfolio esta la. Afirmar sobre ele
 * transformaria "o dono corrigiu a propria bio" em build vermelho - e uma suite que reprova por
 * mudanca de texto e uma suite que as pessoas aprendem a ignorar. As afirmacoes deste pacote sao
 * sobre dados que o proprio teste escreveu.
 *
 * <p>Tudo em SQL cru, nunca pelos repositorios da aplicacao. Preparar o cenario com o codigo sob
 * teste fecha um circulo: um mapeamento invertido gravaria invertido, leria invertido e passaria.
 */
public final class ProfileFixtures {

  /** Sintetico e obviamente sintetico: ninguem confunde com o conteudo publicado. */
  public static final String FULL_NAME = "Perfil De Teste";

  public static final String HEADLINE = "Headline de teste";
  public static final String BIO = "Bio de teste, escrita para o ProfileIntegrationTest.";

  /**
   * Acentuacao de proposito.
   *
   * <p>E o unico ponto da suite onde til, cedilha e o ponto medio atravessam a cadeia inteira -
   * driver JDBC, Hibernate, Jackson e os bytes da resposta HTTP. Mojibake nao quebra nada: grava,
   * serializa e entrega, e so aparece na tela do visitante. Sem uma afirmacao sobre o texto exato,
   * o build seguiria verde.
   */
  public static final String LOCATION = "Sertãozinho · São Paulo";

  private static final String SEED_SCRIPT = "db/migration/R__seed_profile.sql";

  private ProfileFixtures() {}

  /**
   * Deixa no banco exatamente um perfil de teste, com tres links.
   *
   * <p>Os links entram fora da ordem de exibicao - {@code email} primeiro, com {@code
   * display_order} 2. E o que impede a assercao de ordem de passar por acidente: com a insercao ja
   * ordenada, ela seguiria verde sem ninguem ordenar coisa alguma.
   *
   * <p>Medido, e o resultado corrige o que se supunha: removida a ordenacao de {@code
   * Profile.normalizeLinks}, a resposta volta na ordem de insercao e o teste falha. Removido apenas
   * o {@code @OrderBy} da entidade, o teste passa. Quem garante a ordem e o dominio; o ORDER BY no
   * SQL e redundancia, e nao a fonte da garantia.
   */
  public static void replaceWithTestProfile(JdbcTemplate jdbcTemplate) {
    empty(jdbcTemplate);
    jdbcTemplate.update(
        """
        INSERT INTO profile (full_name, headline, bio, location, resume_url, available_for_work)
        VALUES (?, ?, ?, ?, NULL, TRUE)
        """,
        FULL_NAME,
        HEADLINE,
        BIO,
        LOCATION);

    // A tabela e travada em uma linha pela constraint singleton, e o id e
    // GENERATED ALWAYS - entao ele se descobre por consulta, nunca se fixa.
    Long profileId = jdbcTemplate.queryForObject("SELECT id FROM profile", Long.class);

    insertLink(jdbcTemplate, profileId, "email", "mailto:teste@exemplo.dev", 2);
    insertLink(jdbcTemplate, profileId, "github", "https://github.com/exemplo", 0);
    insertLink(jdbcTemplate, profileId, "linkedin", "https://www.linkedin.com/in/exemplo/", 1);
  }

  /**
   * Banco sem perfil nenhum - o cenario do 404.
   *
   * <p>Apaga so {@code profile}: os links vao junto pelo {@code ON DELETE CASCADE} declarado na V1.
   * Apagar as duas tabelas a mao seria mais explicito e testaria menos - do jeito que esta, um
   * cascade perdido numa migracao futura deixa linha orfa e a contagem denuncia.
   */
  public static void empty(JdbcTemplate jdbcTemplate) {
    jdbcTemplate.update("DELETE FROM profile");
  }

  /**
   * Reaplica o seed de producao, o mesmo arquivo que o Flyway executa.
   *
   * <p>Serve a dois propositos: devolver o banco ao estado padrao depois de cada teste, e permitir
   * afirmar sobre a idempotencia que o {@code R__} promete no proprio cabecalho.
   *
   * <p>O {@code EncodedResource} com UTF-8 explicito repete a escolha do {@code flyway.encoding}, e
   * pela mesma razao: em Windows o charset padrao da JVM nao e UTF-8, e ler o arquivo como CP1252
   * gravaria mojibake sem erro nenhum.
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

  private static void insertLink(
      JdbcTemplate jdbcTemplate, Long profileId, String platform, String url, int displayOrder) {
    jdbcTemplate.update(
        """
        INSERT INTO social_link (profile_id, platform, url, display_order)
        VALUES (?, ?, ?, ?)
        """,
        profileId,
        platform,
        url,
        displayOrder);
  }
}
