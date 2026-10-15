import { getTranslations } from 'next-intl/server';
import type { ReactNode } from 'react';

import { NotFoundMessage } from '@/components/common/NotFoundMessage';
import { idiomaFixado } from '@/i18n/locale';
import { caminhoPublico } from '@/i18n/navigation';

/**
 * A 404 de projeto - a que o {@code notFound()} da pagina ao lado renderiza.
 *
 * <p>Vive dentro de {@code [slug]} porque o Next escolhe a
 * {@code not-found.tsx} mais proxima do segmento que chamou. Uma unica 404 na
 * raiz atenderia todo o site com o mesmo texto generico; aqui a pagina sabe o
 * que a pessoa procurava e para onde mandar, que e o que separa uma 404 util de
 * um beco sem saida.
 *
 * <p><strong>O status HTTP e 404 de verdade.</strong> O Next serve este arquivo
 * com o codigo certo, e isso importa alem da etiqueta: pagina de erro respondida
 * como 200 e indexada pelo buscador como conteudo valido, e o endereco morto
 * passa a aparecer nos resultados.
 *
 * <p>Sem {@code metadata} aqui. O {@code generateMetadata} da rota nunca chega a
 * devolver nada quando o projeto nao existe - ele mesmo e quem chama
 * {@code notFound()} -, entao o titulo que aparece na aba e o do layout. Um
 * titulo proprio para tela de erro entra no commit 50, junto com o resto da
 * metadata por rota.
 *
 * <p>Sem parametros, como toda {@code not-found.tsx}: o idioma vem do layout,
 * por {@link idiomaFixado} - que explica por que o {@code useTranslations} nao
 * serve aqui.
 */
export default async function ProjetoNaoEncontrado(): Promise<ReactNode> {
  const locale = await idiomaFixado();
  const t = await getTranslations({ locale, namespace: 'Projects' });

  return (
    <NotFoundMessage
      eyebrow={t('notFoundEyebrow')}
      title={t('notFoundTitle')}
      description={t('notFoundDescription')}
      href={caminhoPublico({ href: '/projetos', locale })}
      linkLabel={t('notFoundBack')}
    />
  );
}
