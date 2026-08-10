import type { Metadata, Viewport } from 'next';
import type { ReactNode } from 'react';

import { SkipLink } from '@/components/common/SkipLink';
import { Footer } from '@/components/layout/Footer';
import { Navbar } from '@/components/layout/Navbar';
import { ThemeProvider } from '@/components/providers/ThemeProvider';
import { fontVariables } from '@/lib/fonts';
import { MAIN_ID, SOCIAL_LINKS } from '@/lib/navigation';

import './globals.css';

export const metadata: Metadata = {
  title: 'Crystofer Demetino — Desenvolvedor Backend',
  description:
    'Portfólio de Crystofer Demetino, Desenvolvedor Backend especializado em Java e Spring Boot.',
};

/**
 * Duas metas, uma por preferencia de sistema: e o que tinge a barra do
 * navegador certo na primeira visita, sem depender de JavaScript. A partir da
 * hidratacao o ThemeProvider assume as duas, para que a escolha manual - que
 * nao mexe na preferencia do sistema - tambem seja refletida.
 *
 * As cores sao os --color-bg dos dois temas: <meta> nao aceita var().
 */
export const viewport: Viewport = {
  themeColor: [
    { media: '(prefers-color-scheme: dark)', color: '#08080a' },
    { media: '(prefers-color-scheme: light)', color: '#ffffff' },
  ],
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    // suppressHydrationWarning e obrigatorio aqui: o script anti-FOUC escreve
    // data-theme no <html> antes da hidratacao, entao o atributo que o React
    // encontra nao e o que ele renderizou. O aviso e esperado e vale so para
    // este elemento - nao silencia nada abaixo dele.
    //
    // As variaveis de fonte entram no <html>, e nao no <body>: os tokens de
    // tipografia sao resolvidos em :root, entao precisam estar visiveis no
    // elemento raiz.
    <html lang="pt-BR" className={fontVariables} suppressHydrationWarning>
      <body>
        <ThemeProvider>
          {/*
            Primeiro no DOM, e portanto primeiro na ordem de tabulacao - essa
            posicao e a razao de o componente existir. Skip link depois da navbar
            nao pula nada.
          */}
          <SkipLink />
          <Navbar />

          {/*
            tabIndex={-1} nao e detalhe: sem ele o pulo do skip link move a
            rolagem mas nao o foco, e o proximo Tab volta para o topo da navbar -
            o atalho parece funcionar e nao funciona.

            pt-16 compensa a altura do header fixo, que saiu do fluxo.
          */}
          <main id={MAIN_ID} tabIndex={-1} className="pt-16">
            {children}
          </main>

          <Footer links={SOCIAL_LINKS} />
        </ThemeProvider>
      </body>
    </html>
  );
}
