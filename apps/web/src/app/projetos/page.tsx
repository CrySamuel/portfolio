import type { Metadata } from 'next';
import type { ReactNode } from 'react';

import { ProjectsSection } from '@/features/projects/components/ProjectsSection';

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
export const metadata: Metadata = {
  title: 'Projetos',
  description:
    'Projetos com o problema que resolviam, o que foi construído e o resultado que veio depois.',
};

export default function ProjetosPage(): ReactNode {
  return <ProjectsSection variante="catalogo" />;
}
