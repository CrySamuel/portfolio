package dev.crystofer.portfolio.projects.domain.model;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.crystofer.portfolio.shared.domain.Slug;

/**
 * Um projeto do catalogo, com a narrativa que separa case de listagem de repositorio.
 *
 * <p>Record pelo mesmo motivo de {@code Experience}: e um valor lido, nunca alterado em memoria. O
 * conteudo entra por migracao (ADR-0004).
 *
 * <p><strong>Problema, solucao e resultado sao obrigatorios</strong>, e essa e a regra que da nome
 * ao MVP. A secao 16 registra o risco - a tentacao de escrever "fiz uma API REST" - e a mitigacao e
 * estrutural: sem os tres textos o objeto nao existe, entao nao ha caminho pelo qual um projeto sem
 * narrativa chegue a tela. A {@code V4__create_project_tables} diz o mesmo com {@code NOT NULL}, e
 * a repeticao e deliberada pela razao de sempre - o dominio nao pode depender de o banco estar
 * correto para estar correto.
 *
 * <p><strong>Repositorio e site podem faltar os dois</strong>, e isso tambem e decisao. Trabalho
 * sob acordo de confidencialidade e real e nao tem link publico; exigi-lo aqui transformaria uma
 * politica de conteudo, que ainda nao foi tomada, em invariante de dominio.
 *
 * @param slug identificador da URL publica
 * @param title nome do projeto
 * @param summary uma ou duas frases, o texto do card
 * @param problem o que doia antes
 * @param solution o que foi construido
 * @param outcome o que mudou, de preferencia com numero
 * @param repoUrl endereco do repositorio; {@code null} quando nao ha
 * @param liveUrl endereco do que esta no ar; {@code null} quando nao ha
 * @param coverImage caminho da imagem de capa; {@code null} quando nao ha
 * @param featured se aparece em destaque na home
 * @param displayOrder posicao editorial na listagem; o mais forte primeiro
 * @param publishedAt quando ficou pronto; {@code null} quando nao ha data honesta
 * @param technologies tecnologias declaradas, em ordem alfabetica
 * @param metrics numeros que sustentam o resultado
 */
public record Project(
    Slug slug,
    String title,
    String summary,
    String problem,
    String solution,
    String outcome,
    String repoUrl,
    String liveUrl,
    String coverImage,
    boolean featured,
    int displayOrder,
    LocalDate publishedAt,
    List<Technology> technologies,
    List<ProjectMetric> metrics) {

  // Espelham os limites das colunas em V4__create_project_tables.sql. Narrativa
  // nao tem limite porque as colunas sao TEXT: os tres textos vivem na pagina de
  // detalhe, que rola, entao nao ha layout a defender.
  private static final int MAX_TITLE_LENGTH = 120;
  private static final int MAX_SUMMARY_LENGTH = 280;

  /** Exigido pelo {@code project_repo_url_ck} e pelo {@code project_live_url_ck}. */
  private static final String REQUIRED_SCHEME = "https://";

  /**
   * Ordem editorial das metricas, com o rotulo desempatando.
   *
   * <p>O {@code displayOrder} vem da coluna porque nao ha nada no dado de onde deduzir que a
   * economia mensal importa mais que o tempo de resposta. O rotulo fecha a ordem: duas metricas com
   * o mesmo numero sairiam em ordem indefinida, e o seed nao impede numeros repetidos.
   */
  private static final Comparator<ProjectMetric> POR_ORDEM_EDITORIAL =
      Comparator.comparingInt(ProjectMetric::displayOrder).thenComparing(ProjectMetric::label);

  public Project {
    if (slug == null) {
      throw new IllegalArgumentException("Slug do projeto e obrigatorio");
    }
    title = requireText(title, "Titulo", MAX_TITLE_LENGTH);
    summary = requireText(summary, "Resumo", MAX_SUMMARY_LENGTH);
    problem = requireText(problem, "Problema", Integer.MAX_VALUE);
    solution = requireText(solution, "Solucao", Integer.MAX_VALUE);
    outcome = requireText(outcome, "Resultado", Integer.MAX_VALUE);
    repoUrl = normalizeUrl(repoUrl, "Repositorio");
    liveUrl = normalizeUrl(liveUrl, "Endereco do site");
    coverImage = normalizeOptionalText(coverImage, "Imagem de capa");
    technologies = normalizeTechnologies(technologies);
    metrics = normalizeMetrics(metrics);
  }

  /**
   * Endereco do repositorio, quando ha.
   *
   * <p>Existe como {@link Optional} - e {@code coverImage} e {@code publishedAt} nao - porque estes
   * dois sao os campos sobre os quais o card <em>decide</em>: havendo link, ele renderiza um botao
   * a mais e muda a ordem de foco. Capa e data sao exibidas ou omitidas, sem ramificacao.
   */
  public Optional<String> findRepoUrl() {
    return Optional.ofNullable(repoUrl);
  }

  /** Endereco do que esta no ar, quando ha. */
  public Optional<String> findLiveUrl() {
    return Optional.ofNullable(liveUrl);
  }

  private static String requireText(String value, String field, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " e obrigatorio");
    }
    String trimmed = value.trim();
    if (trimmed.length() > maxLength) {
      throw new IllegalArgumentException(
          field + " excede " + maxLength + " caracteres: " + trimmed.length());
    }
    return trimmed;
  }

  /**
   * Ausente e valido; presente exige o esquema.
   *
   * <p>Endereco sem esquema e o erro que nao aparece: {@code github.com/user/repo} num href vira
   * caminho relativo, o navegador o resolve contra o proprio site e devolve 404 - sem erro no
   * console, sem linha no log. E {@code http://} e recusado junto porque a pagina e servida por
   * HTTPS, entao um recurso em texto claro seria bloqueado como conteudo misto.
   */
  private static String normalizeUrl(String url, String field) {
    String trimmed = normalizeOptionalText(url, field);
    if (trimmed != null && !trimmed.startsWith(REQUIRED_SCHEME)) {
      throw new IllegalArgumentException(field + " precisa comecar com " + REQUIRED_SCHEME);
    }
    return trimmed;
  }

  private static String normalizeOptionalText(String value, String field) {
    if (value == null) {
      return null;
    }
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " em branco; use null quando nao houver");
    }
    return value.trim();
  }

  /**
   * Copia defensiva, unicidade por slug e ordem alfabetica.
   *
   * <p>A unicidade repete a chave primaria composta de {@code project_tech}. A ordem e por nome, e
   * <strong>nao</strong> por categoria: agrupar os chips por familia e escolha de apresentacao, e o
   * front ja recebe a categoria em cada um. Ordenar aqui por categoria daria peso semantico a ordem
   * de declaracao de {@link TechnologyCategory}, que o javadoc daquele enum recusa ter.
   */
  private static List<Technology> normalizeTechnologies(List<Technology> technologies) {
    if (technologies == null) {
      throw new IllegalArgumentException("Lista de tecnologias e obrigatoria; use List.of()");
    }
    Set<Slug> vistos = new HashSet<>();
    for (Technology technology : technologies) {
      if (!vistos.add(technology.slug())) {
        throw new IllegalArgumentException("Tecnologia repetida no projeto: " + technology.slug());
      }
    }
    return technologies.stream().sorted(Comparator.comparing(Technology::name)).toList();
  }

  /** Copia defensiva, unicidade por rotulo e ordem editorial. */
  private static List<ProjectMetric> normalizeMetrics(List<ProjectMetric> metrics) {
    if (metrics == null) {
      throw new IllegalArgumentException("Lista de metricas e obrigatoria; use List.of()");
    }
    Set<String> vistos = new HashSet<>();
    for (ProjectMetric metric : metrics) {
      if (!vistos.add(metric.label())) {
        throw new IllegalArgumentException("Metrica repetida no projeto: " + metric.label());
      }
    }
    return metrics.stream().sorted(POR_ORDEM_EDITORIAL).toList();
  }
}
