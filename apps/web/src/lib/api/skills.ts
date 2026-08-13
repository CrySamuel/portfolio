import 'server-only';

import type { SkillCategory } from '@portfolio/api-client';
import { cache } from 'react';

import { api } from '@/lib/api/client';

/** Uma hora de frescor, como as demais leituras: o conteudo muda por deploy. */
export const SKILLS_REVALIDATE_SECONDS = 3600;

export const SKILLS_CACHE_TAG = 'skills';

/** Espera longa pelo mesmo motivo das outras leituras — quem chama e o build, nunca o visitante. */
const COLD_START_TIMEOUT_MS = 90_000;

/**
 * As competencias agrupadas, na ordem que o backend estabeleceu.
 *
 * O agrupamento chega pronto e nao se refaz aqui: e regra de negocio, e refazer
 * seria um segundo lugar decidindo a mesma coisa.
 *
 * Sem try/catch, como as demais: uma secao que falha em silencio publicaria um
 * portfolio com aparencia de pronto e conteudo faltando.
 */
export const listSkills = cache((): Promise<SkillCategory[]> => {
  return api.listSkills({
    next: { revalidate: SKILLS_REVALIDATE_SECONDS, tags: [SKILLS_CACHE_TAG] },
    timeoutMs: COLD_START_TIMEOUT_MS,
  });
});
