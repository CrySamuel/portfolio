package dev.crystofer.portfolio.projects.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.crystofer.portfolio.projects.domain.model.Project;
import dev.crystofer.portfolio.projects.domain.port.out.LoadProjectPort;
import dev.crystofer.portfolio.shared.domain.Slug;
import dev.crystofer.portfolio.shared.error.ResourceNotFoundException;
import dev.crystofer.portfolio.support.fixtures.ProjectSamples;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

  @Mock LoadProjectPort loadProjectPort;

  /**
   * A ordem sai do catalogo, e nao da porta.
   *
   * <p>A porta devolve na ordem em que a origem entregar - aqui, de proposito, fora de ordem. Se o
   * servico apenas repassasse a lista, este teste seria o que reprovaria.
   */
  @Test
  @DisplayName("deve montar o catalogo ordenado a partir da lista da porta")
  void shouldBuildOrderedCatalog_fromThePortList() {
    // given
    Project segundo = ProjectSamples.projeto("portfolio", "Portfolio", 1, true);
    Project primeiro = ProjectSamples.projeto("finai", "FinAI", 0, true);
    given(loadProjectPort.loadProjects()).willReturn(List.of(segundo, primeiro));

    // when
    var catalogo = new ProjectService(loadProjectPort).listProjects();

    // then
    assertThat(catalogo.projects())
        .extracting(Project::title)
        .containsExactly("FinAI", "Portfolio");
  }

  /**
   * Catalogo vazio nao e erro, e a diferenca em relacao ao perfil e de significado.
   *
   * <p>Portfolio sem perfil e sistema quebrado; portfolio sem projeto e conteudo que o dono ainda
   * nao informou. Traduzir isto em excecao poria a secao inteira fora do ar enquanto o seed nao
   * chega - que e o estado real do projeto neste commit.
   */
  @Test
  @DisplayName("deve devolver catalogo vazio quando a porta nao tem nada")
  void shouldReturnEmptyCatalog_whenThePortHasNothing() {
    given(loadProjectPort.loadProjects()).willReturn(List.of());

    assertThat(new ProjectService(loadProjectPort).listProjects().isEmpty()).isTrue();
  }

  @Test
  @DisplayName("deve devolver o projeto do slug pedido")
  void shouldReturnProject_whenSlugExists() {
    // given
    Project projeto = ProjectSamples.projeto("finai", "FinAI", 0, true);
    given(loadProjectPort.loadProjectBySlug(Slug.of("finai"))).willReturn(Optional.of(projeto));

    // when
    Project encontrado = new ProjectService(loadProjectPort).getProjectBySlug(Slug.of("finai"));

    // then
    assertThat(encontrado.title()).isEqualTo("FinAI");
  }

  /**
   * Aqui a ausencia vira excecao, ao contrario da listagem.
   *
   * <p>E nesta classe, num lugar so, que o {@code Optional} da porta deixa de existir. Se ele
   * vazasse para o controlador, cada chamador futuro decidiria por conta o que fazer com um slug
   * que nao existe - e decidiriam diferente.
   */
  @Test
  @DisplayName("deve lancar recurso nao encontrado quando o slug nao existe")
  void shouldThrowNotFound_whenSlugIsUnknown() {
    given(loadProjectPort.loadProjectBySlug(Slug.of("nao-existe"))).willReturn(Optional.empty());

    assertThatThrownBy(
            () -> new ProjectService(loadProjectPort).getProjectBySlug(Slug.of("nao-existe")))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("nao-existe");
  }

  /** A mensagem cita o slug, que veio da URL, e nao a tabela nem o arquivo de seed (secao 2.4). */
  @Test
  @DisplayName("deve manter detalhe de infraestrutura fora da mensagem publica")
  void shouldKeepInfrastructureDetail_outOfThePublicMessage() {
    given(loadProjectPort.loadProjectBySlug(Slug.of("sumido"))).willReturn(Optional.empty());

    assertThatThrownBy(
            () -> new ProjectService(loadProjectPort).getProjectBySlug(Slug.of("sumido")))
        .hasMessageNotContainingAny("project", "seed", "SELECT", "tabela");
  }
}
