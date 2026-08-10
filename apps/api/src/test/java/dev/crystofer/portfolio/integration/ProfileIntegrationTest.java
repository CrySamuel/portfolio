package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import dev.crystofer.portfolio.support.fixtures.ProfileFixtures;

/**
 * O perfil de ponta a ponta: linha no Postgres, resposta HTTP.
 *
 * <p>Existe porque nenhuma das camadas testadas isoladamente cobre o que acontece entre elas. O
 * {@code ProfileServiceTest} usa duble de porta, o {@code ProfileControllerTest} e fatia web sem
 * banco, e os testes de mapper conferem conversao com objetos montados a mao. Todos passariam com
 * uma coluna renomeada na migracao, um {@code ORDER BY} removido do repositorio ou um
 * {@code @EntityGraph} apagado. Aqui a requisicao sai pela rede, atravessa Tomcat, controlador,
 * caso de uso, adaptador, Hibernate, driver e Postgres, e volta.
 *
 * <p><strong>Cada teste estabelece o proprio cenario</strong> e o {@code @AfterEach} devolve o
 * banco ao seed. Nenhum depende da ordem de execucao nem do que o anterior deixou - o container e
 * um so para a suite inteira (ver {@link AbstractIntegrationTest}), e teste que herda estado do
 * vizinho falha em ordem aleatoria meses depois, quando alguem acrescenta um metodo no meio.
 */
class ProfileIntegrationTest extends AbstractIntegrationTest {

  private static final String ENDPOINT = "/api/v1/profile";
  private static final String CACHE_CONTROL = "max-age=300, public, stale-while-revalidate=3600";

  private final BasicJsonTester json = new BasicJsonTester(getClass());

  @Autowired DataSource dataSource;

  @Autowired EntityManagerFactory entityManagerFactory;

  @AfterEach
  void devolverOBancoAoSeed() {
    ProfileFixtures.reapplySeed(dataSource);
  }

  /**
   * O caminho feliz inteiro, com uma afirmacao por promessa do contrato (secao 3.8).
   *
   * <p>A resposta e lida como bytes e decodificada em UTF-8 a mao, e nao como String pelo
   * conversor. Nao e preciosismo: assim o que se afirma e que os bytes na rede sao UTF-8. Deixar o
   * cliente escolher o charset esconderia justamente o defeito que o teste procura - o servidor
   * emitindo em outra codificacao e o cliente adivinhando de volta.
   */
  @Test
  @DisplayName("deve servir o perfil do banco ate a resposta http, com o contrato da secao 3.8")
  void shouldServeProfileFromDatabase_whenProfileExists() {
    // given
    ProfileFixtures.replaceWithTestProfile(jdbcTemplate);

    // when
    ResponseEntity<byte[]> response = restTemplate.getForEntity(ENDPOINT, byte[].class);
    String body = new String(response.getBody(), StandardCharsets.UTF_8);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType())
        .isNotNull()
        .satisfies(type -> assertThat(type.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue());
    assertThat(response.getHeaders().getCacheControl()).isEqualTo(CACHE_CONTROL);

    assertThat(json.from(body))
        .extractingJsonPathStringValue("$.fullName")
        .isEqualTo(ProfileFixtures.FULL_NAME);
    assertThat(json.from(body))
        .extractingJsonPathStringValue("$.headline")
        .isEqualTo(ProfileFixtures.HEADLINE);
    assertThat(json.from(body)).extractingJsonPathBooleanValue("$.availableForWork").isTrue();

    // Acentuacao integra depois de atravessar driver, Hibernate, Jackson e a
    // rede - mojibake em qualquer uma das pontas quebra esta linha e nenhuma
    // outra.
    assertThat(json.from(body))
        .extractingJsonPathStringValue("$.location")
        .isEqualTo(ProfileFixtures.LOCATION);

    // A ordem do array e o contrato (secao 3.8). Quem a garante e o dominio,
    // em Profile.normalizeLinks - o @OrderBy da entidade e redundancia, e o
    // teste segue verde sem ele. Os links foram inseridos fora de ordem de
    // proposito: sem isso a assercao passaria pela ordem de insercao.
    assertThat(json.from(body))
        .extractingJsonPathArrayValue("$.socialLinks[*].platform")
        .containsExactly("github", "linkedin", "email");

    // Campo opcional vazio: chave presente com null, e nao chave omitida. O
    // jsonPath nao distingue as duas coisas (secao 4.14), entao a assercao e
    // sobre o texto cru.
    assertThat(body).contains("\"resumeUrl\":null");
  }

  /**
   * A promessa do {@code @EntityGraph}, medida.
   *
   * <p>O Javadoc do {@code ProfileJpaRepository} afirma que perfil e links saem num LEFT JOIN so.
   * Sem esta afirmacao, apagar a anotacao nao quebraria teste nenhum: a resposta seria identica, so
   * que com uma consulta a mais - e o preco apareceria no MVP 3, onde a mesma omissao vira uma
   * consulta por projeto.
   *
   * <p>O contador e do Hibernate, ligado apenas no perfil {@code test}.
   */
  @Test
  @DisplayName("deve resolver perfil e links numa consulta so, e nao em duas")
  void shouldFetchProfileAndLinksInASingleQuery_whenServingTheEndpoint() {
    // given
    ProfileFixtures.replaceWithTestProfile(jdbcTemplate);
    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    assertThat(statistics.isStatisticsEnabled())
        .as("generate_statistics precisa estar ligado no application-test.yml")
        .isTrue();
    statistics.clear();

    // when
    ResponseEntity<String> response = restTemplate.getForEntity(ENDPOINT, String.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
  }

  /**
   * O 404 contra um banco vazio de verdade.
   *
   * <p>A fatia web ja cobre o formato da resposta, mas com a excecao entregue por um duble. O que
   * so aqui se verifica e o caminho que produz a excecao: consulta que nao acha linha, {@code
   * Optional} vazio na porta, e a traducao para {@code ResourceNotFoundException} na camada de
   * aplicacao. Um {@code findFirst} que devolvesse linha errada, ou uma consulta que estourasse em
   * vez de vir vazia, passaria pela fatia e falharia aqui.
   */
  @Test
  @DisplayName("deve responder 404 em problem+json quando o banco nao tem perfil")
  void shouldReturnProblemDetail_whenDatabaseHasNoProfile() {
    // given
    ProfileFixtures.empty(jdbcTemplate);

    // when
    ResponseEntity<String> response = restTemplate.getForEntity(ENDPOINT, String.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getHeaders().getContentType())
        .isNotNull()
        .satisfies(
            type -> assertThat(type.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue());

    // Erro nao herda o frescor do caminho feliz: um 404 guardado por cinco
    // minutos na CDN sobreviveria ao deploy que aplica o seed.
    assertThat(response.getHeaders().getCacheControl()).isNotEqualTo(CACHE_CONTROL);

    String body = response.getBody();
    assertThat(json.from(body)).extractingJsonPathNumberValue("$.status").isEqualTo(404);
    assertThat(json.from(body))
        .extractingJsonPathStringValue("$.type")
        .isEqualTo("/errors/resource-not-found");
    assertThat(json.from(body))
        .extractingJsonPathStringValue("$.detail")
        .isEqualTo("Perfil nao encontrado");
  }

  /**
   * A idempotencia que o {@code R__seed_profile.sql} promete no cabecalho.
   *
   * <p>Ela ja tinha sido conferida a mao, e conferencia manual protege o dia em que foi feita. A
   * migracao e repetivel: o Flyway a reexecuta a cada mudanca de checksum, ou seja, a cada correcao
   * de texto do portfolio. Se o arquivo deixasse de ser idempotente, o efeito seria em producao, no
   * deploy.
   *
   * <p>O snapshot inclui os ids de proposito. E o que distingue upsert de {@code DELETE} seguido de
   * {@code INSERT}: os dois deixam o mesmo conteudo visivel, mas o segundo troca as chaves a cada
   * execucao e arrastaria as FKs de {@code social_link} junto. Comparar so o conteudo daria o teste
   * por bom nos dois casos - e e exatamente para evitar o segundo que existe a coluna {@code
   * singleton}.
   */
  @Test
  @DisplayName("o seed e idempotente: rodar duas vezes deixa o banco identico, ids inclusive")
  void shouldLeaveDatabaseUnchanged_whenSeedRunsTwice() {
    // given
    ProfileFixtures.reapplySeed(dataSource);
    List<Map<String, Object>> depoisDaPrimeira = snapshot();

    // when
    ProfileFixtures.reapplySeed(dataSource);

    // then
    assertThat(snapshot()).isEqualTo(depoisDaPrimeira);
    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM profile", Long.class))
        .as("a constraint singleton trava a tabela em uma linha")
        .isEqualTo(1L);
  }

  private List<Map<String, Object>> snapshot() {
    return jdbcTemplate.queryForList(
        """
        SELECT p.id AS profile_id, p.full_name, p.headline, p.bio, p.location,
               p.resume_url, p.available_for_work,
               l.id AS link_id, l.platform, l.url, l.display_order
        FROM profile p
        LEFT JOIN social_link l ON l.profile_id = p.id
        ORDER BY l.display_order
        """);
  }
}
