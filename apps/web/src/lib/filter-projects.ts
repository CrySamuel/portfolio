import type { ProjectSummary, Technology } from '@portfolio/api-client';

/**
 * O nome do parametro de busca que carrega o filtro.
 *
 * Fonte unica: o componente que escreve a URL e o que a le usam esta constante.
 * Duas strings soltas divergiriam no primeiro ajuste, e a divergencia e do tipo
 * que nao quebra nada - so faz o filtro parar de filtrar.
 */
export const PARAMETRO_TECNOLOGIA = 'tecnologia';

/**
 * As tecnologias que aparecem em algum projeto, sem repetir.
 *
 * <p>Derivada dos projetos, e nao pedida a API. Uma rota propria de tecnologias
 * devolveria tambem as que nenhum projeto declara, e o filtro passaria a
 * oferecer opcoes que so podem levar ao estado vazio.
 *
 * <p>A ordem sai dos proprios projetos, que ja chegam com as tecnologias
 * ordenadas por nome pelo dominio. Reordenar aqui seria um terceiro lugar
 * decidindo a mesma coisa; o que se faz e desduplicar preservando a ordem de
 * primeira aparicao, que para listas ja ordenadas e a ordem alfabetica.
 */
export function listarTecnologias(projetos: readonly ProjectSummary[]): Technology[] {
  const vistas = new Map<string, Technology>();

  for (const projeto of projetos) {
    for (const tecnologia of projeto.technologies) {
      if (!vistas.has(tecnologia.slug)) vistas.set(tecnologia.slug, tecnologia);
    }
  }

  return [...vistas.values()].sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'));
}

/**
 * Os projetos que declaram a tecnologia pedida.
 *
 * <p>`null` significa "sem filtro" e devolve tudo - o que e diferente de um slug
 * que nenhum projeto declara, que devolve lista vazia e leva ao estado vazio da
 * tela. Confundir os dois faria a pagina sem filtro parecer um resultado vazio.
 *
 * <p>Funcao pura, e por isso vive em `lib` e nao dentro do componente: ela e o
 * unico lugar onde a regra do filtro existe, e e testavel sem montar React.
 */
export function filtrarPorTecnologia<T extends { readonly tecnologias: readonly string[] }>(
  itens: readonly T[],
  slug: string | null,
): T[] {
  if (slug === null) return [...itens];
  return itens.filter((item) => item.tecnologias.includes(slug));
}

/**
 * Le o filtro de um conjunto de parametros de busca.
 *
 * <p>Devolve `null` para ausente e tambem para vazio: `?tecnologia=` e o que
 * sobra quando alguem apaga o valor a mao, e tratar isso como um slug faria a
 * pagina mostrar o estado vazio sem nenhum chip aceso.
 *
 * <p>Um slug que nao existe **nao** e corrigido para `null`. Ele leva ao estado
 * vazio de propósito: a URL foi compartilhada com aquele valor, e mostrar todos
 * os projetos faria a pagina contradizer o proprio endereco.
 */
export function lerFiltro(parametros: URLSearchParams | null): string | null {
  const valor = parametros?.get(PARAMETRO_TECNOLOGIA) ?? null;
  return valor === null || valor.length === 0 ? null : valor;
}
