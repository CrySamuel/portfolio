import 'server-only';

import type { ProjectSummary } from '@portfolio/api-client';
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
