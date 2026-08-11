import 'server-only';

import type { Profile } from '@portfolio/api-client';
import { cache } from 'react';

import { api } from '@/lib/api/client';

/**
 * Uma hora de frescor.
 *
 * O conteudo do portfolio muda por migracao e deploy (ADR-0004), entao o dado
 * so pode mudar num momento em que o deploy ja invalida tudo de qualquer jeito.
 * A tag permite invalidar sob demanda quando houver painel ou webhook.
 */
export const PROFILE_REVALIDATE_SECONDS = 3600;

export const PROFILE_CACHE_TAG = 'profile';

/**
 * O perfil publico.
 *
 * O `cache` do React memoiza por passagem de renderizacao: o rodape no layout e
 * o hero na pagina chamam esta funcao, e a API e consultada **uma vez**. Sem
 * ele seriam duas requisicoes para montar uma tela - e o numero cresceria a cada
 * secao nova do MVP 2, que le o mesmo perfil.
 *
 * Nao ha try/catch aqui, e a ausencia e deliberada. Perfil indisponivel nao tem
 * resposta razoavel: renderizar a pagina sem nome nem bio publicaria um
 * portfolio vazio, com aparencia de site pronto. Falhar alto deixa o erro no log
 * do deploy, que e onde alguem pode agir.
 */
export const getProfile = cache((): Promise<Profile> => {
  return api.getProfile({
    next: { revalidate: PROFILE_REVALIDATE_SECONDS, tags: [PROFILE_CACHE_TAG] },
  });
});
