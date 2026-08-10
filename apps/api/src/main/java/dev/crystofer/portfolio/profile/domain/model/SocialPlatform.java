package dev.crystofer.portfolio.profile.domain.model;

/**
 * Plataformas de perfil externo suportadas.
 *
 * <p>Enum, e nao String (secao 13.5): o conjunto e fechado e conhecido em tempo de compilacao. Com
 * String, "linkedin", "LinkedIn" e "linkdin" sao todos aceitos pelo compilador e so o ultimo e um
 * bug - descoberto quando o icone nao aparece.
 *
 * <p>Os nomes acompanham os valores usados no front, em {@code SocialLinks.tsx}, e na coluna {@code
 * social_link.platform}. A traducao entre as tres pontas fica nos adaptadores; aqui o dominio so
 * declara o conjunto.
 */
public enum SocialPlatform {
  GITHUB,
  LINKEDIN,
  EMAIL
}
