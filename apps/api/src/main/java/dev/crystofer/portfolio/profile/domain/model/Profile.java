package dev.crystofer.portfolio.profile.domain.model;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * O perfil publico do portfolio.
 *
 * <p>Record, e nao classe com setters: o perfil e um valor lido, nunca alterado em memoria. Trocar
 * o conteudo e escrever uma migracao, nao chamar um setter (ADR-0004).
 *
 * <p>Nenhuma importacao de framework aqui, nem de {@code org.springframework}, nem de {@code
 * jakarta.persistence}, nem de adaptador. Isso nao e uma convencao escrita no README - vira teste
 * de ArchUnit que quebra o build no commit 20.
 *
 * <p>Sobre a quantidade de componentes: a secao 13.1 limita funcoes a tres parametros. A regra mira
 * comportamento, onde muitos parametros escondem responsabilidades demais. O construtor canonico de
 * um record e o dado em si, e agrupa-lo em objetos artificiais so para contar menos argumentos
 * tornaria a leitura pior sem separar responsabilidade alguma.
 *
 * @param fullName nome completo
 * @param headline uma linha, a proposta de valor
 * @param bio texto de apresentacao
 * @param location cidade ou regiao; {@code null} quando nao informada
 * @param resumeUrl endereco do curriculo; {@code null} quando nao ha
 * @param availableForWork se esta aberto a propostas
 * @param socialLinks perfis externos, sempre em ordem de exibicao e sem plataforma repetida
 */
public record Profile(
    String fullName,
    String headline,
    String bio,
    String location,
    String resumeUrl,
    boolean availableForWork,
    List<SocialLink> socialLinks) {

  // Espelham os limites das colunas em V1__create_profile_tables.sql. Validar
  // aqui tambem nao e redundancia: o banco recusaria com erro de driver, ja
  // dentro da transacao, e o dominio recusa antes com uma mensagem que diz qual
  // campo estourou.
  private static final int MAX_FULL_NAME_LENGTH = 120;
  private static final int MAX_HEADLINE_LENGTH = 180;
  private static final int MAX_LOCATION_LENGTH = 120;
  private static final int MAX_RESUME_URL_LENGTH = 500;

  public Profile {
    fullName = requireText(fullName, "Nome completo", MAX_FULL_NAME_LENGTH);
    headline = requireText(headline, "Headline", MAX_HEADLINE_LENGTH);
    bio = requireText(bio, "Bio", Integer.MAX_VALUE);
    location = normalizeOptionalText(location, "Localizacao", MAX_LOCATION_LENGTH);
    resumeUrl = normalizeOptionalText(resumeUrl, "URL do curriculo", MAX_RESUME_URL_LENGTH);
    socialLinks = normalizeLinks(socialLinks);
  }

  /** Localizacao, quando informada. */
  public Optional<String> findLocation() {
    return Optional.ofNullable(location);
  }

  /** Endereco do curriculo, quando existe. */
  public Optional<String> findResumeUrl() {
    return Optional.ofNullable(resumeUrl);
  }

  /** O link da plataforma, quando o perfil o declara. */
  public Optional<SocialLink> findLink(SocialPlatform platform) {
    return socialLinks.stream().filter(link -> link.platform() == platform).findFirst();
  }

  private static String requireText(String value, String field, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " e obrigatorio");
    }
    String trimmed = value.trim();
    if (trimmed.length() > maxLength) {
      throw new IllegalArgumentException(
          field + " excede " + maxLength + " caracteres: " + trimmed.length());
    }
    return trimmed;
  }

  /** Ausencia continua sendo {@code null}; o que nao se aceita e presenca em branco. */
  private static String normalizeOptionalText(String value, String field, int maxLength) {
    if (value == null) {
      return null;
    }
    return requireText(value, field, maxLength);
  }

  /**
   * Copia defensiva, ordenacao e unicidade de plataforma, nesta ordem.
   *
   * <p>A copia importa: sem ela o record guardaria a referencia recebida, e quem a passou poderia
   * seguir alterando a lista depois - um record com componente {@code List} nao e imovel de graca.
   *
   * <p>Ordenar aqui torna "os links estao em ordem de exibicao" verdade por construcao, em vez de
   * uma promessa que cada consulta SQL e cada template precisa lembrar de cumprir.
   *
   * <p>A unicidade repete a constraint {@code social_link_profile_platform_uk}. De proposito: o
   * dominio nao pode depender de o banco estar correto para estar correto.
   */
  private static List<SocialLink> normalizeLinks(List<SocialLink> links) {
    if (links == null) {
      throw new IllegalArgumentException("Lista de links e obrigatoria; use List.of() se vazia");
    }
    Set<SocialPlatform> seen = EnumSet.noneOf(SocialPlatform.class);
    for (SocialLink link : links) {
      if (!seen.add(link.platform())) {
        throw new IllegalArgumentException("Plataforma repetida: " + link.platform());
      }
    }
    return links.stream().sorted(Comparator.comparingInt(SocialLink::displayOrder)).toList();
  }
}
