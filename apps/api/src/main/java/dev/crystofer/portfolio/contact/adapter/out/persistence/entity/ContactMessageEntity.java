package dev.crystofer.portfolio.contact.adapter.out.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.crystofer.portfolio.contact.domain.model.EmailStatus;

/**
 * Linha de {@code contact_message}.
 *
 * <p><strong>A primeira entidade deste sistema que existe para escrever.</strong> As outras sao
 * lidas e nunca alteradas, porque o conteudo delas entra por migracao; esta e inserida a cada
 * mensagem e atualizada quando o envio termina.
 */
@Entity
@Table(name = "contact_message")
public class ContactMessageEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String subject;

  @Column(nullable = false)
  private String message;

  /**
   * {@code @JdbcTypeCode(CHAR)}, e nao {@code columnDefinition}.
   *
   * <p>A coluna e {@code CHAR(64)} e o {@code ddl-auto: validate} confere o <strong>codigo JDBC do
   * tipo</strong>, e nao o texto da definicao. Com {@code columnDefinition = "char(64)"} o
   * Hibernate segue tratando o campo como {@code Types#VARCHAR} e a validacao recusa o contexto
   * inteiro com "found [bpchar (Types#CHAR)], but expecting [char(64) (Types#VARCHAR)]" - mensagem
   * que parece dizer o contrario do que diz.
   *
   * <p>E a terceira vez que o {@code validate} cobra a entidade por espelhar o banco: foi {@code
   * SMALLINT} no commit 30, e e {@code CHAR} agora. Quem cede e sempre a entidade.
   */
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "ip_hash", length = 64)
  private String ipHash;

  @Column(name = "user_agent")
  private String userAgent;

  /**
   * {@code EnumType.STRING}, e a alternativa e um defeito conhecido.
   *
   * <p>{@code ORDINAL} grava a posicao do valor no enum. Inserir um estado novo no meio da
   * declaracao - ou reordenar - reinterpretaria em silencio todas as linhas ja gravadas: o que era
   * SENT viraria FAILED sem nenhuma escrita acontecer. Com STRING o nome vai para a coluna, que e
   * tambem o que o {@code CHECK} da V5 espera ler.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "email_status", nullable = false)
  private EmailStatus emailStatus;

  /**
   * Preenchidos pelo banco, e nao pela aplicacao.
   *
   * <p>{@code insertable = false} e {@code updatable = false} entregam as duas colunas ao {@code
   * DEFAULT now()} da V5. Deixar a aplicacao escrever a data introduziria o relogio da instancia
   * como fonte de verdade - e com duas instancias, ou com uma que acorda de hibernacao com o
   * relogio dessincronizado, a ordem das mensagens deixaria de ser confiavel.
   */
  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private Instant updatedAt;

  /** Exigido pelo Hibernate. Protegido para que ninguem o use por engano. */
  protected ContactMessageEntity() {}

  public ContactMessageEntity(
      String name,
      String email,
      String subject,
      String message,
      String ipHash,
      String userAgent,
      EmailStatus emailStatus) {
    this.name = name;
    this.email = email;
    this.subject = subject;
    this.message = message;
    this.ipHash = ipHash;
    this.userAgent = userAgent;
    this.emailStatus = emailStatus;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getSubject() {
    return subject;
  }

  public String getMessage() {
    return message;
  }

  public String getIpHash() {
    return ipHash;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public EmailStatus getEmailStatus() {
    return emailStatus;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
