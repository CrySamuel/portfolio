import { getTranslations } from 'next-intl/server';
import type { ReactNode } from 'react';

import { NotFoundMessage } from '@/components/common/NotFoundMessage';
import { idiomaFixado } from '@/i18n/locale';
import { caminhoPublico } from '@/i18n/navigation';

/**
 * A 404 de endereco sem rota - a que o catch-all {@code [...rest]} renderiza.
 *
 * <p><strong>Existe junto com o catch-all.</strong> Ate o commit 50 a 404 de endereco sem rota era
 * a padrao do Next - "This page could not be found.", em ingles, num site em portugues -, e a troca
 * por uma tela propria foi a unica mudanca visivel do refactor de i18n.
 *
 * <p><strong>Ela chega montada pelo navegador, e nao pronta do servidor</strong> - como a 404 de
 * projeto ja chegava antes do commit 51, em producao. No Next 15.5, um {@code notFound()} lancado
 * durante a renderizacao sai como documento de erro ({@code <html id="__next_error__">}), com status
 * 404 e a pagina inteira no payload, e o navegador a monta com o layout. Medido no commit 51: as
 * tentativas de devolve-la pronta - layout raiz de repasse, 404 global propria - fizeram todas as
 * 404 sairem vazias, e foram desfeitas.
 *
 * <p>O idioma vem de {@link idiomaFixado}, pela mesma razao da 404 de projeto: o Next a renderiza
 * junto com toda pagina do segmento, e um {@code useTranslations} aqui tornaria todas dinamicas.
 */
export default async function PaginaNaoEncontrada(): Promise<ReactNode> {
  const locale = await idiomaFixado();
  const t = await getTranslations({ locale, namespace: 'NotFound' });

  return (
    <NotFoundMessage
      eyebrow={t('eyebrow')}
      title={t('title')}
      description={t('description')}
      href={caminhoPublico({ href: '/', locale })}
      linkLabel={t('back')}
    />
  );
}
