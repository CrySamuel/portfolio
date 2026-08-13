package dev.crystofer.portfolio.profile.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Linha de {@code skill}.
 *
 * <p>{@code proficiency} e String, e nao enum, pelo mesmo motivo de {@code
 * SocialLinkEntity.platform}: a coluna guarda o codigo em minusculo, que e o formato publicado, e
 * {@code @Enumerated(STRING)} exigiria que o banco guardasse o nome exato da constante Java.
 * Converter no mapper mantem a escolha de representacao no adaptador.
 *
 * <p>Nao ha referencia de volta para a categoria. A navegacao acontece num sentido so - categoria
 * para competencias -, que e como a leitura real funciona; um {@code @ManyToOne} aqui existiria
 * apenas para o Hibernate poder percorre-lo, e cada relacao bidirecional e mais um jeito de o
 * modelo ficar inconsistente em memoria.
 */
@Entity
@Table(name = "skill")
public class SkillEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String proficiency;

  /**
   * {@code Short} porque a coluna e {@code SMALLINT}, e nao {@code INTEGER}.
   *
   * <p>Nao e detalhe: o {@code ddl-auto: validate} recusa subir a aplicacao quando os dois
   * divergem, e foi assim que este campo foi corrigido - escrito primeiro como {@code Integer}, ele
   * derrubou o contexto com "found [int2], but expecting [integer]". A entidade espelha o banco; a
   * conversao para {@code Integer}, que e o tipo natural no dominio, e trabalho do mapper. Mesma
   * divisao que {@code SocialLinkEntity.displayOrder} ja fazia.
   *
   * <p>Nulavel, porque nem toda competencia tem numero honesto a declarar.
   */
  @Column(name = "years_of_experience")
  private Short yearsOfExperience;

  /** Exigido pelo Hibernate. Protegido para que ninguem o use por engano. */
  protected SkillEntity() {}

  /** Construtor completo, usado pelos testes do mapper. A aplicacao nunca constroi entidade. */
  public SkillEntity(Long id, String name, String proficiency, Short yearsOfExperience) {
    this.id = id;
    this.name = name;
    this.proficiency = proficiency;
    this.yearsOfExperience = yearsOfExperience;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getProficiency() {
    return proficiency;
  }

  public Short getYearsOfExperience() {
    return yearsOfExperience;
  }
}
