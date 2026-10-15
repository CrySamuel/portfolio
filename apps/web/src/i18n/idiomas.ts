/**
 * Os idiomas do site, em modulo puro: sem next-intl, para que o navegador possa importa-lo.
 *
 * <p>O {@code routing} do next-intl le daqui, e o seletor de idioma da navbar - componente de
 * cliente - tambem. Uma lista so para as duas pontas: se o servidor ganhasse um idioma que o
 * seletor nao conhece, o link para ele simplesmente nao apareceria.
 */
export const IDIOMAS = ['pt-BR', 'en-US'] as const;

export type Idioma = (typeof IDIOMAS)[number];

/** O idioma sem prefixo na URL - o do conteudo original e de todo link ja compartilhado. */
export const IDIOMA_PADRAO: Idioma = 'pt-BR';
