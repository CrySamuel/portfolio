import type { Degrau } from '@/components/seo/structured-data';

/**
 * A trilha de navegacao da listagem, que e tambem o comeco da trilha de cada
 * projeto.
 *
 * <p>Mora no segmento, e nao na pagina, porque o Next so aceita de um
 * {@code page.tsx} as exportacoes da convencao - e as duas paginas precisam da
 * mesma lista. Duas copias divergiriam no primeiro ajuste de nome.
 */
export const DEGRAUS_DA_LISTAGEM: readonly Degrau[] = [
  { nome: 'Início', caminho: '/' },
  { nome: 'Projetos', caminho: '/projetos' },
];
