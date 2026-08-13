package dev.crystofer.portfolio.profile.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.crystofer.portfolio.profile.adapter.in.web.mapper.SkillWebMapperImpl;
import dev.crystofer.portfolio.profile.domain.model.Proficiency;
import dev.crystofer.portfolio.profile.domain.model.Skill;
import dev.crystofer.portfolio.profile.domain.model.SkillCatalog;
import dev.crystofer.portfolio.profile.domain.model.SkillCategory;
import dev.crystofer.portfolio.profile.domain.port.in.ListSkillsUseCase;
import dev.crystofer.portfolio.shared.error.GlobalExceptionHandler;

/** Fatia web: controlador, conversor de JSON e tratador de erro, sem banco. */
@WebMvcTest(SkillController.class)
@Import({SkillWebMapperImpl.class, GlobalExceptionHandler.class})
class SkillControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean ListSkillsUseCase listSkillsUseCase;

  @Test
  @DisplayName("deve devolver as categorias como array json, com as competencias dentro")
  void shouldReturnGroupedCategories_whenUseCaseSucceeds() throws Exception {
    // given
    given(listSkillsUseCase.listSkills()).willReturn(umCatalogo());

    // when / then
    mockMvc
        .perform(get("/api/v1/skills"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].name").value("Linguagens"))
        .andExpect(jsonPath("$[0].skills[0].name").value("Java"))
        .andExpect(jsonPath("$[0].skills[0].yearsOfExperience").value(3))
        .andExpect(jsonPath("$[1].name").value("Infraestrutura"));
  }

  /**
   * O nivel sai como codigo minusculo, e nao como nome da constante.
   *
   * <p>Publicar {@code "ADVANCED"} obrigaria o cliente a normalizar, e a decidir sozinho como. E
   * tambem quebraria a uniao literal do TypeScript, que e gerada a partir do enum do contrato.
   */
  @Test
  @DisplayName("deve publicar o nivel como codigo minusculo")
  void shouldPublishLowercaseProficiency() throws Exception {
    // given
    given(listSkillsUseCase.listSkills()).willReturn(umCatalogo());

    // when / then
    mockMvc
        .perform(get("/api/v1/skills"))
        .andExpect(jsonPath("$[0].skills[0].proficiency").value("advanced"))
        .andExpect(jsonPath("$[1].skills[0].proficiency").value("intermediate"));
  }

  /**
   * Tempo ausente vira nulo explicito, e nao chave omitida.
   *
   * <p>A assercao e sobre o texto cru: o {@code jsonPath(...).doesNotExist()} tambem passa quando o
   * valor e nulo, entao ele nao distingue as duas coisas (secao 4.14). O schema declara o campo
   * como {@code required} com tipo {@code ["integer","null"]}, e ausencia e diferente de zero.
   */
  @Test
  @DisplayName("deve publicar yearsOfExperience como null explicito quando nao ha numero")
  void shouldPublishExplicitNullYears_whenAbsent() throws Exception {
    // given
    given(listSkillsUseCase.listSkills()).willReturn(umCatalogo());

    // when / then
    mockMvc
        .perform(get("/api/v1/skills"))
        .andExpect(content().string(containsString("\"yearsOfExperience\":null")));
  }

  @Test
  @DisplayName("deve responder 200 com array vazio quando nao ha competencias")
  void shouldReturnEmptyArray_whenCatalogIsEmpty() throws Exception {
    // given
    given(listSkillsUseCase.listSkills()).willReturn(SkillCatalog.empty());

    // when / then
    mockMvc
        .perform(get("/api/v1/skills"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  @DisplayName("deve declarar o mesmo frescor de cache dos outros endpoints")
  void shouldSetCacheControl_whenResponding() throws Exception {
    // given
    given(listSkillsUseCase.listSkills()).willReturn(umCatalogo());

    // when / then
    mockMvc
        .perform(get("/api/v1/skills"))
        .andExpect(
            header().string("Cache-Control", "max-age=300, public, stale-while-revalidate=3600"));
  }

  /** {@code displayOrder} nao e publicado: a ordem do array e o contrato. */
  @Test
  @DisplayName("nao deve publicar a ordem editorial como campo")
  void shouldNotPublishDisplayOrder() throws Exception {
    // given
    given(listSkillsUseCase.listSkills()).willReturn(umCatalogo());

    // when / then
    mockMvc.perform(get("/api/v1/skills")).andExpect(jsonPath("$[0].displayOrder").doesNotExist());
  }

  private static SkillCatalog umCatalogo() {
    return new SkillCatalog(
        List.of(
            new SkillCategory(
                "Linguagens",
                0,
                List.of(
                    new Skill("Java", Proficiency.ADVANCED, 3),
                    new Skill("Python", Proficiency.INTERMEDIATE, null))),
            new SkillCategory(
                "Infraestrutura", 1, List.of(new Skill("Docker", Proficiency.INTERMEDIATE, 2)))));
  }
}
