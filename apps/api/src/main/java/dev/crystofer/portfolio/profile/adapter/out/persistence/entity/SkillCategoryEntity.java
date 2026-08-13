package dev.crystofer.portfolio.profile.adapter.out.persistence.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Linha de {@code skill_category}, com as competencias associadas.
 *
 * <p>Relacao unidirecional, como em {@link ProfileEntity}: {@link SkillEntity} nao tem referencia
 * de volta, porque bidirecional serve para sincronizar os dois lados durante a escrita e aqui nao
 * ha escrita.
 *
 * <p>{@code created_at} e {@code updated_at} ficam fora do mapeamento de proposito. O {@code
 * ddl-auto: validate} nao reclama de coluna que existe no banco e nao na entidade - so do
 * contrario.
 */
@Entity
@Table(name = "skill_category")
public class SkillCategoryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(name = "display_order", nullable = false)
  private short displayOrder;

  /**
   * LAZY, e resolvida por {@code @EntityGraph} no repositorio.
   *
   * <p>Aqui o N+1 deixa de ser hipotetico: sao varias categorias, e sem o grafo cada uma dispararia
   * a propria consulta de competencias. E a situacao que o modulo de perfil nao tinha - la a tabela
   * tem uma linha - e por isso o habito foi estabelecido antes, quando o erro ainda era barato.
   *
   * <p><strong>O {@code @OrderBy} ordena so por nome, e a ausencia do nivel e deliberada.</strong>
   * Em {@link ProfileEntity} a anotacao repete a ordem do dominio, e o SQL conta a mesma historia
   * que o modelo. Aqui isso seria impossivel: a coluna guarda o codigo em texto, e a ordem
   * alfabetica dele - {@code advanced}, {@code basic}, {@code intermediate} - nao tem relacao com a
   * escala de dominio. Um {@code proficiency DESC} produziria intermediate, basic, advanced, ou
   * seja, exatamente a ordem errada.
   *
   * <p>Como quem ordena e {@code SkillCategory}, a resposta sairia certa mesmo assim - e e por isso
   * que a anotacao errada seria perigosa: ela nao quebraria nada, so mentiria para quem lesse o log
   * de consultas tentando entender a ordem da tela.
   */
  @OneToMany(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  @OrderBy("name ASC")
  private List<SkillEntity> skills = new ArrayList<>();

  /** Exigido pelo Hibernate. Protegido para que ninguem o use por engano. */
  protected SkillCategoryEntity() {}

  /** Construtor completo, usado pelos testes do mapper. A aplicacao nunca constroi entidade. */
  public SkillCategoryEntity(Long id, String name, short displayOrder, List<SkillEntity> skills) {
    this.id = id;
    this.name = name;
    this.displayOrder = displayOrder;
    this.skills = skills;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public short getDisplayOrder() {
    return displayOrder;
  }

  public List<SkillEntity> getSkills() {
    return skills;
  }
}
