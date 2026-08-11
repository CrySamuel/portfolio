import type { SocialPlatform } from '@portfolio/api-client';
import { cn } from '@portfolio/ui';
import type { ReactNode } from 'react';

export interface SocialLink {
  readonly platform: SocialPlatform;
  readonly href: string;
  /** Nome acessível do link. Descreve o destino, não o ícone. */
  readonly label: string;
}

/**
 * O nome acessível de cada plataforma.
 *
 * Rótulo é texto de interface, não dado: ele muda com o idioma e não com o
 * conteúdo do perfil, então não vem da API. O que vem de lá é a lista de
 * plataformas e as URLs.
 *
 * O tipo é `Record<SocialPlatform, string>`, e `SocialPlatform` é a união
 * gerada do contrato. Uma plataforma nova na API passa a **quebrar a compilação
 * aqui** até que alguém escreva o rótulo e desenhe o ícone - que é exatamente o
 * momento certo de decidir as duas coisas. Sem isso, o link novo apareceria em
 * produção anunciado como "link", com o ícone de e-mail.
 */
export const SOCIAL_LINK_LABELS: Record<SocialPlatform, string> = {
  github: 'Perfil no GitHub',
  linkedin: 'Perfil no LinkedIn',
  email: 'Enviar e-mail',
};

export interface SocialLinksProps {
  links: readonly SocialLink[];
  className?: string;
}

/**
 * Perfis externos (seção 8.3).
 *
 * Recebe os links por prop e não conhece nenhum deles: o conteúdo real vem do
 * perfil na API, e um componente que soubesse a URL do GitHub precisaria ser
 * editado para trocar de conta. Regra 6 da seção 8.7 - dado não mora no
 * componente.
 *
 * Sem `target="_blank"`. Abrir aba nova sem avisar é mudança de contexto
 * inesperada (critério 3.2.5 da WCAG), e a alternativa - anunciar "(abre em
 * nova aba)" em cada rótulo - polui a leitura de todos para resolver o problema
 * de nenhum. Quem quer aba nova usa o meio do mouse ou Ctrl+clique, que
 * continuam funcionando.
 */
export function SocialLinks({ links, className }: SocialLinksProps): ReactNode {
  return (
    <ul className={cn('flex items-center gap-1', className)}>
      {links.map((link) => (
        <li key={link.platform}>
          <a
            href={link.href}
            // `me` declara que o perfil do outro lado é da mesma pessoa - é o
            // que permite verificação de identidade por backlink. `noopener`
            // impede que o destino alcance esta janela por window.opener.
            rel="me noopener"
            // O ícone é decorativo; quem nomeia o link é este rótulo. Sem ele o
            // leitor de tela anunciaria apenas "link", ou leria a URL inteira.
            aria-label={link.label}
            className="flex size-11 items-center justify-center rounded-md text-fg-muted transition-colors duration-(--duration-fast) hover:bg-surface-2 hover:text-fg"
          >
            <SocialIcon platform={link.platform} />
          </a>
        </li>
      ))}
    </ul>
  );
}

/**
 * Ícones desenhados aqui, sem biblioteca.
 *
 * A seção 10.3 conta ~50 KB para uma biblioteca de ícones completa. Três
 * caminhos SVG resolvem o caso e custam bytes, não quilobytes.
 */
function SocialIcon({ platform }: { platform: SocialPlatform }): ReactNode {
  return SOCIAL_ICONS[platform]();
}

const COMUM = { 'aria-hidden': true, width: 20, height: 20, viewBox: '0 0 24 24' } as const;

/**
 * Um ícone por plataforma, num `Record` e não numa cadeia de `if`.
 *
 * A cadeia terminava com `return` do ícone de e-mail, e esse `else` implícito
 * era uma armadilha: plataforma nova compilaria e apareceria com o ícone
 * errado. O `Record` obriga a completar o conjunto - a mesma exaustividade que
 * `SOCIAL_LINK_LABELS` dá aos rótulos.
 */
const SOCIAL_ICONS: Record<SocialPlatform, () => ReactNode> = {
  github: () => (
    <svg {...COMUM} fill="currentColor">
      <path d="M12 2C6.48 2 2 6.58 2 12.25c0 4.53 2.87 8.37 6.84 9.73.5.1.68-.22.68-.49l-.01-1.72c-2.78.62-3.37-1.37-3.37-1.37-.46-1.18-1.11-1.5-1.11-1.5-.91-.64.07-.62.07-.62 1 .07 1.53 1.06 1.53 1.06.89 1.57 2.34 1.12 2.91.86.09-.66.35-1.12.63-1.38-2.22-.26-4.56-1.14-4.56-5.07 0-1.12.39-2.03 1.03-2.75-.1-.26-.45-1.3.1-2.71 0 0 .84-.28 2.75 1.05a9.3 9.3 0 0 1 5 0c1.91-1.33 2.75-1.05 2.75-1.05.55 1.41.2 2.45.1 2.71.64.72 1.03 1.63 1.03 2.75 0 3.94-2.34 4.81-4.57 5.06.36.32.68.94.68 1.9l-.01 2.82c0 .27.18.6.69.49A10.03 10.03 0 0 0 22 12.25C22 6.58 17.52 2 12 2Z" />
    </svg>
  ),

  linkedin: () => (
    <svg {...COMUM} fill="currentColor">
      <path d="M20.45 20.45h-3.56v-5.57c0-1.33-.02-3.04-1.85-3.04-1.85 0-2.14 1.45-2.14 2.94v5.67H9.35V9h3.41v1.56h.05c.48-.9 1.63-1.85 3.36-1.85 3.6 0 4.27 2.37 4.27 5.45v6.29ZM5.34 7.43a2.06 2.06 0 1 1 0-4.13 2.06 2.06 0 0 1 0 4.13Zm1.78 13.02H3.55V9h3.57v11.45ZM22.22 0H1.77C.79 0 0 .77 0 1.72v20.56C0 23.23.79 24 1.77 24h20.45c.98 0 1.78-.77 1.78-1.72V1.72C24 .77 23.2 0 22.22 0Z" />
    </svg>
  ),

  email: () => (
    <svg {...COMUM} fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round">
      <rect x="2" y="4" width="20" height="16" rx="2" />
      <path d="m2.5 6.5 8.4 6.1a2 2 0 0 0 2.2 0l8.4-6.1" />
    </svg>
  ),
};
