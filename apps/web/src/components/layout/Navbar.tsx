'use client';

import Link from 'next/link';
import { useEffect, useRef, useState, type ReactNode } from 'react';

import { ThemeToggle } from '@/components/common/ThemeToggle';
import { Container } from '@/components/layout/Container';
import { MobileNav } from '@/components/layout/MobileNav';
import { useActiveSection } from '@/hooks/use-active-section';
import { NAV_IDS, NAV_ITEMS } from '@/lib/navigation';

/**
 * Navegação principal (seção 8.3).
 *
 * Client Component por dois motivos sem equivalente no servidor: a seção ativa
 * depende da posição da rolagem, e o fundo da barra muda quando a página sai do
 * topo.
 *
 * Nenhum dos dois usa listener de scroll. Os dois são IntersectionObserver -
 * avaliado fora da thread principal, acordando só quando o cruzamento muda. Um
 * handler de scroll rodaria dezenas de vezes por segundo exatamente durante a
 * rolagem, que é quando o INP é medido.
 */
export function Navbar(): ReactNode {
  const secaoAtiva = useActiveSection(NAV_IDS);
  const sentinela = useRef<HTMLDivElement>(null);
  const [rolou, setRolou] = useState(false);

  useEffect(() => {
    const alvo = sentinela.current;
    if (alvo === null) return;

    const observer = new IntersectionObserver(([entry]) => {
      setRolou(entry !== undefined && !entry.isIntersecting);
    });
    observer.observe(alvo);

    return () => {
      observer.disconnect();
    };
  }, []);

  return (
    <>
      {/*
        Sentinela de 1px no topo do documento: enquanto ela estiver visível, a
        página está no topo. É o que substitui o listener de scroll - e o header
        não pode observar a si mesmo, porque é fixo e nunca sai da viewport.
      */}
      <div ref={sentinela} aria-hidden="true" className="absolute top-0 h-px w-px" />

      <header
        className={[
          'fixed inset-x-0 top-0 z-40 border-b transition-colors duration-(--duration-base)',
          rolou ? 'border-border bg-bg/80 backdrop-blur-md' : 'border-transparent',
        ].join(' ')}
      >
        {/*
          "Principal" nomeia o landmark. Esta página tem três <nav> - este, o do
          menu mobile e o do rodapé -, e o leitor de tela lista todos: sem nome
          viram "navegação, navegação, navegação".
        */}
        <nav aria-label="Principal">
          <Container className="flex h-16 items-center gap-2">
            <Link
              href="/"
              className="mr-auto flex min-h-11 items-center rounded-md text-body font-medium text-fg"
            >
              Crystofer Demetino
            </Link>

            <ul className="hidden items-center gap-1 md:flex">
              {NAV_ITEMS.map((item) => (
                <li key={item.id}>
                  <a
                    href={`#${item.id}`}
                    // "location" é o valor para "onde estou dentro deste
                    // conjunto". "true" ou "page" diriam outra coisa: que este é
                    // o item atual de uma lista de páginas.
                    aria-current={secaoAtiva === item.id ? 'location' : undefined}
                    className="flex min-h-11 items-center rounded-md px-3 text-body-sm text-fg-muted transition-colors duration-(--duration-fast) hover:text-fg aria-[current]:text-accent"
                  >
                    {item.label}
                  </a>
                </li>
              ))}
            </ul>

            <ThemeToggle />
            <MobileNav items={NAV_ITEMS} activeId={secaoAtiva} />
          </Container>
        </nav>
      </header>
    </>
  );
}
