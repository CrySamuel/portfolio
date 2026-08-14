package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import dev.crystofer.portfolio.shared.web.ServiceKeyAuthFilter;
import dev.crystofer.portfolio.support.fixtures.ProjectFixtures;

/**
 * Os dois endpoints do catalogo, por HTTP de verdade, contra o Postgres 16 de verdade.
 *
 * <p>O que so aqui pode ser verificado: o ETag e o 304 que ele habilita, os status de erro no
 * caminho completo, e a contagem de consultas <em>atravessando a aplicacao inteira</em> - o teste
 * do adaptador mede a porta, este mede o endpoint.
 */
class ProjectIntegrationTest extends AbstractIntegrationTest {

  @Autowired EntityManagerFactory entityManagerFactory;

  @Autowired DataSource dataSource;

  @BeforeEach
  void partirDeTabelasVazias() {
    ProjectFixtures.empty(jdbcTemplate);
  }

  @AfterEach
  void devolverOBancoAoSeed() {
    ProjectFixtures.reapplySeed(dataSource);
  }

  @Test
  @DisplayName("deve listar o catalogo com cache control e etag")
  void shouldListCatalog_withCacheControlAndEtag() {
    // given
    ProjectFixtures.insertProject(jdbcTemplate, "finai", "FinAI", true, 0, 2, 1);

    // when
    var response = getComChave("/api/v1/projects", String.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType())
        .isNotNull()
        .satisfies(tipo -> assertThat(tipo.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue());
    assertThat(response.getHeaders().getCacheControl())
        .contains("max-age=300")
        .contains("public")
        .contains("stale-while-revalidate=3600");
    assertThat(response.getHeaders().getETag()).isNotBlank();
    assertThat(response.getBody()).contains("\"slug\":\"finai\"");
  }

  /**
   * A divida do commit 19, cobrada: corpo identico responde 304 sem corpo.
   *
   * <p>O ganho e de banda, e nao de trabalho - a consulta ao banco acontece de qualquer forma. Para
   * este projeto e o suficiente: quem faz estas requisicoes e a revalidacao do ISR, e o que ela
   * ganha com 304 e nao rebaixar o cache da CDN por um corpo que nao mudou.
   */
  @Test
  @DisplayName("deve responder 304 quando o if-none-match bate")
  void shouldRespond304_whenIfNoneMatchHits() {
    // given
    ProjectFixtures.insertProject(jdbcTemplate, "finai", "FinAI", true, 0, 2, 1);
    String etag = getComChave("/api/v1/projects", String.class).getHeaders().getETag();
    assertThat(etag).isNotBlank();

    // when
    var response = getComEtag("/api/v1/projects", etag);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
    assertThat(response.getBody()).isNull();
  }

  /**
   * Corpo diferente muda o ETag, e o 304 deixa de acontecer.
   *
   * <p>Sem esta metade, um filtro que devolvesse sempre o mesmo resumo passaria no teste acima - e
   * o site serviria conteudo velho para sempre. E a mesma razao pela qual as guardas deste projeto
   * sao conferidas nos dois sentidos.
   */
  @Test
  @DisplayName("deve trocar o etag quando o conteudo muda")
  void shouldChangeEtag_whenContentChanges() {
    // given
    ProjectFixtures.insertProject(jdbcTemplate, "finai", "FinAI", true, 0, 2, 1);
    String etagAntigo = getComChave("/api/v1/projects", String.class).getHeaders().getETag();

    // when
    ProjectFixtures.insertProject(jdbcTemplate, "portfolio", "Portfolio", true, 1, 1, 1);
    var response = getComEtag("/api/v1/projects", etagAntigo);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getETag()).isNotBlank().isNotEqualTo(etagAntigo);
  }

  @Test
  @DisplayName("deve detalhar o projeto pelo slug")
  void shouldDetailProject_bySlug() {
    // given
    ProjectFixtures.insertProject(jdbcTemplate, "finai", "FinAI", true, 0, 3, 2);

    // when
    var response = getComChave("/api/v1/projects/finai", String.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .contains("\"problem\"")
        .contains("\"solution\"")
        .contains("\"outcome\"")
        .contains("\"metrics\"")
        .contains("\"liveUrl\":null");
  }

  /**
   * Slug bem formado que nao existe e 404; slug fora do formato e 400.
   *
   * <p>A distincao e o que o {@code SlugConverter} existe para produzir. Sem ele o segundo caso
   * viraria 500, e a API diria que quebrou quando quem errou foi quem digitou o endereco.
   */
  @Test
  @DisplayName("deve separar slug inexistente de slug malformado")
  void shouldSeparate_unknownSlugFromMalformedSlug() {
    var inexistente = getComChave("/api/v1/projects/nao-existe", String.class);
    var malformado = getComChave("/api/v1/projects/Slug%20Invalido", String.class);

    assertThat(inexistente.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(inexistente.getHeaders().getContentType())
        .isNotNull()
        .satisfies(
            tipo -> assertThat(tipo.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue());
    assertThat(malformado.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /** O endpoint novo nasce protegido, porque o filtro cobre {@code /api/*} por prefixo. */
  @Test
  @DisplayName("deve recusar as duas rotas sem a chave de servico")
  void shouldRefuseBothRoutes_withoutTheServiceKey() {
    assertThat(restTemplate.getForEntity("/api/v1/projects", String.class).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(restTemplate.getForEntity("/api/v1/projects/finai", String.class).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("deve responder array vazio quando nao ha projeto")
  void shouldRespondEmptyArray_whenThereAreNoProjects() {
    var response = getComChave("/api/v1/projects", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("[]");
  }

  /**
   * A contagem de consultas do endpoint nao cresce com o catalogo.
   *
   * <p>Sao duas quantidades afirmando o mesmo numero, e a igualdade entre elas e a prova - com um
   * projeto so, N+1 e invisivel, porque {@code 1 + 1} da o mesmo que a solucao correta. E a licao
   * que o commit 35 pagou ao quebrar o {@code @BatchSize} e ver o cenario pequeno passar verde.
   */
  @Test
  @DisplayName("deve manter as consultas constantes servindo a listagem")
  void shouldKeepQueriesConstant_whenServingTheListing() {
    // given
    ProjectFixtures.insertProject(jdbcTemplate, "um", "Um", true, 0, 2, 1);
    Statistics estatisticas = estatisticasLimpas();
    getComChave("/api/v1/projects", String.class);
    long comUmProjeto = estatisticas.getPrepareStatementCount();

    ProjectFixtures.insertProject(jdbcTemplate, "dois", "Dois", true, 1, 3, 2);
    ProjectFixtures.insertProject(jdbcTemplate, "tres", "Tres", false, 2, 1, 1);
    estatisticas.clear();

    // when
    var response = getComChave("/api/v1/projects", String.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(estatisticas.getPrepareStatementCount())
        .as("tres projetos precisam do mesmo numero de consultas que um")
        .isEqualTo(comUmProjeto);
  }

  /**
   * Acentuacao do banco ate o JSON, lida como bytes - mojibake quebra esta linha e nenhuma outra.
   */
  @Test
  @DisplayName("deve preservar acentuacao do banco ate a resposta")
  void shouldPreserveAccents_fromDatabaseToResponse() {
    // given
    ProjectFixtures.insertProject(jdbcTemplate, "acentos", "Gestao de Metricas", true, 0, 0, 0);
    jdbcTemplate.update(
        "UPDATE project SET title = ? WHERE slug = ?", "Gestão de Métricas", "acentos");

    // when
    var response = getComChave("/api/v1/projects/acentos", byte[].class);

    // then
    assertThat(new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8))
        .contains("Gestão de Métricas");
  }

  /**
   * O seed repetivel promete no cabecalho que rodar duas vezes deixa o banco igual.
   *
   * <p>A promessa e o que separa "migracao repetivel" de "migracao que duplica a cada deploy", e o
   * Flyway a executa em toda subida da aplicacao. Sem esta assercao ela ficaria conferida so a mao,
   * uma vez, no dia em que o arquivo foi escrito.
   *
   * <p>O snapshot inclui os <strong>ids</strong> de proposito. Um seed que apagasse tudo e
   * reinserisse tambem deixaria o conteudo igual - e descartaria as chaves, quebrando qualquer
   * referencia. Comparar ids e o que distingue {@code ON CONFLICT DO UPDATE} de {@code DELETE} mais
   * {@code INSERT}. As colunas de tempo ficam fora, porque o seed grava {@code now()}.
   */
  @Test
  @DisplayName("deve deixar o banco identico ao rodar o seed duas vezes")
  void shouldBeIdempotent_whenSeedRunsTwice() {
    // given
    ProjectFixtures.reapplySeed(dataSource);
    var primeira = snapshotDoCatalogo();
    assertThat(primeira).as("o seed precisa ter escrito alguma coisa").isNotEmpty();

    // when
    ProjectFixtures.reapplySeed(dataSource);

    // then
    assertThat(snapshotDoCatalogo()).isEqualTo(primeira);
  }

  /**
   * A lista do seed e fonte de verdade nos dois sentidos.
   *
   * <p>Sem o {@code DELETE}, o arquivo saberia acrescentar e corrigir mas nunca tirar - e conteudo
   * retirado da lista continuaria publicado para sempre. Esta guarda nao e hipotetica: foi ela que
   * tornou possivel remover o Music Style API do catalogo editando uma lista.
   */
  @Test
  @DisplayName("deve remover o projeto que sai da lista do seed")
  void shouldRemoveProject_whenItLeavesTheSeedList() {
    // given
    ProjectFixtures.reapplySeed(dataSource);
    jdbcTemplate.update(
        """
        INSERT INTO project (slug, title, summary, problem, solution, outcome)
        VALUES ('obsoleto', 'Obsoleto', 'r', 'p', 's', 'o')
        """);
    assertThat(existe("project", "obsoleto")).isTrue();

    // when
    ProjectFixtures.reapplySeed(dataSource);

    // then
    assertThat(existe("project", "obsoleto")).isFalse();
  }

  /** O mesmo para tecnologia, cuja remocao so e possivel depois de os vinculos sairem. */
  @Test
  @DisplayName("deve remover a tecnologia que sai da lista do seed")
  void shouldRemoveTechnology_whenItLeavesTheSeedList() {
    // given
    ProjectFixtures.reapplySeed(dataSource);
    jdbcTemplate.update(
        "INSERT INTO technology (name, slug, category) VALUES ('COBOL', 'cobol', 'language')");
    assertThat(existe("technology", "cobol")).isTrue();

    // when
    ProjectFixtures.reapplySeed(dataSource);

    // then
    assertThat(existe("technology", "cobol")).isFalse();
  }

  private boolean existe(String tabela, String slug) {
    Integer total =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM " + tabela + " WHERE slug = ?", Integer.class, slug);
    return total != null && total > 0;
  }

  private java.util.List<java.util.Map<String, Object>> snapshotDoCatalogo() {
    return jdbcTemplate.queryForList(
        """
        SELECT p.id, p.slug, p.title, p.display_order, p.featured, p.live_url,
               t.id AS tech_id, t.slug AS tech_slug,
               m.id AS metric_id, m.label, m.value
        FROM project p
        LEFT JOIN project_tech pt   ON pt.project_id = p.id
        LEFT JOIN technology t      ON t.id = pt.technology_id
        LEFT JOIN project_metric m  ON m.project_id = p.id
        ORDER BY p.id, t.slug, m.label
        """);
  }

  private org.springframework.http.ResponseEntity<String> getComEtag(String path, String etag) {
    var headers = new HttpHeaders();
    headers.set(ServiceKeyAuthFilter.HEADER, CHAVE_DE_SERVICO);
    headers.setIfNoneMatch(etag);
    return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
  }

  private Statistics estatisticasLimpas() {
    Statistics estatisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    assertThat(estatisticas.isStatisticsEnabled()).isTrue();
    estatisticas.clear();
    return estatisticas;
  }
}
