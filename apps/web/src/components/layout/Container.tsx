import { cn } from '@portfolio/ui';
import type { ComponentPropsWithRef, ReactNode } from 'react';

/**
 * Largura maxima e respiro lateral (secao 7.4): 1200px, com padding de
 * 1rem / 1.5rem / 2rem conforme a tela cresce.
 *
 * E so isso - "sem estilo alem do layout", como manda a secao 8.3. Nao pinta
 * fundo, nao define tipografia, nao empilha nada. Quem precisa de grid ou de
 * gap coloca por className, porque essas decisoes pertencem a secao, nao a
 * caixa que a centraliza.
 *
 * Os breakpoints do plano (sm 640, md 768, lg 1024, xl 1280, 2xl 1536) sao
 * exatamente os padroes do Tailwind, entao nao ha o que configurar.
 */
export type ContainerProps = ComponentPropsWithRef<'div'>;

export function Container({ className, ...props }: ContainerProps): ReactNode {
  return (
    <div className={cn('mx-auto w-full max-w-page px-4 md:px-6 lg:px-8', className)} {...props} />
  );
}
