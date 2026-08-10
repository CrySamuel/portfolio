'use client';

import { Button } from '@portfolio/ui';
import { useEffect, useRef, useState, type ReactNode } from 'react';

import type { NavItem } from '@/lib/navigation';

const DIALOG_ID = 'menu-mobile';

export interface MobileNavProps {
  items: readonly NavItem[];
  activeId: string | null;
}

/**
 * Menu de navegação em painel lateral.
 *
 * O plano previa o `Sheet` do Radix (seção 8.2). Medido antes de escrever:
 * `@radix-ui/react-dialog` leva o First Load JS de 113,3 KB para 126,1 KB
 * comprimidos - 12,8 KB para um menu que só aparece abaixo de 768px. O limite
 * rígido da seção 10.1 é 130 KB, então sobrariam 3,9 KB para Framer Motion,
 * next-intl, Turnstile, formulário de contato e analytics. A conta não fecha.
 *
 * O `<dialog>` nativo entrega de graça exatamente o que o Radix empacota:
 *
 *   focus trap ....... `showModal()` prende o Tab dentro do diálogo
 *   Esc .............. evento `cancel` nativo, sem listener de teclado
 *   inertização ...... o resto da página sai da árvore de acessibilidade
 *   camada superior .. top layer, imune a z-index e a `overflow: hidden`
 *   backdrop ......... pseudo-elemento `::backdrop`
 *   foco no retorno .. volta para o gatilho ao fechar
 *
 * Custo: 0 KB. Sobra para o CSS o bloqueio de rolagem do fundo, feito em
 * `body:has(dialog[open])` no globals.css, e a transição de entrada, que usa
 * `@starting-style` e degrada para aparecer sem animação onde não houver
 * suporte.
 */
export function MobileNav({ items, activeId }: MobileNavProps): ReactNode {
  const dialogo = useRef<HTMLDialogElement>(null);
  const [aberto, setAberto] = useState(false);

  useEffect(() => {
    const el = dialogo.current;
    if (el === null) return;

    // Esc é tratado pelo navegador e não passa pelo React. Sem ouvir `close`, o
    // `aria-expanded` do gatilho ficaria mentindo depois do primeiro Esc.
    const aoFechar = (): void => {
      setAberto(false);
    };

    // Fechar clicando fora. O ::backdrop não é alcançável pelo JavaScript, mas o
    // clique nele tem como alvo o próprio <dialog> - o conteúdo interno é que
    // reporta os filhos. Comparar o alvo com o elemento é o teste exato.
    //
    // O listener fica aqui, e não numa prop onClick do JSX, porque a regra
    // jsx-a11y/click-events-have-key-events exigiria um handler de teclado ao
    // lado. Exigência correta no caso geral - clique sem equivalente de teclado
    // é armadilha -, mas aqui o equivalente já existe e é nativo: Esc. Um
    // onKeyDown redundante calaria o lint sem acrescentar nada a quem usa.
    const aoClicar = (event: MouseEvent): void => {
      if (event.target === el) el.close();
    };

    el.addEventListener('close', aoFechar);
    el.addEventListener('click', aoClicar);
    return () => {
      el.removeEventListener('close', aoFechar);
      el.removeEventListener('click', aoClicar);
    };
  }, []);

  return (
    <>
      <Button
        variant="ghost"
        size="icon"
        className="md:hidden"
        aria-haspopup="dialog"
        aria-expanded={aberto}
        aria-controls={DIALOG_ID}
        onClick={() => {
          dialogo.current?.showModal();
          setAberto(true);
        }}
      >
        <IconeMenu />
        <span className="sr-only">Abrir menu de navegação</span>
      </Button>

      <dialog ref={dialogo} id={DIALOG_ID} aria-label="Menu de navegação" className={CN_DIALOGO}>
        <nav aria-label="Navegação mobile" className="flex h-full flex-col gap-2 p-4">
          <Button
            variant="ghost"
            size="icon"
            className="self-end"
            onClick={() => {
              dialogo.current?.close();
            }}
          >
            <IconeFechar />
            <span className="sr-only">Fechar menu</span>
          </Button>

          <ul className="flex flex-col gap-1">
            {items.map((item) => (
              <li key={item.id}>
                <a
                  href={`#${item.id}`}
                  aria-current={activeId === item.id ? 'location' : undefined}
                  // Fecha antes de navegar. O foco volta ao gatilho pelo
                  // comportamento nativo, e a âncora leva a rolagem ao destino.
                  onClick={() => {
                    dialogo.current?.close();
                  }}
                  className="flex min-h-11 items-center rounded-md px-3 text-body text-fg-muted transition-colors duration-(--duration-fast) hover:bg-surface-2 hover:text-fg aria-[current]:text-accent"
                >
                  {item.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>
      </dialog>
    </>
  );
}

/**
 * Painel colado à direita, ocupando a altura toda.
 *
 * O `<dialog>` vem centrado por padrão, com margem automática e altura máxima
 * calculada - por isso as primeiras utilidades desfazem o padrão antes de
 * posicionar. `h-dvh` e não `h-screen`: no mobile a barra do navegador some ao
 * rolar, e `100vh` deixaria o rodapé do painel fora da tela.
 */
const CN_DIALOGO = [
  'm-0 ml-auto h-dvh max-h-dvh w-4/5 max-w-xs p-0',
  'border-l border-border bg-surface text-fg',
  'backdrop:bg-black/60',
  // Entrada pela direita. `transition-discrete` é o que permite animar um
  // elemento que sai de `display: none` - sem ele o painel apenas aparece.
  'translate-x-full opacity-0 open:translate-x-0 open:opacity-100',
  'starting:open:translate-x-full starting:open:opacity-0',
  'transition-all transition-discrete duration-(--duration-base) ease-(--ease-out)',
].join(' ');

function IconeMenu(): ReactNode {
  return (
    <svg
      aria-hidden="true"
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
    >
      <path d="M4 7h16M4 12h16M4 17h16" />
    </svg>
  );
}

function IconeFechar(): ReactNode {
  return (
    <svg
      aria-hidden="true"
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
    >
      <path d="m6 6 12 12M18 6 6 18" />
    </svg>
  );
}
