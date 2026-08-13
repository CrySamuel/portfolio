package dev.crystofer.portfolio.profile.adapter.out.persistence.entity;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Linha de {@code experience}.
 *
 * <p>Entidade, e nao modelo, pela mesma razao de {@link SocialLinkEntity}: ela carrega jakarta,
 * {@code id} e tipos de coluna, tudo que o dominio recusa. A traducao para {@link
 * dev.crystofer.portfolio.profile.domain.model.Experience} acontece no mapper.
 *
 * <p><strong>Nao ha ordenacao declarada aqui.</strong> Quem ordena a timeline e o {@code Timeline}
 * do dominio, e a consulta do repositorio pede {@code ORDER BY} apenas para que o planejador use o
 * indice e evite o sort. A distincao ja foi medida uma vez neste projeto, no modulo de perfil, e o
 * resultado desmentiu a suposicao de que a anotacao e que garantia a ordem.
 */
@Entity
@Table(name = "experience")
public class ExperienceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String company;

  /**
   * {@code role} e palavra-chave nao reservada no PostgreSQL, entao o Hibernate a emite sem aspas e
   * sem conflito. O nome vem da secao 3.7 do plano e da coluna correspondente.
   */
  @Column(nullable = false)
  private String role;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  /** Nulo significa cargo atual - a mesma leitura da coluna e do dominio. */
  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(nullable = false)
  private String description;

  /**
   * A coluna e {@code jsonb} e o campo e uma lista, com o Hibernate fazendo a ponte.
   *
   * <p>{@code SqlTypes.JSON} e o que o dialeto do Postgres traduz para {@code jsonb}. A alternativa
   * seria guardar {@code String} aqui e desserializar no mapper, o que exigiria um parser injetado
   * numa interface do MapStruct - mais peca, mesmo resultado.
   *
   * <p>O {@code CHECK} da migracao continua necessario apesar deste mapeamento: ele protege a
   * coluna de escrita que nao passe por aqui, e toda escrita deste projeto e assim, porque conteudo
   * entra por migracao.
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private List<String> highlights;

  /** Exigido pelo Hibernate. Protegido para que ninguem o use por engano. */
  protected ExperienceEntity() {}

  /**
   * Construtor completo, usado pelos testes do mapper.
   *
   * <p>A aplicacao nunca constroi entidade: este modulo nao escreve, o conteudo vem de migracao
   * (ADR-0004).
   */
  public ExperienceEntity(
      Long id,
      String company,
      String role,
      LocalDate startDate,
      LocalDate endDate,
      String description,
      List<String> highlights) {
    this.id = id;
    this.company = company;
    this.role = role;
    this.startDate = startDate;
    this.endDate = endDate;
    this.description = description;
    this.highlights = highlights;
  }

  public Long getId() {
    return id;
  }

  public String getCompany() {
    return company;
  }

  public String getRole() {
    return role;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getHighlights() {
    return highlights;
  }
}
