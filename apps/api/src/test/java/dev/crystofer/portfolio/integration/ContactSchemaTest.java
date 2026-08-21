package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * As promessas da {@code V5__create_contact_message_table} contra o Postgres 16 de verdade.
 *
 * <p>Existe pela mesma razao dos outros testes de schema: ate o commit 46 nao ha entidade nem
 * adaptador que tente gravar errado, entao remover um {@code CHECK} daqui deixaria a suite inteira
 * verde e o defeito so apareceria como dado impossivel em producao.
 *
 * <p><strong>Esta tabela pesa mais que as outras, e por um motivo de fronteira.</strong> Todas as
 * anteriores recebem conteudo por migracao, escrito por quem conhece o schema. Esta recebe escrita
 * <em>da internet</em>, entao cada restricao aqui e a ultima linha que continua valendo quando a
 * validacao da aplicacao falha, e nao apenas uma segunda opiniao sobre ela.
 *
 * <p>Cada assercao exige o nome da restricao. Um {@code INSERT} pode ser recusado por muitos
 * motivos, e teste que aceita qualquer falha como prova passa pelo motivo errado.
 *
 * <p>Nao ha {@code @BeforeEach} esvaziando a tabela porque nao ha seed para ela - o {@code
 * contact_message} nasce vazio e so recebe o que estes testes escrevem. O {@code @AfterEach} limpa
 * para que a contagem de um teste nao dependa de quem rodou antes.
 */
class ContactSchemaTest extends AbstractIntegrationTest {

  private static final String INSERIR =
      """
      INSERT INTO contact_message (name, email, subject, message)
      VALUES (?, ?, ?, ?)
      """;

  @AfterEach
  void limparAsMensagens() {
    jdbcTemplate.update("DELETE FROM contact_message");
  }

  /**
   * O caminho feliz, e os tres valores que a tabela preenche sozinha.
   *
   * <p>Os defaults sao a parte que importa: o {@code PENDING} nao e escolha do codigo que insere, e
   * sim do schema. E o que garante que uma mensagem gravada por qualquer caminho - inclusive um
   * {@code INSERT} manual numa madrugada de incidente - entre na fila de envio em vez de ficar
   * parada num estado que ninguem processa.
   */
  @Test
  @DisplayName("deve aceitar a mensagem e nascer PENDING, com as duas datas preenchidas")
  void shouldInsert_withPendingDefault() {
    jdbcTemplate.update(INSERIR, "Fulana", "fulana@exemplo.com", "Vaga backend", "Ola!");

    var linha =
        jdbcTemplate.queryForMap(
            "SELECT email_status, ip_hash, user_agent, created_at, updated_at FROM contact_message");

    assertThat(linha.get("email_status")).isEqualTo("PENDING");
    assertThat(linha.get("created_at")).isNotNull();
    assertThat(linha.get("updated_at")).isNotNull();

    // Os dois nulaveis chegam nulos sem que ninguem os mencione no INSERT, que e
    // o caso de quem grava sem conseguir determinar a origem.
    assertThat(linha.get("ip_hash")).isNull();
    assertThat(linha.get("user_agent")).isNull();
  }

  /**
   * {@code NOT NULL} nao impede string vazia, e este e o teste que prova que alguem se lembrou.
   *
   * <p>Sao quatro colunas obrigatorias e quatro restricoes separadas, e nao uma so: com uma
   * restricao unica cobrindo as quatro, a mensagem de erro nao diria qual campo veio vazio - e essa
   * informacao e justamente a que o tratador de erro precisa para apontar o campo certo na tela.
   */
  @ParameterizedTest(name = "{0} vazio reprova em {4}")
  @CsvSource({
    "name,     '',       fulana@exemplo.com, Assunto, contact_message_name_ck",
    "email,    Fulana,   '',                 Assunto, contact_message_email_ck",
    "subject,  Fulana,   fulana@exemplo.com, '',      contact_message_subject_ck",
  })
  void shouldReject_whenRequiredTextIsBlank(
      String coluna, String nome, String email, String assunto, String restricao) {
    Throwable falha =
        catchThrowable(() -> jdbcTemplate.update(INSERIR, nome, email, assunto, "Ola!"));

    assertThat(falha).hasMessageContaining(restricao);
  }

  /**
   * Espaco em branco tambem e vazio, e e por isso que as restricoes usam {@code btrim}.
   *
   * <p>Sem ele, um campo com tres espacos passaria: nao e nulo, nao e string vazia, e chega na
   * caixa de entrada como uma mensagem de remetente sem nome. E o caso que uma validacao ingenua
   * deixa passar e que um robo de formulario encontra sozinho.
   */
  @Test
  @DisplayName("deve recusar nome feito so de espacos")
  void shouldReject_whenNameIsOnlyWhitespace() {
    Throwable falha =
        catchThrowable(
            () -> jdbcTemplate.update(INSERIR, "   ", "fulana@exemplo.com", "Assunto", "Ola!"));

    assertThat(falha).hasMessageContaining("contact_message_name_ck");
  }

  /**
   * Os dois extremos da mesma restricao.
   *
   * <p>O teto existe porque o limite anunciado ao visitante e do formulario, e formulario vive no
   * navegador - qualquer um desliga o JavaScript e manda o que quiser. O piso existe porque uma
   * mensagem vazia ocupa uma linha, dispara um e-mail e nao diz nada.
   */
  @Test
  @DisplayName("deve recusar mensagem vazia e mensagem acima de 5000 caracteres")
  void shouldReject_whenMessageIsOutOfBounds() {
    Throwable vazia =
        catchThrowable(
            () -> jdbcTemplate.update(INSERIR, "Fulana", "fulana@exemplo.com", "Assunto", "   "));

    Throwable longa =
        catchThrowable(
            () ->
                jdbcTemplate.update(
                    INSERIR, "Fulana", "fulana@exemplo.com", "Assunto", "x".repeat(5_001)));

    assertThat(vazia).hasMessageContaining("contact_message_message_ck");
    assertThat(longa).hasMessageContaining("contact_message_message_ck");
  }

  /** Exatamente 5000 passa - o limite e inclusivo, e um teste de fronteira que so erra por um. */
  @Test
  @DisplayName("deve aceitar mensagem com exatamente 5000 caracteres")
  void shouldAccept_whenMessageIsAtTheLimit() {
    jdbcTemplate.update(INSERIR, "Fulana", "fulana@exemplo.com", "Assunto", "x".repeat(5_000));

    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM contact_message", Integer.class))
        .isEqualTo(1);
  }

  /**
   * O estado de entrega e fechado, e a lista mora no schema.
   *
   * <p>Sem o {@code CHECK}, um erro de digitacao no codigo de envio gravaria {@code 'SENDT'} em
   * silencio: a mensagem contaria como processada, nunca seria reenviada e nunca chegaria. O
   * defeito apareceria como ausencia, que e o mais caro de perceber.
   */
  @Test
  @DisplayName("deve recusar estado de entrega fora dos tres previstos")
  void shouldReject_whenEmailStatusIsUnknown() {
    Throwable falha =
        catchThrowable(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO contact_message (name, email, subject, message, email_status)
                    VALUES ('Fulana', 'fulana@exemplo.com', 'Assunto', 'Ola!', 'SENDT')
                    """));

    assertThat(falha).hasMessageContaining("contact_message_email_status_ck");
  }

  /**
   * O indice do reprocessamento existe <strong>e e parcial</strong>.
   *
   * <p>Conferir so a existencia deixaria passar a versao ingenua - indice sobre a coluna inteira -,
   * que funciona igual e custa escrita em toda linha para indexar o valor que a consulta nunca
   * procura. O que distingue os dois e a clausula {@code WHERE} na definicao, e e ela que este
   * teste exige.
   */
  @Test
  @DisplayName("deve ter indice parcial apenas sobre as mensagens FAILED")
  void shouldHavePartialIndex_forFailedMessages() {
    String definicao =
        jdbcTemplate.queryForObject(
            "SELECT indexdef FROM pg_indexes WHERE indexname = 'contact_message_failed_idx'",
            String.class);

    assertThat(definicao).contains("WHERE").contains("FAILED");
  }
}
