package dev.crystofer.portfolio.profile.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Linha de {@code social_link}.
 *
 * <p>Entidade, e nao modelo. Ela existe para conversar com o banco e por isso carrega jakarta,
 * {@code id} e tipos de coluna - tudo que o dominio recusa. A traducao para {@link
 * dev.crystofer.portfolio.profile.domain.model.SocialLink} acontece no mapper, e e por isso que o
 * dominio nunca ve esta classe.
 *
 * <p>{@code platform} e String, nao enum. A coluna guarda minusculo ({@code "github"}), que e o
 * mesmo valor usado no front; {@code @Enumerated(STRING)} exigiria que o banco guardasse o nome
 * exato da constante Java. Converter no mapper mantem a escolha de representacao onde ela pertence
 * - no adaptador - em vez de deixar o formato do banco ditar o nome do enum de dominio.
 */
@Entity
@Table(name = "social_link")
public class SocialLinkEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String platform;

  @Column(nullable = false)
  private String url;

  @Column(name = "display_order", nullable = false)
  private short displayOrder;

  /** Exigido pelo Hibernate. Protegido para que ninguem o use por engano. */
  protected SocialLinkEntity() {}

  /**
   * Construtor completo, usado pelos testes do mapper.
   *
   * <p>A aplicacao nunca constroi entidade: este modulo nao escreve, o conteudo vem de migracao
   * (ADR-0004). Sem ele, testar a conversao exigiria reflection ou um banco no ar - e a conversao
   * de plataforma tem logica de verdade para exercitar.
   */
  public SocialLinkEntity(Long id, String platform, String url, short displayOrder) {
    this.id = id;
    this.platform = platform;
    this.url = url;
    this.displayOrder = displayOrder;
  }

  public Long getId() {
    return id;
  }

  public String getPlatform() {
    return platform;
  }

  public String getUrl() {
    return url;
  }

  public short getDisplayOrder() {
    return displayOrder;
  }
}
