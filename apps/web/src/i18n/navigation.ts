import { IDIOMA_PADRAO, IDIOMAS, type Idioma } from '@/i18n/idiomas';

/**
 * Os segmentos de rota com nome proprio em cada idioma. A chave e o nome da pasta em
 * {@code app/[locale]} - o caminho interno, em portugues.
 *
 * <p>So o primeiro segmento muda de nome: {@code /projetos/finai} e {@code /projects/finai}. O slug
 * do projeto e o mesmo nos dois idiomas, e as ancoras das secoes da home tambem.
 */
export const SEGMENTOS: Readonly<Record<string, Readonly<Record<Idioma, string>>>> = {
  projetos: { 'pt-BR': 'projetos', 'en-US': 'projects' },
};

/**
 * O endereco publico de uma rota num idioma - {@code /projetos/finai} em portugues,
 * {@code /en-US/projects/finai} em ingles.
 *
 * <p><strong>Escrita aqui, e nao o {@code getPathname} do {@code createNavigation} do next-intl - e
 * a troca foi medida.</strong> O {@code createNavigation} cria tambem o {@code Link} da biblioteca,
 * que e componente de cliente, e importar o modulo basta para o build registrar esse {@code Link}: o
 * bundle de toda pagina passou a levar o {@code Link} e o pedaco do use-intl que ele usa, sem um
 * unico link dele na tela.
 *
 * <p>A regra e a do {@code as-needed} de i18n/routing: o idioma padrao fica sem prefixo, os demais o
 * levam. O {@code href} e sempre o caminho interno, em portugues e sem idioma.
 */
export function caminhoPublico({ href, locale }: { href: string; locale: Idioma }): string {
  const traduzido = caminhoTraduzido(href, locale);
  if (locale === IDIOMA_PADRAO) return traduzido;
  return traduzido === '/' ? `/${locale}` : `/${locale}${traduzido}`;
}

/**
 * O caminho com os segmentos no idioma, e sem o prefixo dele - a forma que os {@code pathnames}
 * do next-intl declaram. Aceita os moldes de rota ({@code /projetos/[slug]}) tanto quanto
 * caminhos concretos.
 */
export function caminhoTraduzido(href: string, locale: Idioma): string {
  const [, primeiro = '', ...resto] = href.split('/');
  return ['', SEGMENTOS[primeiro]?.[locale] ?? primeiro, ...resto].join('/');
}

/**
 * Os enderecos de uma pagina - o dela e o de cada idioma -, como {@code metadataDaPagina} os quer
 * para o canonical e o {@code hreflang}.
 */
export function enderecosDaPagina(
  href: string,
  locale: Idioma,
): { idioma: Idioma; porIdioma: Record<Idioma, string>; padrao: Idioma } {
  return { idioma: locale, porIdioma: caminhosPorIdioma(href), padrao: IDIOMA_PADRAO };
}

/** O mesmo caminho interno em cada idioma - as alternativas que o {@code hreflang} anuncia. */
export function caminhosPorIdioma(href: string): Record<Idioma, string> {
  return Object.fromEntries(
    IDIOMAS.map((locale) => [locale, caminhoPublico({ href, locale })]),
  ) as Record<Idioma, string>;
}

/**
 * O inverso de {@link caminhoPublico}: de um endereco do navegador ao idioma e ao caminho interno.
 *
 * <p>Aceita as duas formas que uma pagina pode ver de si mesma. No navegador, o
 * {@code usePathname} devolve o endereco publico ({@code /en-US/projects/finai}); na
 * pre-renderizacao, o da rota ({@code /en-US/projetos/finai}, e {@code /pt-BR/...} para o padrao).
 * Resolver as duas para o mesmo caminho interno e o que mantem o link do seletor igual no HTML e
 * depois da hidratacao.
 */
export function caminhoInterno(caminho: string): { locale: Idioma; href: string } {
  const [, primeiro = '', ...resto] = caminho.split('/');
  const prefixo = IDIOMAS.find((idioma) => idioma === primeiro);
  const locale = prefixo ?? IDIOMA_PADRAO;
  const segmentos = prefixo === undefined ? [primeiro, ...resto] : resto;

  const [inicial = '', ...demais] = segmentos;
  const interno =
    Object.entries(SEGMENTOS).find(
      ([pasta, nomes]) => inicial === pasta || inicial === nomes[locale],
    )?.[0] ?? inicial;
  const href = ['', interno, ...demais].join('/');
  return { locale, href: href === '' ? '/' : href };
}
