/**
 * O que toda imagem de compartilhamento do site tem em comum: o tamanho e as
 * cores.
 *
 * <p><strong>As cores sao os tokens da secao 7 do plano, escritos como
 * valor</strong> - e e a unica excecao a regra 7.8, "nenhum componente escreve
 * um valor hexadecimal". Ela e imposta pela ferramenta: estas imagens sao
 * desenhadas pelo satori, fora do navegador, sem folha de estilo e sem
 * {@code var()}. Nao ha CSS custom property para consultar.
 *
 * <p>Por isso ficam juntas e nomeadas aqui, e num lugar so: quando um token
 * mudar em {@code tokens.css}, o que precisa acompanhar esta neste arquivo, e
 * nao espalhado por cada imagem. Sao os valores do tema escuro, que e o padrao
 * do site.
 */
export const COR_DO_CARTAO = {
  fundo: '#08080a',
  texto: '#fafafa',
  apagado: '#a1a1aa',
  borda: '#232329',
  destaque: '#8b7cf6',
} as const;

/** O tamanho que Facebook, LinkedIn, WhatsApp e X esperam. */
export const TAMANHO_DO_CARTAO = { width: 1200, height: 630 } as const;

/**
 * O cartao do site - o {@code app/opengraph-image.tsx} - como endereco e texto
 * alternativo.
 *
 * <p>Fica aqui, e nao so no arquivo da imagem, porque tem dois leitores: o
 * proprio arquivo, que publica o {@code alt}, e o {@code metadataDaPagina}, que
 * precisa declarar a imagem nas paginas sem cartao proprio. O motivo de
 * declarar esta la.
 */
export const CARTAO_DO_SITE = {
  caminho: '/opengraph-image',
  alt: 'Cartão do portfólio, com o nome, o cargo e as principais competências',
} as const;
