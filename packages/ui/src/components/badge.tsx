import { cva, type VariantProps } from 'class-variance-authority';
import type { ComponentPropsWithRef, ReactNode } from 'react';

import { cn } from '../lib/cn';

/**
 * Rotulo curto e nao interativo (secao 8.2): status, tecnologia, categoria.
 *
 * Nao ha variante de hover nem cursor de ponteiro de proposito. Badge que
 * parece clicavel e clicavel aos olhos de quem usa - se precisar de acao, o
 * componente certo e Button ou um link.
 *
 * As cores de estado saem dos tokens e ja vem validadas em contraste nos dois
 * temas. O fundo usa a mesma cor com opacidade, o que preserva a razao de
 * contraste do texto sobre qualquer superficie da paleta.
 */
const badgeVariants = cva(
  'inline-flex items-center gap-1 rounded-sm border font-medium whitespace-nowrap',
  {
    variants: {
      variant: {
        neutral: 'border-border bg-surface-2 text-fg-muted',
        accent: 'border-accent/25 bg-accent/10 text-accent',
        success: 'border-success/25 bg-success/10 text-success',
        warning: 'border-warning/25 bg-warning/10 text-warning',
        danger: 'border-danger/25 bg-danger/10 text-danger',
        outline: 'border-border-interactive bg-transparent text-fg',
      },
      size: {
        sm: 'h-5 px-1.5 text-caption',
        md: 'h-6 px-2 text-caption',
      },
    },
    defaultVariants: { variant: 'neutral', size: 'md' },
  },
);

export interface BadgeProps
  extends ComponentPropsWithRef<'span'>, VariantProps<typeof badgeVariants> {}

export function Badge({ className, variant, size, ...props }: BadgeProps): ReactNode {
  // <span>, e nao <div>: badge quase sempre aparece dentro de um paragrafo, de
  // um titulo ou de um item de lista, e um elemento de bloco ali seria HTML
  // invalido.
  return <span className={cn(badgeVariants({ variant, size }), className)} {...props} />;
}

export { badgeVariants };
