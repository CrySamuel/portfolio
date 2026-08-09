import type { Metadata } from 'next';
import type { ReactNode } from 'react';

export const metadata: Metadata = {
  title: 'Crystofer Demetino — Desenvolvedor Backend',
  description:
    'Portfólio de Crystofer Demetino, Desenvolvedor Backend especializado em Java e Spring Boot.',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="pt-BR" suppressHydrationWarning>
      <body>{children}</body>
    </html>
  );
}
