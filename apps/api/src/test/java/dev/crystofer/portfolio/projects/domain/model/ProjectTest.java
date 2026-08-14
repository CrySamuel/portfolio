package dev.crystofer.portfolio.projects.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.crystofer.portfolio.shared.domain.Slug;

class ProjectTest {

  private static final Technology JAVA =
      new Technology("Java", Slug.of("java"), TechnologyCategory.LANGUAGE, null);
  private static final Technology DOCKER =
      new Technology("Docker", Slug.of("docker"), TechnologyCategory.INFRASTRUCTURE, null);
  private static final Technology SPRING =
      new Technology("Spring Boot", Slug.of("spring-boot"), TechnologyCategory.FRAMEWORK, null);

  @Test
  @DisplayName("deve aceitar projeto com narrativa completa")
  void shouldAccept_whenNarrativeIsComplete() {
    // when
    Project projeto = projeto().build();

    // then
    assertThat(projeto.slug()).isEqualTo(Slug.of("finai"));
    assertThat(projeto.problem()).isEqualTo("Planilha nao sobrevive a rotina.");
    assertThat(projeto.solution()).isEqualTo("Bot de Telegram com LLM.");
    assertThat(projeto.outcome()).isEqualTo("R$ 800 economizados em um mes.");
  }

  @Test
  @DisplayName("deve recusar projeto sem slug")
  void shouldReject_whenSlugIsNull() {
    assertThatThrownBy(() -> projeto().slug(null).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Slug do projeto");
  }

  /**
   * A regra que da nome ao MVP, exercida nos tres campos.
   *
   * <p>A secao 16 registra o risco - "fiz uma API REST" - e escolhe uma mitigacao estrutural: sem
   * os tres textos o objeto nao existe, entao nao ha caminho pelo qual um projeto sem narrativa
   * chegue a tela.
   */
  @ParameterizedTest
  @DisplayName("deve recusar problema vazio ou nulo")
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void shouldReject_whenProblemIsBlank(String invalido) {
    assertThatThrownBy(() -> projeto().problem(invalido).build())
        .hasMessageContaining("Problema e obrigatorio");
  }

  @ParameterizedTest
  @DisplayName("deve recusar solucao vazia ou nula")
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void shouldReject_whenSolutionIsBlank(String invalido) {
    // A mensagem sai do mesmo helper generico que Experience usa para "Descricao",
    // entao a concordancia fica no masculino. Manter o helper unico vale mais que
    // acertar o genero de um campo com um caminho de codigo proprio.
    assertThatThrownBy(() -> projeto().solution(invalido).build())
        .hasMessageContaining("Solucao e obrigatorio");
  }

  @ParameterizedTest
  @DisplayName("deve recusar resultado vazio ou nulo")
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void shouldReject_whenOutcomeIsBlank(String invalido) {
    assertThatThrownBy(() -> projeto().outcome(invalido).build())
        .hasMessageContaining("Resultado e obrigatorio");
  }

  @Test
  @DisplayName("deve recusar titulo e resumo acima dos limites das colunas")
  void shouldReject_whenTitleOrSummaryExceedTheColumns() {
    assertThatThrownBy(() -> projeto().title("a".repeat(121)).build())
        .hasMessageContaining("excede 120");
    assertThatThrownBy(() -> projeto().summary("a".repeat(281)).build())
        .hasMessageContaining("excede 280");
  }

  /**
   * Projeto sem nenhum endereco publico e valido, e isso e decisao.
   *
   * <p>Trabalho sob acordo de confidencialidade e real e nao tem link para mostrar. Exigi-lo aqui
   * transformaria uma politica de conteudo, que ainda nao foi tomada, em invariante de dominio.
   */
  @Test
  @DisplayName("deve aceitar projeto sem repositorio e sem site")
  void shouldAccept_whenBothUrlsAreAbsent() {
    Project projeto = projeto().repoUrl(null).liveUrl(null).build();

    assertThat(projeto.findRepoUrl()).isEmpty();
    assertThat(projeto.findLiveUrl()).isEmpty();
  }

  @Test
  @DisplayName("deve expor os enderecos quando existem")
  void shouldExposeUrls_whenPresent() {
    Project projeto =
        projeto().repoUrl("https://github.com/x/y").liveUrl("https://exemplo.dev").build();

    assertThat(projeto.findRepoUrl()).contains("https://github.com/x/y");
    assertThat(projeto.findLiveUrl()).contains("https://exemplo.dev");
  }

  /**
   * Sem esquema e o erro que nao aparece, e {@code http} e o que o navegador bloqueia.
   *
   * <p>{@code github.com/user/repo} num href vira caminho relativo, resolvido contra o proprio
   * site, e devolve 404 sem erro no console. E texto claro numa pagina HTTPS e conteudo misto.
   */
  @ParameterizedTest
  @DisplayName("deve recusar endereco sem https")
  @ValueSource(
      strings = {"github.com/CrySamuel/finai", "http://finai.dev", "//finai.dev", "ftp://x"})
  void shouldReject_whenUrlHasNoHttpsScheme(String invalido) {
    assertThatThrownBy(() -> projeto().repoUrl(invalido).build())
        .hasMessageContaining("precisa comecar com https://");
    assertThatThrownBy(() -> projeto().liveUrl(invalido).build())
        .hasMessageContaining("precisa comecar com https://");
  }

  @Test
  @DisplayName("deve recusar capa em branco, aceitando ausente")
  void shouldReject_whenCoverIsBlankButAcceptAbsent() {
    assertThat(projeto().coverImage(null).build().coverImage()).isNull();
    assertThatThrownBy(() -> projeto().coverImage("  ").build()).hasMessageContaining("use null");
  }

  @Test
  @DisplayName("deve aceitar projeto sem data de publicacao")
  void shouldAccept_whenPublishedAtIsAbsent() {
    assertThat(projeto().publishedAt(null).build().publishedAt()).isNull();
  }

  /**
   * A ordem e por nome, e nao por categoria.
   *
   * <p>Agrupar chips por familia e escolha de apresentacao, e o front ja recebe a categoria em cada
   * um. Ordenar por categoria aqui daria peso semantico a ordem de declaracao de {@link
   * TechnologyCategory}, que aquele enum recusa ter.
   */
  @Test
  @DisplayName("deve ordenar as tecnologias por nome")
  void shouldOrderTechnologies_byName() {
    Project projeto = projeto().technologies(List.of(SPRING, JAVA, DOCKER)).build();

    assertThat(projeto.technologies())
        .extracting(Technology::name)
        .containsExactly("Docker", "Java", "Spring Boot");
  }

  @Test
  @DisplayName("deve recusar a mesma tecnologia duas vezes")
  void shouldReject_whenTechnologyRepeats() {
    Technology outroNome =
        new Technology("Java 21", Slug.of("java"), TechnologyCategory.LANGUAGE, null);

    assertThatThrownBy(() -> projeto().technologies(List.of(JAVA, outroNome)).build())
        .hasMessageContaining("Tecnologia repetida no projeto: java");
  }

  @Test
  @DisplayName("deve ordenar as metricas pela ordem editorial, com o rotulo desempatando")
  void shouldOrderMetrics_byEditorialOrderThenLabel() {
    ProjectMetric terceira = new ProjectMetric("uptime", "99%", 5);
    ProjectMetric primeira = new ProjectMetric("economia", "R$ 800+", 0);
    ProjectMetric empatadaB = new ProjectMetric("b-latencia", "80ms", 5);

    Project projeto = projeto().metrics(List.of(terceira, primeira, empatadaB)).build();

    assertThat(projeto.metrics())
        .extracting(ProjectMetric::label)
        .containsExactly("economia", "b-latencia", "uptime");
  }

  @Test
  @DisplayName("deve recusar duas metricas com o mesmo rotulo")
  void shouldReject_whenMetricLabelRepeats() {
    assertThatThrownBy(
            () ->
                projeto()
                    .metrics(
                        List.of(
                            new ProjectMetric("p95", "80ms", 0),
                            new ProjectMetric("p95", "90ms", 1)))
                    .build())
        .hasMessageContaining("Metrica repetida no projeto: p95");
  }

  @Test
  @DisplayName("deve recusar listas nulas em vez de trata-las como vazias")
  void shouldReject_whenCollectionsAreNull() {
    assertThatThrownBy(() -> projeto().technologies(null).build())
        .hasMessageContaining("Lista de tecnologias");
    assertThatThrownBy(() -> projeto().metrics(null).build())
        .hasMessageContaining("Lista de metricas");
  }

  /**
   * Copia defensiva, exercitada e nao suposta.
   *
   * <p>Sem ela o record guardaria a referencia recebida, e quem a passou poderia seguir alterando a
   * lista depois de o objeto existir - um valor que muda por baixo de quem ja o leu.
   */
  @Test
  @DisplayName("deve copiar as colecoes recebidas")
  void shouldCopy_theIncomingCollections() {
    List<Technology> mutavel = new ArrayList<>(List.of(JAVA));
    Project projeto = projeto().technologies(mutavel).build();

    mutavel.add(DOCKER);

    assertThat(projeto.technologies()).hasSize(1);
  }

  private static Builder projeto() {
    return new Builder();
  }

  /**
   * Construtor de teste, porque o record tem catorze componentes.
   *
   * <p>Sem ele cada caso repetiria os catorze argumentos para variar um, e a linha que importa
   * ficaria escondida no meio dos treze iguais.
   */
  private static final class Builder {
    private Slug slug = Slug.of("finai");
    private String title = "FinAI";
    private String summary = "Assistente financeiro no Telegram.";
    private String problem = "Planilha nao sobrevive a rotina.";
    private String solution = "Bot de Telegram com LLM.";
    private String outcome = "R$ 800 economizados em um mes.";
    private String repoUrl = "https://github.com/CrySamuel/FinAI-Bot";
    private String liveUrl;
    private String coverImage;
    private boolean featured = true;
    private int displayOrder;
    private LocalDate publishedAt = LocalDate.of(2026, 3, 24);
    private List<Technology> technologies = List.of(JAVA);
    private List<ProjectMetric> metrics = List.of(new ProjectMetric("economia", "R$ 800+", 0));

    Builder slug(Slug value) {
      this.slug = value;
      return this;
    }

    Builder title(String value) {
      this.title = value;
      return this;
    }

    Builder summary(String value) {
      this.summary = value;
      return this;
    }

    Builder problem(String value) {
      this.problem = value;
      return this;
    }

    Builder solution(String value) {
      this.solution = value;
      return this;
    }

    Builder outcome(String value) {
      this.outcome = value;
      return this;
    }

    Builder repoUrl(String value) {
      this.repoUrl = value;
      return this;
    }

    Builder liveUrl(String value) {
      this.liveUrl = value;
      return this;
    }

    Builder coverImage(String value) {
      this.coverImage = value;
      return this;
    }

    Builder publishedAt(LocalDate value) {
      this.publishedAt = value;
      return this;
    }

    Builder technologies(List<Technology> value) {
      this.technologies = value;
      return this;
    }

    Builder metrics(List<ProjectMetric> value) {
      this.metrics = value;
      return this;
    }

    Project build() {
      return new Project(
          slug,
          title,
          summary,
          problem,
          solution,
          outcome,
          repoUrl,
          liveUrl,
          coverImage,
          featured,
          displayOrder,
          publishedAt,
          technologies,
          metrics);
    }
  }
}
