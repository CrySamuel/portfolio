import { notFound } from 'next/navigation';
import { setRequestLocale } from 'next-intl/server';
import { cache } from 'react';

import { ehIdioma, routing, type Idioma } from '@/i18n/routing';

/**
 * Um conjunto de parametros por idioma - o {@code generateStaticParams} do segmento de idioma.
 *
 * <p>O layout declara, e as imagens de compartilhamento declaram de novo: rota de imagem e route
 * handler, e route handler nao passa pelo layout, entao nao herda a lista dele. Medido: sem a
 * declaracao propria, o build marcou {@code /[locale]/opengraph-image} como {@code ƒ}.
 */
export function parametrosDosIdiomas(): { locale: Idioma }[] {
  return routing.locales.map((locale) => ({ locale }));
}

/** Os parametros de toda rota sob {@code app/[locale]}, como o Next 15 os entrega: em promessa. */
export interface ParametrosDoIdioma {
  readonly params: Promise<{ readonly locale: string }>;
}

/**
 * O idioma da rota, ja conferido.
 *
 * <p>O Next tipa o segmento como {@code string}, e o next-intl pede um dos idiomas do site - e a
 * diferenca e real: o segmento vem da URL. Um idioma fora da lista e 404, e nao o portugues por
 * engano; servir a pagina padrao em {@code /xx} seria publicar a mesma pagina num segundo endereco.
 *
 * <p>Layout, paginas e imagens chamam esta funcao, e nao um {@code as}: a conversao calaria o
 * compilador sem conferir nada.
 */
export async function idiomaDaRota(params: Promise<{ readonly locale: string }>): Promise<Idioma> {
  const { locale } = await params;
  if (!ehIdioma(locale)) notFound();
  return locale;
}

/**
 * Fixa o idioma desta renderizacao, para o next-intl le-lo sem olhar a requisicao.
 *
 * <p>Sem isto, cada traducao leria o cabecalho que o middleware deixa, e ler cabecalho torna a rota
 * dinamica: o ISR iria embora junto. Layout e paginas chamam antes de qualquer traducao.
 *
 * <p><strong>O {@code setRequestLocale} esta deprecado, e continua aqui de proposito.</strong> O
 * substituto que o next-intl indica e o {@code next/root-params}, que no Next 15.5 ainda e
 * {@code experimental.rootParams} - flag experimental em producao e o risco maior. Esta funcao e o
 * unico lugar que chama a API deprecada: a migracao, quando o Next a estabilizar, e um arquivo.
 */
export function fixarIdioma(locale: Idioma): void {
  // eslint-disable-next-line @typescript-eslint/no-deprecated -- o substituto e experimental no Next 15.5; ver acima
  setRequestLocale(locale);
  idiomaDaRenderizacao().fixar(locale);
}

/**
 * O idioma desta renderizacao, para quem nao recebe parametros - a {@code not-found.tsx}.
 *
 * <p><strong>A 404 nao pode usar o {@code useTranslations} direto, e isso foi medido.</strong> O Next
 * renderiza a {@code not-found.tsx} de um segmento junto com a pagina, antes de o layout chegar ao
 * {@code fixarIdioma}; sem idioma fixado, o next-intl le o cabecalho da requisicao, e ler cabecalho
 * torna a rota dinamica. Com a 404 traduzida assim, o build deixou de gravar o HTML das paginas de
 * projeto - e elas passaram a ser renderizadas por visita, de volta ao cold start da API.
 *
 * <p>A promessa e por renderizacao (o {@code cache} do React) e resolve quando o layout fixa o
 * idioma. Quem espera por ela traduz com o idioma explicito, que o next-intl usa sem olhar a
 * requisicao. Nao fica pendurada: toda rota sob {@code app/[locale]} passa pelo layout.
 */
export function idiomaFixado(): Promise<Idioma> {
  return idiomaDaRenderizacao().idioma;
}

const idiomaDaRenderizacao = cache(() => {
  let fixar: (locale: Idioma) => void = () => undefined;
  const idioma = new Promise<Idioma>((resolve) => {
    fixar = resolve;
  });
  return { idioma, fixar };
});
