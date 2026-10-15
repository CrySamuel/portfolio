import { getTranslations } from 'next-intl/server';

import type { Degrau } from '@/components/seo/structured-data';
import { caminhoPublico } from '@/i18n/navigation';
import type { Idioma } from '@/i18n/routing';

/**
 * A trilha de navegacao da listagem, que e tambem o comeco da trilha de cada
 * projeto.
 *
 * <p>Mora no segmento, e nao na pagina, porque o Next so aceita de um
 * {@code page.tsx} as exportacoes da convencao - e as duas paginas precisam da
 * mesma lista. Duas copias divergiriam no primeiro ajuste de nome.
 *
 * <p>Funcao, e nao constante, desde que o site tem idioma: o nome de cada degrau
 * e o caminho dele dependem do idioma da pagina.
 */
export async function degrausDaListagem(locale: Idioma): Promise<readonly Degrau[]> {
  const t = await getTranslations({ locale, namespace: 'Metadata' });

  return [
    { nome: t('breadcrumbHome'), caminho: caminhoPublico({ href: '/', locale }) },
    { nome: t('breadcrumbProjects'), caminho: caminhoPublico({ href: '/projetos', locale }) },
  ];
}
