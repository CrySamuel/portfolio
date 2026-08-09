import { Slot } from '@radix-ui/react-slot';
import type { ComponentPropsWithRef, ReactNode } from 'react';

import { cn } from '../lib/cn';

/**
 * Superficie contentora composta (secao 8.2).
 *
 * Compound component em vez de props: `<Card variant="withFooter" title=...>`
 * cresceria uma prop por arranjo possivel. Seis pecas nomeadas cobrem qualquer
 * combinacao, e a estrutura fica legivel no JSX de quem compoe (secao 8.7).
 *
 * Nenhuma das pecas tem margem propria - contrato 8.2, item 4. O espacamento
 * interno e feito por padding e gap, que pertencem ao container; margem
 * pertenceria a quem compoe, e e justamente a fonte de inconsistencia que a
 * regra elimina.
 *
 * A profundidade vem de superficie e borda, nao de sombra: no tema escuro
 * padrao, sombra preta sobre fundo quase preto e invisivel (secao 7.5).
 */
export type CardProps = ComponentPropsWithRef<'div'>;

export function Card({ className, ...props }: CardProps): ReactNode {
  return (
    <div
      className={cn('rounded-lg border border-border bg-surface text-fg', className)}
      {...props}
    />
  );
}

export function CardHeader({ className, ...props }: CardProps): ReactNode {
  return <div className={cn('flex flex-col gap-1.5 p-6', className)} {...props} />;
}

export interface CardTitleProps extends ComponentPropsWithRef<'h3'> {
  /**
   * Troca o <h3> pelo filho, preservando o estilo.
   *
   * O nivel certo depende de onde o card esta: dentro de uma secao com <h2>, o
   * h3 padrao encadeia; em outro contexto, pular ou repetir nivel quebra a
   * navegacao por titulos do leitor de tela (criterio 1.3.1). Sem esta saida, o
   * componente forcaria a hierarquia errada em metade dos usos.
   */
  asChild?: boolean;
}

export function CardTitle({ className, asChild = false, ...props }: CardTitleProps): ReactNode {
  const Component = asChild ? Slot : 'h3';
  return <Component className={cn('text-h3', className)} {...props} />;
}

export function CardDescription({ className, ...props }: ComponentPropsWithRef<'p'>): ReactNode {
  return <p className={cn('text-body-sm text-fg-muted', className)} {...props} />;
}

export function CardContent({ className, ...props }: CardProps): ReactNode {
  // Sem padding no topo: o header ja fechou o espaco acima. Quando o card
  // dispensa header, quem compoe recupera o respiro com className.
  return <div className={cn('p-6 pt-0', className)} {...props} />;
}

export function CardFooter({ className, ...props }: CardProps): ReactNode {
  return <div className={cn('flex items-center gap-3 p-6 pt-0', className)} {...props} />;
}
