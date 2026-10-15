import createMiddleware from 'next-intl/middleware';

import { routing } from '@/i18n/routing';

/**
 * Resolve o idioma de cada endereco e reescreve para a rota em {@code app/[locale]}.
 *
 * <p><strong>Middleware, e o ISR continua de pe.</strong> A ressalva do ADR-0012 era contra o
 * middleware que gera nonce de CSP: o nonce muda por requisicao, e pagina com conteudo por
 * requisicao nao pode ser pre-renderizada. Este nao toca a pagina - so decide para qual rota, ja
 * pre-renderizada, o endereco aponta. Conferido no relatorio do build, que segue marcando as rotas
 * como estaticas.
 *
 * <p>O matcher deixa de fora o que nao e pagina: {@code /_next}, e tudo que tem extensao - o
 * {@code sitemap.xml}, o {@code robots.txt}, o icone, o manifest.
 */
export default createMiddleware(routing);

export const config = {
  matcher: ['/((?!api|_next|_vercel|.*\\..*).*)'],
};
