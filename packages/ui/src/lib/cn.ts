import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

/**
 * Combina classes condicionais e resolve conflitos do Tailwind.
 *
 * O clsx sozinho resolve o condicional mas nao o conflito: `clsx('p-2', 'p-4')`
 * devolve as duas classes, e qual vence passa a depender da ordem no CSS
 * gerado - ou seja, de acaso. O twMerge mantem apenas a ultima da mesma
 * familia.
 *
 * E o que torna `className` uma prop confiavel nos primitivos: quem compoe
 * consegue sobrescrever o estilo padrao sem recorrer a `!important`.
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
