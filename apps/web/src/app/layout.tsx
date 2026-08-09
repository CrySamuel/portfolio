import type { Metadata, Viewport } from 'next';
import type { ReactNode } from 'react';

import { ThemeToggle } from '@/components/common/ThemeToggle';
import { ThemeProvider } from '@/components/providers/ThemeProvider';
import { fontVariables } from '@/lib/fonts';

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
            Posicao provisoria. O lugar definitivo do botao e a navbar, que
            chega no commit 15 - ate la ele fica flutuando no canto, porque
            componente que nao e renderizado nao e componente verificado.
          */}
          <div className="fixed top-4 right-4 z-50">
            <ThemeToggle />
          </div>
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}
