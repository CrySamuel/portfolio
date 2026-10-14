import type { MetadataRoute } from 'next';

import { SITE_URL } from '@/lib/site';

/**
 * O {@code robots.txt}: tudo liberado, e o endereco do sitemap.
 *
 * <p><strong>Nao bloqueia {@code /_next/}, que o exemplo da secao 9.5 do plano
 * bloqueava.</strong> E ali que estao o JavaScript e o CSS da pagina, e o
 * Google renderiza a pagina antes de indexa-la - sem esses arquivos ele ve um
 * documento sem estilo e sem o conteudo dos Client Components, e a propria
 * documentacao dele pede para nao bloquea-los. Tambem nao ha {@code /api/} a
 * bloquear: o site nao tem Route Handler, e o formulario fala com o servidor por
 * Server Action, no endereco da propria pagina.
 *
 * <p>Crawlers de IA ficam liberados de proposito, como a secao 9.5 decide:
 * aparecer em respostas de assistente e canal legitimo de descoberta para um
 * portfolio.
 *
 * <p>Deploys de preview nao precisam de regra aqui. A Vercel ja os serve com
 * {@code X-Robots-Tag: noindex}, e so a producao e indexavel.
 */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: [{ userAgent: '*', allow: '/' }],
    sitemap: new URL('/sitemap.xml', SITE_URL).href,
  };
}
