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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import dev.crystofer.portfolio.shared.domain.ContentLanguage;

/**
 * Linha de {@code profile}, com os links associados.
 *
 * <p>Relacao unidirecional: {@link SocialLinkEntity} nao tem referencia de volta. Bidirecional
 * existe para manter os dois lados sincronizados durante a escrita, e aqui nao ha escrita - o
 * conteudo entra por migracao. O lado inverso seria peso morto que ainda por cima convida a
 * modificar a colecao.
 *
 * <p>A coluna {@code singleton} da V1 nao esta mapeada de proposito. Ela e tecnica: existe para a
 * constraint que trava a tabela em uma linha, e o dominio nao tem o que fazer com ela. O {@code
 * ddl-auto: validate} nao reclama de coluna que existe no banco e nao na entidade - so do
 * contrario. As colunas {@code created_at} e {@code updated_at} ficam de fora pela mesma razao.
 */
@Entity
@Table(name = "profile")
public class ProfileEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(nullable = false)
  private String headline;

  @Column(nullable = false)
  private String bio;

  @Column private String location;

  @Column(name = "resume_url")
  private String resumeUrl;

  @Column(name = "available_for_work", nullable = false)
  private boolean availableForWork;

  /**
   * LAZY, e nao EAGER. EAGER traria os links em toda consulta que tocasse o perfil, inclusive nas
   * que nao os usam - e o pior: seria uma segunda ida ao banco, invisivel no codigo. Quem precisa
   * dos links pede explicitamente, e o {@code @EntityGraph} do repositorio resolve tudo em uma
   * consulta so.
   *
   * <p>O {@code @OrderBy} e do JPA e vira ORDER BY no SQL. O dominio reordena de qualquer forma, na
   * construcao de {@code Profile} - a redundancia e barata e faz o SQL contar a mesma historia que
   * o modelo, para quem estiver lendo o log de consultas.
   */
  @OneToMany(fetch = FetchType.LAZY)
  @JoinColumn(name = "profile_id")
  @OrderBy("displayOrder ASC")
  private List<SocialLinkEntity> socialLinks = new ArrayList<>();

  /**
   * O texto do perfil nos outros idiomas (ADR-0013).
   *
   * <p>LAZY, e e isso que mantem o caminho do portugues exatamente como era: a colecao so e lida
   * quando {@link #traduzidoPara} precisa dela, e no idioma padrao ela nunca e tocada - nenhuma
   * consulta a mais.
   */
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "profile_translation", joinColumns = @JoinColumn(name = "profile_id"))
  private List<ProfileTranslation> translations = new ArrayList<>();

  /** Exigido pelo Hibernate. Protegido para que ninguem o use por engano. */
  protected ProfileEntity() {}

  /** Construtor completo, usado pelos testes do mapper. Ver {@link SocialLinkEntity}. */
  public ProfileEntity(
      Long id,
      String fullName,
      String headline,
      String bio,
      String location,
      String resumeUrl,
      boolean availableForWork,
      List<SocialLinkEntity> socialLinks) {
    this.id = id;
    this.fullName = fullName;
    this.headline = headline;
    this.bio = bio;
    this.location = location;
    this.resumeUrl = resumeUrl;
    this.availableForWork = availableForWork;
    this.socialLinks = socialLinks;
  }

  public Long getId() {
    return id;
  }

  public String getFullName() {
    return fullName;
  }

  public String getHeadline() {
    return headline;
  }

  public String getBio() {
    return bio;
  }

  public String getLocation() {
    return location;
  }

  public String getResumeUrl() {
    return resumeUrl;
  }

  public boolean isAvailableForWork() {
    return availableForWork;
  }

  public List<SocialLinkEntity> getSocialLinks() {
    return socialLinks;
  }

  public List<ProfileTranslation> getTranslations() {
    return translations;
  }

  /**
   * Este perfil com o texto no idioma pedido, ou ele mesmo.
   *
   * <p><strong>A traducao acontece aqui, antes do mapper, e e por isso que o mapper e o dominio nao
   * mudaram.</strong> O que sai e uma copia com o texto trocado - nunca gerenciada pelo Hibernate,
   * nunca gravada, valida so durante a conversao para o dominio. O MapStruct converte a copia como
   * converteria o original, e o {@code Profile} que nasce dela passa pelas mesmas invariantes: um
   * cargo traduzido em branco e recusado igual ao original em branco.
   *
   * <p>A reserva e por linha: sem traducao para o idioma, volta o proprio perfil, em portugues. O
   * local e a excecao por campo, porque e nulavel nos dois lados: traducao sem local fica com o do
   * original.
   */
  public ProfileEntity traduzidoPara(ContentLanguage idioma) {
    if (idioma.isPadrao()) {
      return this;
    }

    return translations.stream()
        .filter(traducao -> traducao.getLocale().equals(idioma.tag()))
        .findFirst()
        .map(
            traducao ->
                new ProfileEntity(
                    id,
                    fullName,
                    traducao.getHeadline(),
                    traducao.getBio(),
                    traducao.getLocation() == null ? location : traducao.getLocation(),
                    resumeUrl,
                    availableForWork,
                    socialLinks))
        .orElse(this);
  }
}
