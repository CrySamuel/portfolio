package dev.crystofer.portfolio.github.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LanguageUsageTest {

  @Test
  @DisplayName("deve aparar espacos do nome")
  void shouldTrim_name() {
    assertThat(new LanguageUsage("  Java  ", 1_024).name()).isEqualTo("Java");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  @DisplayName("deve recusar nome vazio ou em branco")
  void shouldReject_whenNameIsBlank(String name) {
    assertThatThrownBy(() -> new LanguageUsage(name, 1_024))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Nome da linguagem e obrigatorio");
  }

  @Test
  @DisplayName("deve recusar nome nulo")
  void shouldReject_whenNameIsNull() {
    assertThatThrownBy(() -> new LanguageUsage(null, 1_024))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Nome da linguagem e obrigatorio");
  }

  @Test
  @DisplayName("deve recusar nome mais longo que o limite")
  void shouldReject_whenNameIsTooLong() {
    assertThatThrownBy(() -> new LanguageUsage("L".repeat(61), 1_024))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("excede 60 caracteres");
  }

  /**
   * Peso zero nao e pouco uso, e ausencia.
   *
   * <p>Aceitar zero deixaria entrar uma fatia invisivel no grafico que ainda assim ocuparia uma
   * legenda.
   */
  @ParameterizedTest
  @ValueSource(longs = {0L, -1L})
  @DisplayName("deve recusar peso nao positivo")
  void shouldReject_whenWeightIsNotPositive(long weight) {
    assertThatThrownBy(() -> new LanguageUsage("Java", weight))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Peso da linguagem precisa ser positivo");
  }

  @Test
  @DisplayName("deve calcular a fatia sobre o total")
  void shouldCompute_shareOfTotal() {
    assertThat(new LanguageUsage("Java", 750).shareOf(1_000)).isCloseTo(75.0, within(0.001));
  }

  /**
   * A fatia nao arredonda de proposito.
   *
   * <p>Arredondar e decisao de apresentacao, e uma que nao fecha em 100% sozinha - quem desenha o
   * grafico precisa escolher onde absorver a diferenca.
   */
  @Test
  @DisplayName("deve devolver a fatia sem arredondar")
  void shouldNotRound_share() {
    assertThat(new LanguageUsage("Python", 1).shareOf(3)).isCloseTo(33.333, within(0.001));
  }

  @Test
  @DisplayName("deve devolver zero quando o total e zero")
  void shouldReturnZero_whenTotalIsZero() {
    assertThat(new LanguageUsage("Java", 100).shareOf(0)).isZero();
  }

  @Test
  @DisplayName("deve dar o mesmo peso a cada repositorio")
  void shouldWeigh_eachRepositoryEqually() {
    List<LanguageUsage> mistura =
        LanguageUsage.averagingByRepository(
            List.of(repositorio("Java", 1_000L), repositorio("Python", 1_000_000L)));

    long total = mistura.stream().mapToLong(LanguageUsage::weight).sum();
    assertThat(mistura).extracting(LanguageUsage::name).containsExactlyInAnyOrder("Java", "Python");
    assertThat(fatia(mistura, "Java", total)).isCloseTo(50.0, within(0.01));
    assertThat(fatia(mistura, "Python", total)).isCloseTo(50.0, within(0.01));
  }

  /**
   * O caso real que motivou a mudanca, reduzido a numeros.
   *
   * <p>Medido no perfil do dono: um repositorio de estudo com dependencias versionadas dentro
   * carregava <strong>17,8 MB - 94,8% de todo o codigo somado</strong>, e a soma por bytes
   * publicava 93,7% Python contra 3,2% Java. Com peso por repositorio, o mesmo conjunto vira 46% e
   * 36%.
   *
   * <p>Aqui: quatro repositorios de Java pequenos contra um de Python gigante. Por bytes, Python
   * seria mais de 99%; por repositorio, Java fica com 80%.
   */
  @Test
  @DisplayName("nao deve deixar um repositorio gigante dominar a mistura")
  void shouldNotLet_oneHugeRepositoryDominate() {
    List<LanguageUsage> mistura =
        LanguageUsage.averagingByRepository(
            List.of(
                repositorio("Python", 17_800_000L),
                repositorio("Java", 20_000L),
                repositorio("Java", 30_000L),
                repositorio("Java", 5_000L),
                repositorio("Java", 700_000L)));

    long total = mistura.stream().mapToLong(LanguageUsage::weight).sum();
    assertThat(fatia(mistura, "Java", total)).isCloseTo(80.0, within(0.01));
    assertThat(fatia(mistura, "Python", total)).isCloseTo(20.0, within(0.01));
  }

  @Test
  @DisplayName("deve repartir o peso de um repositorio entre as linguagens dele")
  void shouldSplit_weightInsideARepository() {
    Map<String, Long> misto = new LinkedHashMap<>();
    misto.put("Java", 750L);
    misto.put("TypeScript", 250L);

    List<LanguageUsage> mistura = LanguageUsage.averagingByRepository(List.of(misto));
    long total = mistura.stream().mapToLong(LanguageUsage::weight).sum();

    assertThat(fatia(mistura, "Java", total)).isCloseTo(75.0, within(0.01));
    assertThat(fatia(mistura, "TypeScript", total)).isCloseTo(25.0, within(0.01));
  }

  /**
   * O repositorio de perfil - o que tem o nome do usuario - e o caso real disto.
   *
   * <p>Ele so tem um README, e o GitHub nao conta Markdown como linguagem: a resposta vem vazia.
   * Divide-lo seria dividir por zero, e conta-lo como repositorio vazio diluiria todas as fatias
   * por nada.
   */
  @Test
  @DisplayName("deve ignorar repositorio sem linguagem detectada")
  void shouldIgnore_repositoryWithoutLanguages() {
    List<LanguageUsage> mistura =
        LanguageUsage.averagingByRepository(List.of(Map.of(), repositorio("Java", 1_000L)));

    long total = mistura.stream().mapToLong(LanguageUsage::weight).sum();
    assertThat(fatia(mistura, "Java", total)).isCloseTo(100.0, within(0.01));
  }

  @Test
  @DisplayName("deve ignorar bytes nulos ou nao positivos vindos da resposta")
  void shouldIgnore_nullOrNonPositiveBytes() {
    Map<String, Long> suja = new LinkedHashMap<>();
    suja.put("Java", 1_000L);
    suja.put("Nix", null);
    suja.put("Batchfile", 0L);

    assertThat(LanguageUsage.averagingByRepository(List.of(suja)))
        .extracting(LanguageUsage::name)
        .containsExactly("Java");
  }

  @Test
  @DisplayName("deve devolver lista vazia quando nao ha repositorio com linguagem")
  void shouldReturnEmpty_whenThereIsNothingToWeigh() {
    assertThat(LanguageUsage.averagingByRepository(List.of())).isEmpty();
    assertThat(LanguageUsage.averagingByRepository(List.of(Map.of()))).isEmpty();
  }

  @Test
  @DisplayName("deve recusar lista nula em vez de trata-la como vazia")
  void shouldReject_whenListIsNull() {
    assertThatThrownBy(() -> LanguageUsage.averagingByRepository(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Lista de repositorios e obrigatoria");
  }

  private static Map<String, Long> repositorio(String linguagem, long bytes) {
    return Map.of(linguagem, bytes);
  }

  private static double fatia(List<LanguageUsage> mistura, String linguagem, long total) {
    return mistura.stream()
        .filter(uso -> uso.name().equals(linguagem))
        .findFirst()
        .orElseThrow()
        .shareOf(total);
  }
}
