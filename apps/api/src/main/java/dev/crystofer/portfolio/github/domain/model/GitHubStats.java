package dev.crystofer.portfolio.github.domain.model;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * O retrato do perfil publico do GitHub que a secao de estatisticas exibe.
 *
 * <p>Este e o unico tipo do modulo que o resto do sistema conhece, e ele existe agregado - e nao
 * como tres consultas soltas - porque as tres respostas do GitHub so fazem sentido juntas: numero
 * de repositorios sem as linguagens e um numero sem contexto, e linguagens sem os repositorios nao
 * mostram o que foi construido.
 *
 * <p><strong>Vazio e um estado legitimo, e nao um erro.</strong> E o ultimo degrau da cadeia de
 * fallback do ADR-0008 - cache valido, cache expirado, {@link #empty(String)} - e e o que permite a
 * regra que o ADR resume: nenhuma falha do GitHub alcanca o visitante. A secao some ou aparece
 * vazia; a pagina nao quebra.
 *
 * <p><strong>{@code publicRepositories} e {@code repositories.size()} sao numeros diferentes de
 * proposito.</strong> O primeiro vem de {@code GET /users/{user}} e conta tudo o que e publico; o
 * segundo e o recorte que foi carregado para exibicao. Igualar os dois exigiria paginar o perfil
 * inteiro para mostrar seis cards.
 *
 * @param username dono do perfil, como o GitHub o escreve
 * @param publicRepositories quantos repositorios publicos existem no total
 * @param contributionsLastYear contribuicoes no ultimo ano, do calendario do GraphQL
 * @param languages linguagens em uso, da mais usada para a menos
 * @param repositories repositorios carregados, em ordem de destaque
 */
public record GitHubStats(
    String username,
    int publicRepositories,
    int contributionsLastYear,
    List<LanguageUsage> languages,
    List<RepositorySummary> repositories) {

  /**
   * A regra de nome de usuario do proprio GitHub: ate 39 caracteres alfanumericos, com hifens
   * simples no meio.
   *
   * <p>Validar aqui e a segunda guarda, nao a primeira. O nome vem de configuracao, entao o lugar
   * certo de recusar um valor errado e o boot da aplicacao - onde o erro aparece no deploy, com a
   * variavel nomeada, e nao numa requisicao qualquer horas depois. A guarda daqui existe porque um
   * {@code GitHubStats} sobre um nome impossivel nao descreve nada.
   */
  private static final Pattern USERNAME =
      Pattern.compile("^[a-zA-Z0-9](?:[a-zA-Z0-9]|-(?=[a-zA-Z0-9])){0,38}$");

  /**
   * Da linguagem mais presente para a menos, com o nome desempatando.
   *
   * <p>A ordem e o grafico: a primeira fatia e a maior. O desempate por nome existe porque dois
   * repositorios pequenos podem acumular exatamente o mesmo peso, e sem ele a legenda trocaria de
   * ordem entre duas revalidacoes sem que nada tivesse mudado.
   */
  private static final Comparator<LanguageUsage> POR_USO =
      Comparator.comparingLong(LanguageUsage::weight)
          .reversed()
          .thenComparing(LanguageUsage::name, String.CASE_INSENSITIVE_ORDER);

  /**
   * Mais estrelas primeiro; empatados, o que recebeu push mais recente.
   *
   * <p>Estrela e o unico sinal publico de interesse de terceiros, e por isso vem antes. Mas um
   * portfolio novo tem quase tudo com zero estrela - e ai o desempate e que decide a ordem inteira,
   * e ele foi escolhido para responder "no que essa pessoa esta trabalhando" em vez de "o que ela
   * criou primeiro".
   */
  private static final Comparator<RepositorySummary> POR_DESTAQUE =
      Comparator.comparingInt(RepositorySummary::stars)
          .reversed()
          .thenComparing(Comparator.comparing(RepositorySummary::lastPushedAt).reversed())
          .thenComparing(RepositorySummary::name, String.CASE_INSENSITIVE_ORDER);

  public GitHubStats {
    username = requireUsername(username);
    publicRepositories = requireNotNegative(publicRepositories, "Total de repositorios publicos");
    contributionsLastYear =
        requireNotNegative(contributionsLastYear, "Contribuicoes no ultimo ano");
    languages = orderLanguages(languages);
    repositories = orderRepositories(repositories);
  }

  /**
   * O retrato vazio de um perfil - o ultimo degrau do fallback.
   *
   * <p>Guarda o nome de usuario mesmo sem dado nenhum, porque e ele que a secao usa para montar o
   * link do perfil. Um vazio sem nome obrigaria a tela a ter dois caminhos de renderizacao.
   */
  public static GitHubStats empty(String username) {
    return new GitHubStats(username, 0, 0, List.of(), List.of());
  }

  /** Nao ha nada a exibir - a secao decide entre sumir e mostrar o estado vazio. */
  public boolean isEmpty() {
    return publicRepositories == 0
        && contributionsLastYear == 0
        && languages.isEmpty()
        && repositories.isEmpty();
  }

  /**
   * Soma dos pesos de todas as linguagens - o denominador da fatia de cada uma.
   *
   * <p>Vive aqui, e nao em quem desenha o grafico, porque e o unico lugar que conhece o conjunto
   * inteiro. Somar do lado de fora significaria que dois consumidores podem somar diferente.
   */
  public long totalLanguageWeight() {
    return languages.stream().mapToLong(LanguageUsage::weight).sum();
  }

  /**
   * Os primeiros repositorios da ordem de destaque.
   *
   * <p>O corte e do chamador porque quantos cards cabem e decisao de tela, e a ordem e daqui porque
   * o que "destaque" significa e decisao de negocio. E a mesma divisao que {@code
   * ProjectCatalog.featured()} faz.
   *
   * @param limit quantos no maximo; zero ou negativo devolve lista vazia
   */
  public List<RepositorySummary> highlights(int limit) {
    if (limit <= 0) {
      return List.of();
    }
    return repositories.stream().limit(limit).toList();
  }

  private static String requireUsername(String username) {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Nome de usuario do GitHub e obrigatorio");
    }
    String trimmed = username.trim();
    if (!USERNAME.matcher(trimmed).matches()) {
      throw new IllegalArgumentException("Nome de usuario do GitHub invalido: " + trimmed);
    }
    return trimmed;
  }

  private static int requireNotNegative(int value, String field) {
    if (value < 0) {
      throw new IllegalArgumentException(field + " nao pode ser negativo: " + value);
    }
    return value;
  }

  /** Copia defensiva, unicidade por nome e ordem por uso. */
  private static List<LanguageUsage> orderLanguages(List<LanguageUsage> languages) {
    if (languages == null) {
      throw new IllegalArgumentException("Lista de linguagens e obrigatoria; use List.of()");
    }
    // A comparacao de unicidade ignora caixa porque o GitHub e a unica fonte da
    // capitalizacao: duas entradas de "Java" e "JAVA" seriam a mesma linguagem
    // contada duas vezes, e o grafico somaria 100% com uma fatia sobrando.
    Set<String> vistos = new HashSet<>();
    for (LanguageUsage language : languages) {
      if (!vistos.add(language.name().toLowerCase(Locale.ROOT))) {
        throw new IllegalArgumentException("Linguagem repetida: " + language.name());
      }
    }
    return languages.stream().sorted(POR_USO).toList();
  }

  /** Copia defensiva, unicidade por nome e ordem de destaque. */
  private static List<RepositorySummary> orderRepositories(List<RepositorySummary> repositories) {
    if (repositories == null) {
      throw new IllegalArgumentException("Lista de repositorios e obrigatoria; use List.of()");
    }
    Set<String> vistos = new HashSet<>();
    for (RepositorySummary repository : repositories) {
      if (!vistos.add(repository.name().toLowerCase(Locale.ROOT))) {
        throw new IllegalArgumentException("Repositorio repetido: " + repository.name());
      }
    }
    return repositories.stream().sorted(POR_DESTAQUE).toList();
  }
}
