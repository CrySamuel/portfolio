import type { MetadataRoute } from 'next';

import { listProjects } from '@/lib/api/projects';
import { SITE_URL } from '@/lib/site';

/**
 * O sitemap, gerado a partir do catalogo.
 *
 * <p><strong>Adicionar um projeto no banco faz a pagina existir, entrar aqui e
 * ser indexada - sem tocar em codigo do site.</strong> E o retorno concreto de
 * manter o conteudo em banco (secao 9.5 do plano): a lista sai da mesma chamada
 * que a listagem e o {@code generateStaticParams} usam, e por isso nao ha como o
 * sitemap anunciar uma pagina que o build nao gerou.
 *
 * <p>Tres desvios do exemplo do plano, todos pelo mesmo criterio - o sitemap so
 * afirma o que e verdade:
 *
 * <ul>
 *   <li><strong>Sem {@code /sobre} e {@code /contato}.</strong> Sao secoes da
 *       home, e nao rotas; anunciar endereco que responde 404 e erro de
 *       cobertura no Search Console.
 *   <li><strong>Sem {@code lastModified: new Date()}.</strong> Uma data que muda
 *       a cada build diz que tudo mudou sempre, e o Google, ao perceber que o
 *       {@code lastmod} de um site nao corresponde a mudanca real, passa a
 *       ignora-lo no site inteiro. So os projetos levam data, e a que existe: a
 *       de publicacao. A API nao tem data de alteracao para oferecer.
 *   <li><strong>Sem {@code changeFrequency} e {@code priority}.</strong> O
 *       Google declara que ignora os dois.
 * </ul>
 *
 * <p>As alternativas de idioma entram no commit 51, com as rotas localizadas.
 */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const projetos = await listProjects();

  return [
    { url: new URL('/', SITE_URL).href },
    { url: new URL('/projetos', SITE_URL).href },
    ...projetos.map((projeto) => ({
      url: new URL(`/projetos/${projeto.slug}`, SITE_URL).href,
      ...(projeto.publishedAt === null ? {} : { lastModified: projeto.publishedAt }),
    })),
  ];
}
