import 'server-only';

import { createApiClient } from '@portfolio/api-client';

import { env } from '@/lib/env';

/**
 * O cliente da API, configurado uma vez.
 *
 * Instancia unica de proposito: as opcoes - raiz, timeout, tentativas - sao
 * decisoes sobre a mesma API, e espalha-las por chamador significaria descobrir
 * um dia que metade do codigo espera 5s e a outra metade nao espera nada.
 *
 * A leitura sai daqui direto para a API, sem passar por Route Handler. E o que
 * o ADR-0005 determina: Server Component ja esta no servidor, entao um salto
 * extra so acrescentaria latencia. Route Handler existe para **escrita**, onde
 * ha segredo a esconder do navegador e Turnstile a verificar.
 */
export const api = createApiClient({ baseUrl: env.API_URL });
