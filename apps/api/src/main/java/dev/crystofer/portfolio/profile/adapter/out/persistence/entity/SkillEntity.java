package dev.crystofer.portfolio.profile.adapter.out.persistence.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;

import dev.crystofer.portfolio.shared.domain.ContentLanguage;

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

  /**
   * O nome nos outros idiomas, quando ele muda - e quase nunca muda: Java e PostgreSQL se chamam
   * assim em qualquer idioma. {@code @BatchSize} pela mesma razao das traducoes da categoria.
   */
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "skill_translation", joinColumns = @JoinColumn(name = "skill_id"))
  @BatchSize(size = 64)
  private List<NameTranslation> translations = new ArrayList<>();

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

  public List<NameTranslation> getTranslations() {
    return translations;
  }

  /** Esta competencia com o nome no idioma pedido, ou ela mesma, quando o nome nao muda. */
  public SkillEntity traduzidoPara(ContentLanguage idioma) {
    if (idioma.isPadrao()) {
      return this;
    }

    return translations.stream()
        .filter(traducao -> traducao.getLocale().equals(idioma.tag()))
        .findFirst()
        .map(traducao -> new SkillEntity(id, traducao.getName(), proficiency, yearsOfExperience))
        .orElse(this);
  }
}
