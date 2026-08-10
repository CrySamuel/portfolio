import type { ReactNode } from 'react';

import { SocialLinks, type SocialLink } from '@/components/common/SocialLinks';
import { Container } from '@/components/layout/Container';

export interface FooterProps {
  links: readonly SocialLink[];
}

/**
 * Rodapé (seção 8.3).
 *
 * Server Component: só renderiza. Nada aqui depende de estado, efeito ou API do
 * navegador, então nada aqui precisa custar JavaScript no cliente.
 *
 * O ano vem de `new Date()` na renderização do servidor. Numa página estática
 * ele congela no momento do build - aceitável e desejado: a alternativa seria um
 * efeito no cliente, trocando bytes de JS por um número que muda uma vez por
 * ano. O commit 53 revisita isso se a revalidação do ISR não cobrir a virada.
 */
export function Footer({ links }: FooterProps): ReactNode {
  return (
    <footer className="border-t border-border">
      <Container className="flex flex-col items-center justify-between gap-4 py-8 sm:flex-row">
        <p className="text-body-sm text-fg-subtle">
          © {new Date().getFullYear()} Crystofer Demetino
        </p>

        {/*
          O <nav> do rodapé precisa de nome próprio: sem ele, o leitor de tela
          anuncia dois landmarks de navegação indistinguíveis.
        */}
        <nav aria-label="Perfis e contato">
          <SocialLinks links={links} />
        </nav>
      </Container>
    </footer>
  );
}
