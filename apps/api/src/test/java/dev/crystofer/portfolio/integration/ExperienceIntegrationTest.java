package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.crystofer.portfolio.profile.domain.model.Experience;
import dev.crystofer.portfolio.profile.domain.port.in.ListExperiencesUseCase;
import dev.crystofer.portfolio.profile.domain.port.out.LoadExperiencePort;

/**
 * O caminho do banco ate o dominio, com Postgres de verdade.
 *
 * <p>Existe porque duas promessas deste commit nao sao verificaveis em nenhuma outra camada.
 *
 * <p>A primeira e a leitura do {@code jsonb}. O {@code ddl-auto: validate} confere que a coluna
 * existe e tem o tipo esperado, e para por ai - ele nao le uma linha. O teste do mapper monta a
 * entidade a mao, entao tambem nao exercita o Hibernate convertendo {@code jsonb} em {@code
 * List<String>}. Sem este teste, um mapeamento errado passaria por toda a suite e falharia na
 * primeira leitura real.
 *
 * <p>A segunda e a ordenacao ponta a ponta, com a consulta de verdade no meio.
 *
 * <p>SQL cru para preparar o cenario, e nao os repositorios: montar o estado com o mesmo codigo que
 * esta sob teste torna o teste circular.
 */
class ExperienceIntegrationTest extends AbstractIntegrationTest {

  private static final String INSERIR =
      """
      INSERT INTO experience (company, role, start_date, end_date, description, highlights)
      VALUES (?, ?, ?::date, ?::date, ?, ?::jsonb)
      """;

  @Autowired LoadExperiencePort loadExperiencePort;

  @Autowired ListExperiencesUseCase listExperiencesUseCase;

  @AfterEach
  void limparATabela() {
    jdbcTemplate.update("DELETE FROM experience");
  }

  /**
   * A acentuacao vai junto de proposito.
   *
   * <p>O caminho do {@code jsonb} passa por driver, serializador e banco, e cada um deles tem uma
   * chance de trocar a codificacao. O perfil ja tem uma assercao assim pelo mesmo motivo.
   */
  @Test
  @DisplayName("deve ler os destaques do jsonb como lista, com acentuacao intacta")
  void shouldReadHighlightsFromJsonb_whenRowHasThem() {
    // given
    inserir(
        "Acme",
        "Dev Backend",
        "2022-03-01",
        null,
        "[\"Reduziu a latência em 40%\", \"Migrou o deploy para contêiner\"]");

    // when
    var experiences = loadExperiencePort.loadExperiences();

    // then
    assertThat(experiences).hasSize(1);
    assertThat(experiences.getFirst().highlights())
        .containsExactly("Reduziu a latência em 40%", "Migrou o deploy para contêiner");
  }

  @Test
  @DisplayName("deve ler destaques vazios como lista vazia, e nao como nulo")
  void shouldReadEmptyHighlightsAsEmptyList_whenColumnHasDefault() {
    // given
    jdbcTemplate.update(
        """
        INSERT INTO experience (company, role, start_date, description)
        VALUES ('Acme', 'Dev', '2022-03-01'::date, 'Descricao')
        """);

    // when
    var experiences = loadExperiencePort.loadExperiences();

    // then
    assertThat(experiences.getFirst().highlights()).isEmpty();
  }

  @Test
  @DisplayName("deve traduzir data de saida nula em cargo atual")
  void shouldReadNullEndDateAsCurrent_whenPositionIsOngoing() {
    // given
    inserir("Acme", "Dev", "2024-01-01", null, "[]");
    inserir("Antiga", "Dev", "2018-01-01", "2020-12-31", "[]");

    // when
    var timeline = listExperiencesUseCase.listExperiences();

    // then
    assertThat(timeline.findCurrent()).map(Experience::company).contains("Acme");
    assertThat(timeline.experiences().getLast().findEndDate()).contains(LocalDate.of(2020, 12, 31));
  }

  /**
   * A ordem correta com a consulta real no caminho.
   *
   * <p>As linhas entram fora de ordem, e a insercao no Postgres nao promete ordem de leitura. O que
   * este teste afirma e o resultado; <strong>qual das duas pecas o produz nao esta em disputa
   * aqui</strong>, e essa distincao e o que a secao 4.20 registrou: quem garante e o dominio, e o
   * {@code ORDER BY} do repositorio existe para o indice evitar o sort. Remover a clausula nao deve
   * mudar esta assercao - se mudar, a garantia esta no lugar errado.
   */
  @Test
  @DisplayName("deve devolver a timeline em ordem decrescente, com a consulta real no caminho")
  void shouldReturnOrderedTimeline_whenRowsAreInsertedOutOfOrder() {
    // given
    inserir("Meio", "Dev", "2021-06-01", "2023-05-31", "[]");
    inserir("Antiga", "Dev", "2018-01-01", "2020-12-31", "[]");
    inserir("Recente", "Dev", "2024-02-01", null, "[]");

    // when
    var timeline = listExperiencesUseCase.listExperiences();

    // then
    assertThat(timeline.experiences())
        .extracting(Experience::company)
        .containsExactly("Recente", "Meio", "Antiga");
  }

  @Test
  @DisplayName("deve devolver timeline vazia quando a tabela esta vazia")
  void shouldReturnEmptyTimeline_whenTableIsEmpty() {
    // when
    var timeline = listExperiencesUseCase.listExperiences();

    // then
    assertThat(timeline.isEmpty()).isTrue();
  }

  private void inserir(
      String company, String role, String start, String end, String highlightsJson) {
    jdbcTemplate.update(INSERIR, company, role, start, end, "Descricao", highlightsJson);
  }
}
