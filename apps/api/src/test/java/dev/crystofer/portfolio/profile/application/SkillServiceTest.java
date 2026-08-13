package dev.crystofer.portfolio.profile.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.crystofer.portfolio.profile.domain.model.Proficiency;
import dev.crystofer.portfolio.profile.domain.model.Skill;
import dev.crystofer.portfolio.profile.domain.model.SkillCategory;
import dev.crystofer.portfolio.profile.domain.port.out.LoadSkillPort;

/** Sem Spring: a porta e um duble, entao o caso de uso roda em milissegundos. */
@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

  @Mock LoadSkillPort loadSkillPort;

  /**
   * A porta devolve fora de ordem de proposito.
   *
   * <p>E o que prova que a garantia nao depende de a origem colaborar - trocar o adaptador, o banco
   * ou a consulta nao pode mudar o que sai daqui.
   */
  @Test
  @DisplayName("deve devolver o catalogo ordenado mesmo com a origem fora de ordem")
  void shouldReturnOrderedCatalog_whenSourceIsUnordered() {
    // given
    var segunda = umaCategoria("Bancos de Dados", 1);
    var primeira = umaCategoria("Linguagens", 0);
    given(loadSkillPort.loadSkillCategories()).willReturn(List.of(segunda, primeira));
    var service = new SkillService(loadSkillPort);

    // when
    var catalogo = service.listSkills();

    // then
    assertThat(catalogo.categories())
        .extracting(SkillCategory::name)
        .containsExactly("Linguagens", "Bancos de Dados");
  }

  @Test
  @DisplayName("deve devolver catalogo vazio, e nao erro, quando a origem nao tem categorias")
  void shouldReturnEmptyCatalog_whenSourceHasNone() {
    // given
    given(loadSkillPort.loadSkillCategories()).willReturn(List.of());
    var service = new SkillService(loadSkillPort);

    // when
    var catalogo = service.listSkills();

    // then
    assertThat(catalogo.isEmpty()).isTrue();
  }

  private static SkillCategory umaCategoria(String nome, int ordem) {
    return new SkillCategory(nome, ordem, List.of(new Skill("Uma", Proficiency.BASIC, null)));
  }
}
