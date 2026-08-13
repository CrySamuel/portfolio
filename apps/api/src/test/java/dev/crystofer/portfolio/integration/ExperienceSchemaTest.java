package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import dev.crystofer.portfolio.support.fixtures.ExperienceFixtures;

/**
 * As promessas da {@code V2__create_experience_table} contra o Postgres 16 de verdade.
 *
 * <p>Existe porque a migracao afirma coisas que nenhum outro teste alcanca. Que o Flyway aplica o
 * arquivo, isso a suite inteira ja prova por consequencia - o contexto nao sobe se ele falhar. O
 * que fica sem guarda sao as restricoes: elas so se manifestam quando alguem tenta grava-las
 * erradas, e ate o commit 25 nao existe entidade nem adaptador que tente.
 *
 * <p>Sem este teste, remover um {@code CHECK} da migracao deixaria a suite inteira verde. Seria a
 * quarta guarda muda deste projeto, e a mais silenciosa delas: o defeito so apareceria como dado
 * impossivel no banco de producao, meses depois, sem nada apontando para a causa.
 *
 * <p><strong>Cada assercao exige o nome da restricao</strong>, e nao apenas que a escrita tenha
 * falhado. Um {@code INSERT} pode ser recusado por muitos motivos - coluna {@code NOT NULL}
 * esquecida, tipo incompativel, erro de digitacao no proprio teste - e um teste que aceita qualquer
 * falha como prova passa pelo motivo errado. Ja aconteceu aqui, com uma assercao que dava por
 * ausente uma chave que vinha nula.
 *
 * <p>SQL cru, e nao repositorio: o objeto sob teste e o schema, entao qualquer camada de mapeamento
 * no meio so acrescentaria um jeito de o teste mentir.
 */
class ExperienceSchemaTest extends AbstractIntegrationTest {

  private static final String INSERIR =
      """
      INSERT INTO experience (company, role, start_date, end_date, description, highlights)
      VALUES (?, ?, ?::date, ?::date, ?, COALESCE(?::jsonb, '[]'::jsonb))
      """;

  @Autowired DataSource dataSource;

  /**
   * Tabela vazia antes, seed de producao depois.
   *
   * <p>O {@code @BeforeEach} nao era necessario enquanto a timeline nao tinha conteudo. Passou a
   * ser quando o {@code R__seed_experience} entrou: o Flyway o aplica no container, entao a tabela
   * chega a este teste com as duas posicoes reais, e as contagens abaixo passariam a medir o seed
   * em vez das linhas que o proprio teste escreve.
   */
  @BeforeEach
  void esvaziarATabela() {
    ExperienceFixtures.empty(jdbcTemplate);
  }

  @AfterEach
  void devolverOBancoAoSeed() {
    ExperienceFixtures.reapplySeed(dataSource);
  }

  @Test
  @DisplayName("deve aceitar cargo atual, com data de saida nula")
  void shouldAcceptExperience_whenEndDateIsNull() {
    // when
    var thrown = catchThrowable(() -> inserir("Acme", "Dev Backend", "2024-01-01", null, null));

    // then
    assertThat(thrown).isNull();
    assertThat(contar()).isEqualTo(1);
  }

  /**
   * O limite do {@code CHECK}, e nao apenas um caso claramente valido.
   *
   * <p>A restricao e {@code >=}. Escrita como {@code >} por engano, ela recusaria quem entrou e
   * saiu no mesmo dia - e nenhum teste com datas distantes notaria.
   */
  @Test
  @DisplayName("deve aceitar periodo que comeca e termina no mesmo dia")
  void shouldAcceptPeriod_whenEndDateEqualsStartDate() {
    // when
    var thrown = catchThrowable(() -> inserir("Acme", "Estagio", "2023-01-01", "2023-01-01", null));

    // then
    assertThat(thrown).isNull();
  }

  @Test
  @DisplayName("deve recusar periodo que termina antes de comecar")
  void shouldRejectPeriod_whenEndDateIsBeforeStartDate() {
    // when
    var thrown = catchThrowable(() -> inserir("Acme", "Dev", "2022-01-01", "2021-12-31", null));

    // then
    assertThat(thrown).hasMessageContaining("experience_period_ck");
  }

  /**
   * O tipo do {@code jsonb} nao e o tipo da coluna.
   *
   * <p>{@code jsonb} aceita qualquer JSON valido: objeto, texto solto e numero passam pela coluna
   * sem reclamacao. A promessa de que ali mora uma lista e do {@code CHECK}, e e ela que este caso
   * exercita nas tres formas que um erro de escrita produziria.
   */
  @ParameterizedTest
  @DisplayName("deve recusar highlights que nao seja um array json")
  @ValueSource(strings = {"{}", "{\"a\": 1}", "\"texto\"", "42", "true", "null"})
  void shouldRejectHighlights_whenValueIsNotAJsonArray(String naoEArray) {
    // when
    var thrown = catchThrowable(() -> inserir("Acme", "Dev", "2022-01-01", null, naoEArray));

    // then
    assertThat(thrown).hasMessageContaining("experience_highlights_is_array_ck");
  }

  @Test
  @DisplayName("deve aceitar highlights quando for um array json")
  void shouldAcceptHighlights_whenValueIsAJsonArray() {
    // when
    var thrown =
        catchThrowable(() -> inserir("Acme", "Dev", "2022-01-01", null, "[\"um\", \"dois\"]"));

    // then
    assertThat(thrown).isNull();
  }

  /**
   * O default existe para que nao haja dois jeitos de dizer "sem destaques".
   *
   * <p>Se ele sumir, a coluna e {@code NOT NULL} e o {@code INSERT} sem highlights passa a falhar -
   * mas o teste afirma o valor, e nao apenas que a escrita funcionou, porque um default trocado
   * para {@code '{}'} continuaria aceitando a escrita e quebraria o mapeamento la na frente.
   */
  @Test
  @DisplayName("deve preencher highlights com array vazio quando nao informado")
  void shouldDefaultHighlightsToEmptyArray_whenNotProvided() {
    // given
    jdbcTemplate.update(
        """
        INSERT INTO experience (company, role, start_date, description)
        VALUES ('Acme', 'Dev', '2022-01-01'::date, 'descricao')
        """);

    // when
    var highlights =
        jdbcTemplate.queryForObject("SELECT highlights::text FROM experience", String.class);

    // then
    assertThat(highlights).isEqualTo("[]");
  }

  @Test
  @DisplayName("deve recusar a mesma passagem duas vezes")
  void shouldRejectPassage_whenCompanyRoleAndStartDateRepeat() {
    // given
    inserir("Acme", "Dev Backend", "2024-01-01", null, null);

    // when
    var thrown = catchThrowable(() -> inserir("Acme", "Dev Backend", "2024-01-01", null, null));

    // then
    assertThat(thrown).hasMessageContaining("experience_company_role_start_uk");
  }

  /**
   * A chave natural e uma tripla, e este caso e o que impede de encolhe-la.
   *
   * <p>Voltar a mesma empresa no mesmo cargo e comum, e uma chave por {@code (company, role)}
   * recusaria o retorno. Sem este teste, "simplificar" a restricao passaria despercebido ate alguem
   * com essa historia tentar publica-la.
   */
  @Test
  @DisplayName("deve aceitar o retorno a mesma empresa e cargo em outra data")
  void shouldAcceptPassage_whenSameCompanyAndRoleStartOnAnotherDate() {
    // given
    inserir("Acme", "Dev Backend", "2020-05-01", "2022-04-30", null);

    // when
    var thrown = catchThrowable(() -> inserir("Acme", "Dev Backend", "2024-01-01", null, null));

    // then
    assertThat(thrown).isNull();
    assertThat(contar()).isEqualTo(2);
  }

  /**
   * A coluna e {@code GENERATED ALWAYS}, e nao {@code BY DEFAULT}.
   *
   * <p>E o que garante que nenhum seed futuro fixe ids a mao - eles sao referenciados por consulta,
   * como o {@code R__seed_profile} ja faz. Trocado por {@code BY DEFAULT}, o banco aceitaria o id
   * explicito e a sequence ficaria para tras, entregando chave duplicada na primeira insercao
   * normal seguinte.
   *
   * <p>Duas particularidades de diagnostico, e as duas custaram uma tentativa cada. O texto que o
   * Postgres emite aqui e {@code cannot insert a non-DEFAULT value into column "id"}; o {@code
   * GENERATED ALWAYS} aparece so no {@code DETAIL}, que o driver nao propaga na mensagem. E este
   * caso e traduzido pelo Spring como {@code BadSqlGrammarException}, cuja mensagem contem apenas o
   * SQL - diferente do {@code DataIntegrityViolationException} das restricoes, que embute o texto
   * do banco. Por isso a assercao desce ate a causa raiz em vez de confiar no involucro.
   */
  @Test
  @DisplayName("deve recusar id explicito, porque a identidade e sempre gerada")
  void shouldRejectExplicitId_whenIdentityIsGeneratedAlways() {
    // when
    var thrown =
        catchThrowable(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO experience (id, company, role, start_date, description)
                    VALUES (99, 'Acme', 'Dev', '2022-01-01'::date, 'descricao')
                    """));

    // then
    assertThat(thrown)
        .rootCause()
        .hasMessageContaining("cannot insert a non-DEFAULT value into column");
  }

  private void inserir(
      String company, String role, String startDate, String endDate, String highlights) {
    jdbcTemplate.update(INSERIR, company, role, startDate, endDate, "descricao", highlights);
  }

  private Integer contar() {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM experience", Integer.class);
  }
}
