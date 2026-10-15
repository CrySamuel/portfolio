import { getTranslations } from 'next-intl/server';

import { caminhoPublico } from '@/i18n/navigation';
import type { Idioma } from '@/i18n/routing';
import type { Cartao } from '@/lib/metadata';

/*
 * Os cartoes de compartilhamento, como as paginas os declaram: o endereco publico
 * da imagem e o texto alternativo, no idioma da pagina.
 *
 * <p><strong>Declarados pela pagina, e nao deixados a convencao do arquivo, desde
 * o commit 51.</strong> O Next monta o endereco de um {@code opengraph-image.tsx}
 * a partir da rota interna, e a rota interna agora tem o idioma: medido, a home
 * passou a anunciar {@code /pt-BR/opengraph-image}, que o middleware responde com
 * 307 para {@code /opengraph-image}. Crawler de rede social desiste rapido, e um
 * redirecionamento a mais e como um link sai sem imagem. O {@code caminhoPublico} da
 * o endereco publico - sem prefixo no portugues - e a imagem responde 200 direto.
 *
 * <p>O custo e o hash que o Next pendura no endereco da imagem por arquivo
 * ({@code ?b3f1...}), e que so muda quando o codigo da imagem muda. Sem ele, um
 * cartao redesenhado leva o tempo do cache do crawler para aparecer - o mesmo que
 * ja acontece quando o conteudo dele muda.
 *
 * <p>O alt vem das mensagens. O {@code alt} exportado pelo arquivo de imagem e um
 * so para todos os idiomas, e por isso os arquivos deixaram de exporta-lo.
 */

/**
 * O cartao do site - o da home, e o da listagem, que nao tem imagem propria.
 *
 * <p>O alt diz o que ha na imagem alem do que as outras metas ja dizem.
 */
export async function cartaoDoSite(locale: Idioma): Promise<Cartao> {
  const t = await getTranslations({ locale, namespace: 'ShareCard' });
  return { caminho: caminhoPublico({ href: '/opengraph-image', locale }), alt: t('siteAlt') };
}

/**
 * O cartao de um projeto.
 *
 * <p>O alt e o mesmo em todos os projetos. Um por projeto seria possivel, e foi
 * recusado: o {@code og:image:alt} aparece ao lado do titulo e da descricao que
 * as outras metas ja carregam, entao repetir o titulo diria a mesma coisa duas
 * vezes. O que falta a quem nao ve a imagem nao e o titulo - e saber o que ha
 * nela alem dele.
 */
export async function cartaoDoProjeto(locale: Idioma, slug: string): Promise<Cartao> {
  const t = await getTranslations({ locale, namespace: 'ShareCard' });
  return {
    caminho: caminhoPublico({ href: `/projetos/${slug}/opengraph-image`, locale }),
    alt: t('projectAlt'),
  };
}
