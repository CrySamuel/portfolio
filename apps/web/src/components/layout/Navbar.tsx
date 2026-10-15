'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect, useRef, useState, type ReactNode } from 'react';

import { ThemeToggle } from '@/components/common/ThemeToggle';
import { Container } from '@/components/layout/Container';
import { MobileNav } from '@/components/layout/MobileNav';
import { useActiveSection } from '@/hooks/use-active-section';
import type { Idioma } from '@/i18n/idiomas';
import { caminhoInterno, caminhoPublico } from '@/i18n/navigation';
import { NAV_IDS, type NavItem } from '@/lib/navigation';

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
 *
 * Os textos chegam prontos, por props, e o motivo e o orcamento de JS: o next-intl no navegador
 * traria o formatador de mensagens para repetir o que o layout ja fez no servidor.
 */
export interface NavbarProps {
  /** O nome do dono do site, que e o link para a home. Vem do perfil, e nao de um literal. */
  readonly nome: string;
  /** O endereco da home no idioma da pagina. */
  readonly inicio: string;
  readonly itens: readonly NavItem[];
  /**
   * Os outros idiomas, com os textos ja no idioma de cada um: "EN" e "Read in English" numa pagina
   * em portugues.
   */
  readonly idiomas: readonly {
    readonly locale: Idioma;
    readonly sigla: string;
    readonly rotulo: string;
  }[];
  readonly textos: {
    readonly principal: string;
    readonly abrirMenu: string;
    readonly fecharMenu: string;
    readonly menu: string;
    readonly mobile: string;
    readonly temaClaro: string;
    readonly temaEscuro: string;
  };
}

export function Navbar({ nome, inicio, itens, idiomas, textos }: NavbarProps): ReactNode {
  const secaoAtiva = useActiveSection(NAV_IDS);
  const paginaAtual = caminhoInterno(usePathname()).href;
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
        <nav aria-label={textos.principal}>
          <Container className="flex h-16 items-center gap-2">
            <Link
              href={inicio}
              className="mr-auto flex min-h-11 items-center rounded-md text-body font-medium text-fg"
            >
              {nome}
            </Link>

            <ul className="hidden items-center gap-1 md:flex">
              {itens.map((item) => (
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

            {/*
              A mesma pagina no outro idioma - o projeto em ingles, e nao a home em
              ingles. O caminho sai do endereco atual, que o servidor e o navegador
              veem de formas diferentes (ver caminhoInterno); as duas levam ao mesmo
              link. <a>, e nao <Link>: trocar de idioma troca o layout raiz, e a
              pagina carrega inteira de qualquer jeito.

              O nome acessivel comeca pelo texto visivel - "EN — Read in English" -,
              para quem navega por voz poder dizer o que ve (WCAG 2.5.3). O lang faz
              o leitor de tela trocar a pronuncia.
            */}
            {idiomas.map((idioma) => (
              <a
                key={idioma.locale}
                href={caminhoPublico({ href: paginaAtual, locale: idioma.locale })}
                hrefLang={idioma.locale}
                lang={idioma.locale}
                className="flex min-h-11 items-center rounded-md px-3 text-body-sm text-fg-muted transition-colors duration-(--duration-fast) hover:text-fg"
              >
                {idioma.sigla}
                <span className="sr-only"> — {idioma.rotulo}</span>
              </a>
            ))}

            <ThemeToggle rotulos={{ claro: textos.temaClaro, escuro: textos.temaEscuro }} />
            <MobileNav
              items={itens}
              activeId={secaoAtiva}
              textos={{
                abrir: textos.abrirMenu,
                fechar: textos.fecharMenu,
                menu: textos.menu,
                navegacao: textos.mobile,
              }}
            />
          </Container>
        </nav>
      </header>
    </>
  );
}
