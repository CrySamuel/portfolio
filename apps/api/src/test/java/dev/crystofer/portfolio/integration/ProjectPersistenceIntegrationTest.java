package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.crystofer.portfolio.projects.domain.model.Project;
import dev.crystofer.portfolio.projects.domain.model.Technology;
import dev.crystofer.portfolio.projects.domain.port.out.LoadProjectPort;
import dev.crystofer.portfolio.shared.domain.Slug;
import dev.crystofer.portfolio.support.fixtures.ProjectFixtures;

/**
 * O adaptador de projetos contra o Postgres 16 de verdade.
 *
 * <p>A promessa que este teste guarda e a da Definition of Done do MVP 3 - <em>sem N+1</em> - e ela
 * so pode ser verificada com SQL real. O mapeamento tem duas colecoes, e duas colecoes sao onde o
 * N+1 costuma voltar: o {@code @EntityGraph} resolve uma, e a outra dependeria de uma consulta por
 * projeto se nada mais fosse feito.
 *
 * <p><strong>As assercoes sao sobre numero de consultas, e nao sobre tempo.</strong> Tempo varia
 * com a maquina e com o cache do sistema operacional; contagem de statements e deterministica e
 * reprova por motivo nomeavel.
 */
class ProjectPersistenceIntegrationTest extends AbstractIntegrationTest {

  @Autowired LoadProjectPort loadProjectPort;

  @Autowired EntityManagerFactory entityManagerFactory;

  @BeforeEach
  @AfterEach
  void limparAsTabelas() {
    ProjectFixtures.empty(jdbcTemplate);
  }

  /**
   * A consulta atravessa {@code project_tech} e devolve o modelo de dominio inteiro.
   *
   * <p>Sem esta leitura contra banco real, um mapeamento errado da tabela de juncao atravessaria a
   * suite e falharia na primeira consulta de producao - o mesmo raciocinio que ja tinha levado o
   * {@code jsonb} de {@code experience} a ser exercitado em vez de apenas validado.
   */
  @Test
  @DisplayName("deve carregar projeto com tecnologias e metricas do banco real")
  void shouldLoadProjectGraph_fromRealDatabase() {
    // given
    ProjectFixtures.insertProject(jdbcTemplate, "finai", "FinAI", true, 0, 3, 2);

    // when
    List<Project> projetos = loadProjectPort.loadProjects();

    // then
    assertThat(projetos).hasSize(1);
    Project projeto = projetos.get(0);
    assertThat(projeto.slug()).isEqualTo(Slug.of("finai"));
    assertThat(projeto.technologies()).hasSize(3);
    assertThat(projeto.metrics()).hasSize(2);
    assertThat(projeto.technologies()).extracting(Technology::slug).doesNotHaveDuplicates();
  }

  /**
   * O numero de consultas nao cresce com o numero de projetos, e e essa a definicao de "sem N+1".
   *
   * <p>Tres projetos, cada um com tecnologias e metricas proprias. Com o {@code @EntityGraph}
   * sozinho seriam 1 + 3; com o {@code @BatchSize} nas metricas, o lote resolve as tres numa
   * consulta. O teste afirma o mesmo numero que o cenario de um projeto - a igualdade e o que
   * demonstra a constancia, e nao o valor em si.
   */
  @Test
  @DisplayName("deve manter o numero de consultas constante quando ha mais projetos")
  void shouldKeepQueryCountConstant_whenThereAreMoreProjects() {
    // given
    ProjectFixtures.insertProject(jdbcTemplate, "finai", "FinAI", true, 0, 3, 2);
    ProjectFixtures.insertProject(jdbcTemplate, "portfolio", "Portfolio", true, 1, 4, 3);
    ProjectFixtures.insertProject(jdbcTemplate, "terceiro", "Terceiro", false, 2, 2, 1);
    Statistics statistics = estatisticasLimpas();

    // when
    List<Project> projetos = loadProjectPort.loadProjects();

    // then
    assertThat(projetos).hasSize(3);
    assertThat(projetos).flatExtracting(Project::metrics).hasSize(6);
    assertThat(statistics.getPrepareStatementCount())
        .as("uma consulta para projetos e tecnologias, outra para o lote de metricas")
        .isEqualTo(2);
  }

  /**
   * O mesmo numero com um projeto so - e a comparacao com o caso de tres que prova a constancia.
   */
  @Test
  @DisplayName("deve gastar as mesmas consultas com um unico projeto")
  void shouldSpendTheSameQueries_whenThereIsOneProject() {
    // given
    ProjectFixtures.insertProject(jdbcTemplate, "finai", "FinAI", true, 0, 3, 2);
    Statistics statistics = estatisticasLimpas();

    // when
    loadProjectPort.loadProjects();

    // then
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("deve buscar pelo slug sem carregar o catalogo inteiro")
  void shouldFindBySlug_withoutLoadingTheWholeCatalog() {
    // given
    ProjectFixtures.insertProject(jdbcTemplate, "finai", "FinAI", true, 0, 3, 2);
    ProjectFixtures.insertProject(jdbcTemplate, "portfolio", "Portfolio", true, 1, 4, 3);
    Statistics statistics = estatisticasLimpas();

    // when
    var encontrado = loadProjectPort.loadProjectBySlug(Slug.of("portfolio"));

    // then
    assertThat(encontrado).isPresent();
    assertThat(encontrado.get().title()).isEqualTo("Portfolio");
    assertThat(encontrado.get().technologies()).hasSize(4);
    assertThat(encontrado.get().metrics()).hasSize(3);
    assertThat(statistics.getPrepareStatementCount())
        .as("o projeto com suas tecnologias, mais o lote de metricas")
        .isEqualTo(2);
  }

  @Test
  @DisplayName("deve devolver vazio para slug que nao existe")
  void shouldReturnEmpty_whenSlugIsUnknown() {
    ProjectFixtures.insertProject(jdbcTemplate, "finai", "FinAI", true, 0, 1, 1);

    assertThat(loadProjectPort.loadProjectBySlug(Slug.of("nao-existe"))).isEmpty();
  }

  /**
   * Catalogo vazio e lista vazia, e nao excecao.
   *
   * <p>E a mesma escolha que a timeline fez: portfolio sem projeto cadastrado e conteudo que o dono
   * ainda nao informou. Como o seed dos projetos so chega depois do endpoint, este e o estado real
   * de producao por alguns commits.
   */
  @Test
  @DisplayName("deve devolver lista vazia quando nao ha projeto nenhum")
  void shouldReturnEmptyList_whenThereAreNoProjects() {
    assertThat(loadProjectPort.loadProjects()).isEmpty();
  }

  /**
   * Um projeto sem tecnologia e sem metrica atravessa o mapeamento.
   *
   * <p>O {@code LEFT JOIN} do grafo e o lote precisam lidar com colecao vazia sem devolver uma
   * lista com um elemento nulo dentro, que e o modo classico de o fetch join errar.
   */
  @Test
  @DisplayName("deve carregar projeto sem tecnologia e sem metrica")
  void shouldLoadProject_whenCollectionsAreEmpty() {
    ProjectFixtures.insertProject(jdbcTemplate, "vazio", "Vazio", false, 0, 0, 0);

    List<Project> projetos = loadProjectPort.loadProjects();

    assertThat(projetos).hasSize(1);
    assertThat(projetos.get(0).technologies()).isEmpty();
    assertThat(projetos.get(0).metrics()).isEmpty();
  }

  private Statistics estatisticasLimpas() {
    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    assertThat(statistics.isStatisticsEnabled())
        .as("generate_statistics precisa estar ligado no application-test.yml")
        .isTrue();
    statistics.clear();
    return statistics;
  }
}
