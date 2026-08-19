import 'server-only';

import type { GitHubStats } from '@portfolio/api-client';
import { cache } from 'react';

import { api } from '@/lib/api/client';

/**
 * Uma hora de frescor - e, ao contrario das outras leituras, aqui isso importa.
 *
 * Perfil, experiencias, skills e projetos mudam por deploy; o GitHub muda
 * sozinho. Mesmo assim o teto real de frescor nao e este numero: o adaptador
 * guarda o retrato por seis horas (ADR-0008), entao revalidar de hora em hora
 * so garante que a pagina pegue o retrato novo logo depois de ele existir, sem
 * gastar cota nenhuma a mais.
 */
export const GITHUB_REVALIDATE_SECONDS = 3600;

export const GITHUB_CACHE_TAG = 'github';

/** Espera longa pelo mesmo motivo das outras leituras - quem chama e o build, nunca o visitante. */
const COLD_START_TIMEOUT_MS = 90_000;

/**
 * O retrato do GitHub.
 *
 * <strong>Sem try/catch, e aqui a ausencia significa o contrario do usual.</strong>
 * Nas outras secoes ela existe para que uma falha derrube o build em vez de
 * publicar um portfolio com conteudo faltando em silencio. Nesta, ela existe
 * porque **nao ha falha a tratar**: a API responde 200 com o retrato vazio
 * quando o GitHub esta fora, e um `catch` aqui cobriria apenas a queda da
 * propria API - que e o caso em que as outras secoes ja derrubam o build.
 */
export const getGitHubStats = cache((): Promise<GitHubStats> => {
  return api.getGitHubStats({
    next: { revalidate: GITHUB_REVALIDATE_SECONDS, tags: [GITHUB_CACHE_TAG] },
    timeoutMs: COLD_START_TIMEOUT_MS,
  });
});

/**
 * O retrato veio vazio?
 *
 * O contrato nao publica a bandeira `isEmpty` do dominio, e nao deveria mesmo:
 * ela e uma pergunta que o consumidor responde olhando o que recebeu. Vazio e
 * nao ter nem linguagem nem repositorio - com o GitHub de pe, um perfil sem
 * nenhum dos dois nao existe.
 *
 * `publicRepositories` **nao** entra na conta: ele vem do endpoint de perfil, e
 * um retrato pela metade - perfil respondido, repositorios nao - e justamente o
 * que o invariante do dominio recusa antes de chegar aqui.
 */
export function retratoVazio(stats: GitHubStats): boolean {
  return stats.languages.length === 0 && stats.repositories.length === 0;
}
