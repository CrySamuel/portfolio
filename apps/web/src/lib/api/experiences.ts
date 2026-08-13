import 'server-only';

import type { Experience } from '@portfolio/api-client';
import { cache } from 'react';

import { api } from '@/lib/api/client';

/** Uma hora de frescor, pelo mesmo motivo do perfil: o conteudo muda por deploy. */
export const EXPERIENCES_REVALIDATE_SECONDS = 3600;

export const EXPERIENCES_CACHE_TAG = 'experiences';

/**
 * Espera longa, e de propósito — o mesmo raciocínio de `getProfile`.
 *
 * Quem chama é o `next build` e a revalidação do ISR, nunca um visitante. O free
 * tier do Render hiberna em 15 minutos e leva cerca de um minuto para voltar.
 */
const COLD_START_TIMEOUT_MS = 90_000;

/**
 * A timeline profissional, na ordem que o backend estabeleceu.
 *
 * O `cache` do React memoiza por passagem de renderizacao, entao a secao Sobre
 * e qualquer outra que precise da timeline custam **uma** chamada. E a mesma
 * escolha do perfil, e ela ja se pagou uma vez: sem ela, layout e pagina
 * consultavam a API duas vezes para montar uma tela so.
 *
 * Sem try/catch, tambem pelo mesmo motivo: uma timeline que falha em silencio
 * publicaria a secao Sobre vazia com aparencia de pronta. Falhar alto deixa o
 * erro no log do deploy, que e onde alguem pode agir.
 *
 * Lista vazia **nao** e erro - e o estado de quem ainda nao preencheu a propria
 * historia, e a API a distingue de falha respondendo 200 com `[]`.
 */
export const listExperiences = cache((): Promise<Experience[]> => {
  return api.listExperiences({
    next: { revalidate: EXPERIENCES_REVALIDATE_SECONDS, tags: [EXPERIENCES_CACHE_TAG] },
    timeoutMs: COLD_START_TIMEOUT_MS,
  });
});
