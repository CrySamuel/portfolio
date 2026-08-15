import 'server-only';

import type { ProjectDetail, ProjectSummary } from '@portfolio/api-client';
import { cache } from 'react';

import { api } from '@/lib/api/client';

/** Uma hora de frescor, como as demais leituras: o conteudo muda por deploy. */
export const PROJECTS_REVALIDATE_SECONDS = 3600;

export const PROJECTS_CACHE_TAG = 'projects';

/** Espera longa pelo mesmo motivo das outras leituras — quem chama e o build, nunca o visitante. */
const COLD_START_TIMEOUT_MS = 90_000;

/**
 * O catalogo de projetos, na ordem que o backend estabeleceu.
 *
 * A ordem chega pronta e nao se refaz aqui: e regra de negocio, e refazer seria
 * um segundo lugar decidindo a mesma coisa.
 *
 * Sem try/catch, como as demais: uma secao que falha em silencio publicaria um
 * portfolio com aparencia de pronto e conteudo faltando.
 *
 * O `cache` do React desduplica a chamada dentro do mesmo render, que e o que
 * permite a home e a listagem pedirem a mesma coisa sem dobrar a requisicao.
 */
export const listProjects = cache((): Promise<ProjectSummary[]> => {
  return api.listProjects({
    next: { revalidate: PROJECTS_REVALIDATE_SECONDS, tags: [PROJECTS_CACHE_TAG] },
    timeoutMs: COLD_START_TIMEOUT_MS,
  });
});

/**
 * Um projeto pelo slug, com a narrativa completa.
 *
 * <p>Chamada propria, e nao um `find` sobre {@link listProjects}. O resumo da
 * listagem nao carrega `problem`, `solution`, `outcome`, enderecos nem metricas
 * - filtrar a lista devolveria um objeto sem metade da pagina, e o tipo diz
 * isso: `ProjectSummary` e `ProjectDetail` sao contratos diferentes.
 *
 * <p>A mesma tag das demais leituras de projeto: quem revalidar o catalogo
 * revalida os detalhes junto, porque a fonte e a mesma migracao de seed. Uma tag
 * por slug permitiria invalidar um projeto isolado, e nao ha hoje quem chame
 * isso - a rota de revalidacao so chega no MVP 5.
 *
 * <p>Erro nao e engolido aqui. O 404 e o 400 da API sobem como `ApiError` e sao
 * traduzidos na pagina, que e quem sabe que ausencia de projeto vira
 * `notFound()` e nao tela de erro.
 */
export const getProject = cache((slug: string): Promise<ProjectDetail> => {
  return api.getProjectBySlug(slug, {
    next: { revalidate: PROJECTS_REVALIDATE_SECONDS, tags: [PROJECTS_CACHE_TAG] },
    timeoutMs: COLD_START_TIMEOUT_MS,
  });
});
