import { clsx, type ClassValue } from 'clsx';
import { extendTailwindMerge } from 'tailwind-merge';

/**
 * A escala tipografica da secao 7.3, declarada para o tailwind-merge.
 *
 * O merge decide o grupo de cada classe pelo prefixo. `text-` e ambiguo - serve
 * para tamanho e para cor -, e o desempate usa a lista de tamanhos conhecidos.
 * Os nomes daqui sao do projeto, nao do Tailwind, entao `text-body-sm` caia no
 * grupo de cor e passasse a competir com `text-white`: das duas, so a ultima
 * sobrevivia.
 *
 * O efeito era silencioso e generalizado. `cn('bg-accent-solid text-white',
 * 'text-body-sm')` devolvia so `text-body-sm`, e o botao primario herdava a cor
 * de texto do body em vez da declarada. Sem erro, sem aviso: apenas a cor
 * errada, em qualquer componente que combinasse tamanho e cor de texto.
 */
const FONT_SIZES = [
  'display',
  'h1',
  'h2',
  'h3',
  'h4',
  'body-lg',
  'body',
  'body-sm',
  'caption',
  'mono',
] as const;

const twMerge = extendTailwindMerge({
  extend: {
    classGroups: {
      'font-size': [{ text: [...FONT_SIZES] }],
    },
  },
});

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
