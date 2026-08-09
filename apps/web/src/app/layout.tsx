import type { Metadata } from 'next';
import type { ReactNode } from 'react';

import { fontVariables } from '@/lib/fonts';

import './globals.css';

export const metadata: Metadata = {
  title: 'Crystofer Demetino — Desenvolvedor Backend',
  description:
    'Portfólio de Crystofer Demetino, Desenvolvedor Backend especializado em Java e Spring Boot.',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    // As variaveis de fonte entram no <html>, e nao no <body>: os tokens de
    // tipografia sao resolvidos em :root, entao precisam estar visiveis no
    // elemento raiz.
    <html lang="pt-BR" className={fontVariables} suppressHydrationWarning>
      <body>{children}</body>
    </html>
  );
}
