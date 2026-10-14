import type { Metadata } from 'next';
import type { ReactNode } from 'react';

import { DEGRAUS_DA_LISTAGEM } from '@/app/projetos/trilha';
import { JsonLd } from '@/components/seo/JsonLd';
import { grafo, trilha } from '@/components/seo/structured-data';
import { ProjectsSection } from '@/features/projects/components/ProjectsSection';
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
 * <p>O caminho e `app/projetos`, e nao `app/[locale]/projetos` como a secao 16
 * lista, pela mesma razao ja registrada para a home: o segmento de idioma entra
 * no commit 51, com o roteador e o middleware juntos.
 */
export async function generateMetadata(): Promise<Metadata> {
  return metadataDaPagina(await getProfile(), {
    titulo: 'Projetos',
    descricao:
      'Projetos com o problema que resolviam, o que foi construído e o resultado que veio depois.',
    caminho: '/projetos',
    // Nao ha opengraph-image neste segmento, e o da raiz nao chega ate aqui
    // por heranca - ver metadataDaPagina.
    cartaoDoSite: true,
  });
}

export default function ProjetosPage(): ReactNode {
  return (
    <>
      <JsonLd dados={grafo(trilha(DEGRAUS_DA_LISTAGEM))} />
      <ProjectsSection variante="catalogo" />
    </>
  );
}
