package dev.crystofer.portfolio.projects.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import dev.crystofer.portfolio.projects.adapter.out.persistence.entity.ProjectEntity;
import dev.crystofer.portfolio.projects.adapter.out.persistence.entity.ProjectMetricEntity;
import dev.crystofer.portfolio.projects.adapter.out.persistence.entity.TechnologyEntity;
import dev.crystofer.portfolio.projects.domain.model.Project;
import dev.crystofer.portfolio.projects.domain.model.Technology;
import dev.crystofer.portfolio.projects.domain.model.TechnologyCategory;
import dev.crystofer.portfolio.shared.domain.Slug;

class ProjectPersistenceMapperTest {

  private final ProjectPersistenceMapper mapper = Mappers.getMapper(ProjectPersistenceMapper.class);

  @Test
  @DisplayName("deve converter entidade completa em modelo de dominio")
  void shouldMapEverything_whenEntityIsComplete() {
    // given
    ProjectEntity entity = entidadeCompleta();

    // when
    Project projeto = mapper.toDomain(entity);

    // then
    assertThat(projeto.slug()).isEqualTo(Slug.of("finai"));
    assertThat(projeto.title()).isEqualTo("FinAI");
    assertThat(projeto.problem()).isEqualTo("O problema.");
    assertThat(projeto.solution()).isEqualTo("A solucao.");
    assertThat(projeto.outcome()).isEqualTo("O resultado.");
    assertThat(projeto.repoUrl()).isEqualTo("https://github.com/CrySamuel/FinAI-Bot");
    assertThat(projeto.liveUrl()).isEqualTo("https://t.me/gestor_crys_bot");
    assertThat(projeto.featured()).isTrue();
    assertThat(projeto.displayOrder()).isZero();
    assertThat(projeto.publishedAt()).isEqualTo(LocalDate.of(2026, 3, 24));
  }

  /**
   * O id da entidade nao atravessa para o dominio, e a ausencia e verificada.
   *
   * <p>Chave tecnica nao e informacao de negocio - a mesma razao pela qual ela tambem nao entra na
   * resposta da API. Se um dia alguem acrescentar um componente {@code id} ao record, o {@code
   * unmappedTargetPolicy} nao reclamaria, porque a entidade tem de onde preenche-lo; quem reprova e
   * a leitura deste teste.
   */
  @Test
  @DisplayName("deve deixar a chave tecnica fora do modelo de dominio")
  void shouldNotCarry_theTechnicalKey() {
    Project projeto = mapper.toDomain(entidadeCompleta());

    assertThat(Project.class.getRecordComponents())
        .extracting(java.lang.reflect.RecordComponent::getName)
        .doesNotContain("id");
    assertThat(projeto.slug().value()).isEqualTo("finai");
  }

  /** O texto da coluna vira value object, com a validacao de formato acontecendo na leitura. */
  @Test
  @DisplayName("deve envolver o slug da coluna no value object")
  void shouldWrapSlug_intoTheValueObject() {
    Technology tecnologia =
        mapper.toDomain(new TechnologyEntity(9L, "Spring Boot", "spring-boot", "framework", null));

    assertThat(tecnologia.slug()).isEqualTo(Slug.of("spring-boot"));
    assertThat(tecnologia.category()).isEqualTo(TechnologyCategory.FRAMEWORK);
  }

  /**
   * Slug invalido no banco morre na leitura, e nao vira URL quebrada.
   *
   * <p>O {@code CHECK} da V4 ja recusaria a escrita, entao este caminho so existe se alguem chegar
   * a coluna por fora. A mensagem cita o valor, que e o que permite achar a linha.
   */
  @Test
  @DisplayName("deve falhar alto quando o slug da coluna esta fora do formato")
  void shouldFailLoud_whenColumnSlugIsMalformed() {
    TechnologyEntity invalida =
        new TechnologyEntity(9L, "Spring Boot", "Spring Boot", "tool", null);

    assertThatThrownBy(() -> mapper.toDomain(invalida))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Spring Boot");
  }

  /**
   * Categoria desconhecida falha alto, em vez de virar nulo.
   *
   * <p>Devolver null ou pular a linha esconderia o problema, e a tela mostraria um chip a menos sem
   * nada em lugar nenhum dizendo por que.
   */
  @Test
  @DisplayName("deve falhar alto quando a categoria nao existe no enum")
  void shouldFailLoud_whenCategoryIsUnknown() {
    TechnologyEntity invalida = new TechnologyEntity(9L, "Java", "java", "linguagem", null);

    assertThatThrownBy(() -> mapper.toDomain(invalida))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Categoria desconhecida em technology.category: linguagem");
  }

  @Test
  @DisplayName("deve recusar categoria vazia na coluna")
  void shouldFailLoud_whenCategoryIsBlank() {
    TechnologyEntity invalida = new TechnologyEntity(9L, "Java", "java", "  ", null);

    assertThatThrownBy(() -> mapper.toDomain(invalida))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nao pode ser vazia");
  }

  /**
   * O {@code SMALLINT} da coluna vira {@code int} no dominio.
   *
   * <p>A entidade cede ao banco porque o {@code ddl-auto: validate} reprova o contrario; o dominio
   * usa o tipo natural. A conversao acontece aqui, e e o mapper que absorve a diferenca.
   */
  @Test
  @DisplayName("deve converter a ordem de SMALLINT para int")
  void shouldWiden_theDisplayOrder() {
    ProjectMetricEntity entity = new ProjectMetricEntity(3L, "p95", "80ms", (short) 7);

    assertThat(mapper.toDomain(entity).displayOrder()).isEqualTo(7);
  }

  @Test
  @DisplayName("deve preservar as colunas nulaveis como nulas")
  void shouldKeepNullable_asNull() {
    ProjectEntity entity =
        new ProjectEntity(
            1L,
            "interno",
            "Projeto interno",
            "Resumo.",
            "O problema.",
            "A solucao.",
            "O resultado.",
            null,
            null,
            null,
            false,
            (short) 0,
            null,
            List.of(),
            List.of());

    Project projeto = mapper.toDomain(entity);

    assertThat(projeto.repoUrl()).isNull();
    assertThat(projeto.liveUrl()).isNull();
    assertThat(projeto.coverImage()).isNull();
    assertThat(projeto.publishedAt()).isNull();
    assertThat(projeto.findRepoUrl()).isEmpty();
  }

  /**
   * O mapeamento atravessa as duas colecoes, e a ordem do dominio se aplica depois dele.
   *
   * <p>As entidades entram fora de ordem de proposito: quem ordena e o record, na construcao, e nao
   * o {@code @OrderBy} do SQL - a mesma separacao que ja custou uma suposicao errada para ser
   * estabelecida.
   */
  @Test
  @DisplayName("deve mapear as duas colecoes e deixar o dominio ordenar")
  void shouldMapBothCollections_andLetTheDomainOrder() {
    Project projeto = mapper.toDomain(entidadeCompleta());

    assertThat(projeto.technologies())
        .extracting(Technology::name)
        .containsExactly("Oracle Cloud", "Python", "SQLAlchemy");
    assertThat(projeto.metrics())
        .extracting(dev.crystofer.portfolio.projects.domain.model.ProjectMetric::label)
        .containsExactly("Economia em um mes", "Investimento mensal");
  }

  private static ProjectEntity entidadeCompleta() {
    return new ProjectEntity(
        1L,
        "finai",
        "FinAI",
        "Assistente financeiro no Telegram.",
        "O problema.",
        "A solucao.",
        "O resultado.",
        "https://github.com/CrySamuel/FinAI-Bot",
        "https://t.me/gestor_crys_bot",
        "/images/finai.png",
        true,
        (short) 0,
        LocalDate.of(2026, 3, 24),
        List.of(
            new TechnologyEntity(2L, "SQLAlchemy", "sqlalchemy", "framework", null),
            new TechnologyEntity(1L, "Python", "python", "language", null),
            new TechnologyEntity(3L, "Oracle Cloud", "oracle-cloud", "infrastructure", null)),
        List.of(
            new ProjectMetricEntity(2L, "Investimento mensal", "R$ 200+", (short) 1),
            new ProjectMetricEntity(1L, "Economia em um mes", "R$ 800+", (short) 0)));
  }
}
