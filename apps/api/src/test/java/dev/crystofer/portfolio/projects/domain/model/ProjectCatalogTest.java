package dev.crystofer.portfolio.projects.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.crystofer.portfolio.shared.domain.Slug;

class ProjectCatalogTest {

  @Test
  @DisplayName("deve ordenar pela ordem editorial")
  void shouldOrder_byEditorialOrder() {
    // given
    Project segundo = projeto("portfolio", "Este portfolio", 1, true);
    Project primeiro = projeto("finai", "FinAI", 0, true);

    // when
    ProjectCatalog catalogo = new ProjectCatalog(List.of(segundo, primeiro));

    // then
    assertThat(catalogo.projects())
        .extracting(Project::title)
        .containsExactly("FinAI", "Este portfolio");
  }

  /**
   * O desempate existe porque o seed nao impede numeros repetidos.
   *
   * <p>Sem ele, dois projetos com a mesma ordem sairiam numa sequencia indefinida, e a pagina
   * mudaria de aparencia entre dois deploys sem que nada tivesse mudado.
   */
  @Test
  @DisplayName("deve desempatar pelo titulo quando a ordem editorial coincide")
  void shouldBreakTies_byTitle() {
    Project zeta = projeto("zeta", "Zeta", 3, false);
    Project alfa = projeto("alfa", "Alfa", 3, false);

    ProjectCatalog catalogo = new ProjectCatalog(List.of(zeta, alfa));

    assertThat(catalogo.projects()).extracting(Project::title).containsExactly("Alfa", "Zeta");
  }

  @Test
  @DisplayName("deve recusar o mesmo slug duas vezes")
  void shouldReject_whenSlugRepeats() {
    Project um = projeto("finai", "FinAI", 0, true);
    Project outro = projeto("finai", "FinAI v2", 1, false);

    assertThatThrownBy(() -> new ProjectCatalog(List.of(um, outro)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Projeto repetido no catalogo: finai");
  }

  @Test
  @DisplayName("deve recusar lista nula em vez de trata-la como vazia")
  void shouldReject_whenListIsNull() {
    assertThatThrownBy(() -> new ProjectCatalog(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ProjectCatalog.empty()");
  }

  /** Catalogo vazio e estado legitimo enquanto o seed nao chega, e nao erro. */
  @Test
  @DisplayName("deve aceitar catalogo vazio")
  void shouldAccept_whenEmpty() {
    assertThat(ProjectCatalog.empty().isEmpty()).isTrue();
    assertThat(ProjectCatalog.empty().projects()).isEmpty();
    assertThat(ProjectCatalog.empty().featured()).isEmpty();
  }

  /**
   * O recorte dos destacados mora aqui, e nao no componente.
   *
   * <p>A home mostra este subconjunto e a listagem mostra o todo. Com o filtro escrito nos dois
   * lugares, a home e a listagem poderiam discordar sobre o que "em destaque" significa.
   */
  @Test
  @DisplayName("deve devolver so os destacados, na mesma ordem editorial")
  void shouldReturnFeatured_inTheSameOrder() {
    Project destaqueTardio = projeto("portfolio", "Este portfolio", 2, true);
    Project comum = projeto("estudo", "Estudo", 1, false);
    Project destaquePrimeiro = projeto("finai", "FinAI", 0, true);

    ProjectCatalog catalogo = new ProjectCatalog(List.of(destaqueTardio, comum, destaquePrimeiro));

    assertThat(catalogo.projects()).hasSize(3);
    assertThat(catalogo.featured())
        .extracting(Project::title)
        .containsExactly("FinAI", "Este portfolio");
  }

  @Test
  @DisplayName("deve encontrar o projeto pelo slug")
  void shouldFind_bySlug() {
    ProjectCatalog catalogo = new ProjectCatalog(List.of(projeto("finai", "FinAI", 0, true)));

    assertThat(catalogo.findBySlug(Slug.of("finai"))).map(Project::title).contains("FinAI");
  }

  /**
   * Slug inexistente devolve vazio, e nao excecao.
   *
   * <p>Quem traduz a ausencia em 404 e a camada de aplicacao, onde a excecao de recurso nao
   * encontrado vive. O dominio nao a conhece, e a regra do ArchUnit garante isso.
   */
  @Test
  @DisplayName("deve devolver vazio para slug que nao existe")
  void shouldReturnEmpty_whenSlugIsUnknown() {
    ProjectCatalog catalogo = new ProjectCatalog(List.of(projeto("finai", "FinAI", 0, true)));

    assertThat(catalogo.findBySlug(Slug.of("nao-existe"))).isEmpty();
  }

  private static Project projeto(String slug, String title, int displayOrder, boolean featured) {
    return new Project(
        Slug.of(slug),
        title,
        "Resumo do card.",
        "O problema.",
        "A solucao.",
        "O resultado.",
        null,
        null,
        null,
        featured,
        displayOrder,
        null,
        List.of(),
        List.of());
  }
}
