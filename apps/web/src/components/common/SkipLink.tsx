import type { ReactNode } from 'react';

import { MAIN_ID } from '@/lib/navigation';

/**
 * "Pular para o conteúdo" — critério 2.4.1 da WCAG.
 *
 * Precisa ser o **primeiro elemento focável do DOM**, e é por isso que ele mora
 * no layout, antes da navbar. Quem navega por teclado ou leitor de tela atinge
 * a barra de navegação inteira antes do conteúdo em toda página que abre; sem
 * este atalho, são dezenas de Tab repetidos a cada visita.
 *
 * Fica invisível até receber foco, e aí aparece. Esconder com `display:none` ou
 * `visibility:hidden` tiraria da ordem de tabulação e o link nunca seria
 * alcançado - é o erro clássico deste componente, e o que o torna decorativo em
 * tantos sites.
 */
export function SkipLink(): ReactNode {
  return (
    <a
      href={`#${MAIN_ID}`}
      className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-50 focus:rounded-md focus:bg-accent-solid focus:px-4 focus:py-3 focus:text-body-sm focus:font-medium focus:text-white"
    >
      Pular para o conteúdo
    </a>
  );
}
