import type { NextConfig } from 'next';

const isDev = process.env.NODE_ENV === 'development';

/**
 * Content Security Policy **sem nonce**, e a ausencia do nonce e a decisao.
 *
 * A secao 2.4 do plano pede "CSP com nonce". Nonce muda a cada requisicao,
 * entao so existe em pagina renderizada por requisicao - a propria
 * documentacao do Next e explicita: com nonce, "static optimization and
 * Incremental Static Regeneration (ISR) are disabled". A home deste site e
 * pre-renderizada com ISR justamente para que o visitante nunca espere o cold
 * start de ~1 min do plano gratuito do Render (ADR-0006, ADR-0012). Trocar isso
 * por um nonce endureceria a CSP e devolveria ao visitante o minuto de espera
 * que o ISR existe para eliminar.
 *
 * O `unsafe-inline` do script-src nao e desleixo, e o preco medido: o HTML
 * pre-renderizado tem 11 scripts inline - os dados de flight do App Router e o
 * anti-FOUC do next-themes -, nenhum deles com conteudo estavel o bastante para
 * hash. O raciocinio inteiro esta no ADR-0012.
 *
 * **A reavaliacao que o ADR-0012 marcou para o MVP 5 aconteceu aqui, e a
 * conclusao continua a mesma - por um motivo e nao por inercia.** O commit 49
 * trouxe as duas coisas que faltavam para a pergunta ficar seria: entrada de
 * usuario e script de terceiro. O que nao mudou foi o custo do nonce, que segue
 * desligando o ISR - e o formulario vive na home, que e exatamente a rota
 * pre-renderizada que o ADR-0006 protege do cold start. Uma politica por rota
 * tampouco ajuda: nao ha rota nova a proteger.
 *
 * O que mudou e a superficie, e ela foi medida em vez de suposta. O texto que o
 * visitante digita volta para a tela em `defaultValue` de <input>, ou seja como
 * atributo escapado pelo React - nunca como HTML. As mensagens recebidas nao
 * sao renderizadas em lugar nenhum do site: elas vao para o banco e para um
 * e-mail. Nao existe caminho por onde entrada de usuario vire script inline
 * nesta pagina, que era a condicao registrada no ADR.
 *
 * As demais diretivas nao dependem de nonce e valem integralmente - sao elas
 * que barram clickjacking (`frame-ancestors`), injecao de <base>
 * (`base-uri`), exfiltracao por formulario reescrito (`form-action`) e plugins
 * legados (`object-src`).
 */
const contentSecurityPolicy = [
  "default-src 'self'",
  // 'unsafe-eval' so em desenvolvimento: o React usa eval para reconstruir a
  // pilha de erro do servidor no navegador. Producao nao precisa, e deixar a
  // diretiva condicional impede que a conveniencia do dev vaze para o site.
  // challenges.cloudflare.com entra pelo Turnstile do formulario de contato, e
  // e a primeira origem de terceiro desta politica. Sao tres diretivas para um
  // widget so, e cada uma cobre uma peca diferente: o script que a pagina
  // carrega, o iframe que ele monta e as requisicoes que esse script faz para
  // buscar o desafio. Faltando qualquer uma, o widget nao aparece - e um
  // formulario sem widget passa a recusar toda mensagem enviada.
  `script-src 'self' 'unsafe-inline' https://challenges.cloudflare.com${isDev ? " 'unsafe-eval'" : ''}`,
  'frame-src https://challenges.cloudflare.com',
  // connect-src precisa existir explicitamente: sem ela, o `default-src 'self'`
  // e quem responde, e ele nao conhece a Cloudflare. `'self'` fica junto porque
  // declarar a diretiva substitui o default por completo - listar so o terceiro
  // barraria as requisicoes do proprio site.
  "connect-src 'self' https://challenges.cloudflare.com",
  // O next-themes injeta um <style> em tempo de execucao para desligar
  // transicoes durante a troca de tema (`disableTransitionOnChange`). Sem
  // 'unsafe-inline' o navegador bloqueia essa injecao e a troca volta a
  // interpolar cores no meio do caminho - que e exatamente o efeito que
  // produziu os dois falsos positivos de contraste da secao 4.30.
  "style-src 'self' 'unsafe-inline'",
  // blob: e data: cobrem o que o next/image emite. As fontes sao
  // auto-hospedadas via next/font/local, entao font-src nao precisa de origem
  // externa - e nao ter nenhuma e o que torna a diretiva util.
  "img-src 'self' blob: data:",
  "font-src 'self'",
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "frame-ancestors 'none'",
  // Fora do dev: em http://localhost nao ha o que promover, e a diretiva so
  // adicionaria ruido ao depurar.
  ...(isDev ? [] : ['upgrade-insecure-requests']),
].join('; ');

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // Remove o header X-Powered-By: nao ha ganho em anunciar o framework e a versao.
  poweredByHeader: false,
  // O design system e consumido do fonte, nao de um build intermediario. Isso
  // mantem um passo a menos no pipeline e preserva o tree-shaking do Next.
  transpilePackages: ['@portfolio/ui'],

  /**
   * Cabecalhos de seguranca da secao 2.4 do plano.
   *
   * Ficam aqui, e nao num middleware, porque cabecalho estatico nao precisa de
   * requisicao para ser decidido - e middleware nesta rota custaria o ISR. O
   * HSTS nao aparece na lista de proposito: a Vercel ja o envia, e declarar o
   * mesmo cabecalho duas vezes so cria duas fontes de verdade para o mesmo
   * valor.
   *
   * Escrito como `Promise.resolve` e nao como `async`: o Next exige que o
   * retorno seja uma promessa, mas nao ha nada para esperar aqui - a lista e
   * constante. Marcar de `async` uma funcao sem `await` e o que a regra
   * require-await reprova, e ela esta certa.
   */
  headers: () =>
    Promise.resolve([
      {
        source: '/:path*',
        headers: [
          {
            key: 'Content-Security-Policy',
            value: contentSecurityPolicy,
          },
          {
            // Impede o navegador de adivinhar o tipo de um recurso pelo
            // conteudo. Sem ele, um arquivo servido como texto mas que "parece"
            // script pode ser executado como script.
            key: 'X-Content-Type-Options',
            value: 'nosniff',
          },
          {
            // Origem completa para o proprio site, so a origem para destinos
            // externos, e nada quando o destino for http. Preserva o referer
            // util sem vazar o caminho navegado para terceiros.
            key: 'Referrer-Policy',
            value: 'strict-origin-when-cross-origin',
          },
          {
            // Lista vazia desliga a API para todas as origens, inclusive a
            // propria. O site nao usa nenhuma das tres, e recusa-las de
            // antemao significa que passar a usar exige editar esta linha - a
            // pergunta aparece na revisao, em vez de o acesso aparecer sozinho.
            key: 'Permissions-Policy',
            value: 'camera=(), microphone=(), geolocation=()',
          },
        ],
      },
    ]),
};

export default nextConfig;
