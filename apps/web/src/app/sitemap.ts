import type { MetadataRoute } from 'next';

import { caminhosPorIdioma } from '@/i18n/navigation';
import { routing } from '@/i18n/routing';
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
 * <p><strong>Cada pagina aparece uma vez por idioma</strong>, e cada entrada lista todas as
 * alternativas, a propria inclusive, mais o {@code x-default}. E a forma que o Google pede para o
 * {@code hreflang} no sitemap: a declaracao tem de ser reciproca, e uma entrada que nao se lista
 * entre as alternativas e ignorada.
 */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const projetos = await listProjects(routing.defaultLocale);

  const paginas: { href: string; publicadoEm?: string }[] = [
    { href: '/' },
    { href: '/projetos' },
    ...projetos.map((projeto) => ({
      href: `/projetos/${projeto.slug}`,
      ...(projeto.publishedAt === null ? {} : { publicadoEm: projeto.publishedAt }),
    })),
  ];

  return paginas.flatMap(({ href, publicadoEm }) => {
    const porIdioma = caminhosPorIdioma(href);
    const alternativas = {
      ...Object.fromEntries(routing.locales.map((locale) => [locale, absoluto(porIdioma[locale])])),
      'x-default': absoluto(porIdioma[routing.defaultLocale]),
    };

    return routing.locales.map((locale) => ({
      url: absoluto(porIdioma[locale]),
      ...(publicadoEm === undefined ? {} : { lastModified: publicadoEm }),
      alternates: { languages: alternativas },
    }));
  });
}

function absoluto(caminho: string): string {
  return new URL(caminho, SITE_URL).href;
}
