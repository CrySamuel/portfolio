import type { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import type { ReactNode } from 'react';

import { cartaoDoSite } from '@/app/[locale]/cartoes';
import { degrausDaListagem } from '@/app/[locale]/projetos/trilha';
import { JsonLd } from '@/components/seo/JsonLd';
import { grafo, trilha } from '@/components/seo/structured-data';
import { ProjectsSection } from '@/features/projects/components/ProjectsSection';
import { fixarIdioma, idiomaDaRota, type ParametrosDoIdioma } from '@/i18n/locale';
import { enderecosDaPagina } from '@/i18n/navigation';
import { getProfile } from '@/lib/api/profile';
import { metadataDaPagina } from '@/lib/metadata';

/**
 * O catalogo inteiro, com o filtro por tecnologia.
 *
 * <p>A home mostra so os destacados; esta rota mostra todos e e a unica que
 * carrega o filtro - o unico Client Component do MVP 3. Quem so passa pela home
 * nao paga por ele.
 *
 * <p>A pagina e estatica, como a home, e e isso que mantem o visitante longe do
 * cold start da API (secao 3, desvio 32). Ler {@code searchParams} aqui a
 * tornaria dinamica: o filtro seria resolvido no servidor, mais simples de
 * escrever, e cada visita passaria a esperar pela API. O filtro no cliente e o
 * preco de manter a pagina estatica, e ele e pago em bytes, uma vez.
 *
 * <p>Mora em `app/[locale]/projetos` desde o commit 51, como a secao 16 lista;
 * o portugues continua em `/projetos` (ver i18n/routing).
 */
export async function generateMetadata({ params }: ParametrosDoIdioma): Promise<Metadata> {
  const locale = await idiomaDaRota(params);
  const [perfil, t, cartao] = await Promise.all([
    getProfile(locale),
    getTranslations({ locale, namespace: 'Metadata' }),
    cartaoDoSite(locale),
  ]);

  return metadataDaPagina(perfil, {
    titulo: t('projectsTitle'),
    descricao: t('projectsDescription'),
    enderecos: enderecosDaPagina('/projetos', locale),
    // Nao ha opengraph-image neste segmento: a listagem usa o cartao do site.
    cartao,
  });
}

export default async function ProjetosPage({ params }: ParametrosDoIdioma): Promise<ReactNode> {
  const locale = await idiomaDaRota(params);
  fixarIdioma(locale);

  return (
    <>
      <JsonLd dados={grafo(trilha(await degrausDaListagem(locale)))} />
      <ProjectsSection variante="catalogo" />
    </>
  );
}
