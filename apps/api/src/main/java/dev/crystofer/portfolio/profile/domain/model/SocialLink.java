package dev.crystofer.portfolio.profile.domain.model;

import dev.crystofer.portfolio.shared.domain.EmailAddress;

/**
 * Um perfil externo do dono do portfolio.
 *
 * <p>Invariante central: a URL tem de fazer sentido para a plataforma. Um link de GitHub apontando
 * para {@code mailto:} ou um link de e-mail apontando para {@code https://} sao dados corrompidos,
 * e o lugar de barra-los e aqui - antes de virar linha no banco e antes de virar {@code href} numa
 * pagina. A tabela {@code social_link} nao tem como expressar essa regra em constraint.
 *
 * @param platform plataforma de destino
 * @param url endereco completo, ja no formato que vai para o {@code href}
 * @param displayOrder posicao na listagem, crescente
 */
public record SocialLink(SocialPlatform platform, String url, int displayOrder) {

  /** Espelha {@code social_link.url VARCHAR(500)}. */
  private static final int MAX_URL_LENGTH = 500;

  private static final String MAILTO_PREFIX = "mailto:";

  public SocialLink {
    if (platform == null) {
      throw new IllegalArgumentException("Plataforma e obrigatoria");
    }
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("URL e obrigatoria para " + platform);
    }
    url = url.trim();
    if (url.length() > MAX_URL_LENGTH) {
      throw new IllegalArgumentException(
          "URL excede " + MAX_URL_LENGTH + " caracteres: " + url.length());
    }
    if (displayOrder < 0) {
      throw new IllegalArgumentException("Ordem de exibicao nao pode ser negativa");
    }
    requireSchemeMatchingPlatform(platform, url);
  }

  /**
   * Constroi o link de e-mail a partir do value object, e nao de uma String solta.
   *
   * <p>E a unica forma de criar um link EMAIL sem repetir o prefixo {@code mailto:} em cada
   * chamador - e repetir literal de protocolo e como se erra a grafia dele.
   */
  public static SocialLink email(EmailAddress address, int displayOrder) {
    return new SocialLink(SocialPlatform.EMAIL, MAILTO_PREFIX + address.value(), displayOrder);
  }

  /**
   * O e-mail por tras de um link EMAIL.
   *
   * @throws IllegalStateException se a plataforma nao for {@link SocialPlatform#EMAIL}
   */
  public EmailAddress emailAddress() {
    if (platform != SocialPlatform.EMAIL) {
      throw new IllegalStateException("Link de " + platform + " nao tem e-mail");
    }
    return EmailAddress.of(url.substring(MAILTO_PREFIX.length()));
  }

  private static void requireSchemeMatchingPlatform(SocialPlatform platform, String url) {
    if (platform == SocialPlatform.EMAIL) {
      if (!url.startsWith(MAILTO_PREFIX)) {
        throw new IllegalArgumentException("Link de e-mail precisa comecar com " + MAILTO_PREFIX);
      }
      // Delega a validacao do endereco a quem e dono dela. O construtor de
      // EmailAddress lanca se o trecho apos o prefixo nao for um e-mail.
      EmailAddress.of(url.substring(MAILTO_PREFIX.length()));
      return;
    }
    if (!url.startsWith("https://")) {
      throw new IllegalArgumentException("Link de " + platform + " precisa usar https");
    }
  }
}
