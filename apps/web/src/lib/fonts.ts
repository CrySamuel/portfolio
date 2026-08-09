import localFont from 'next/font/local';

/**
 * Fontes da secao 7.3 do plano, servidas pelo proprio dominio.
 *
 * Zero requisicao a dominio de terceiro - a exigencia e de privacidade tanto
 * quanto de performance. Um <link> para fonts.gstatic.com entrega a cada
 * visitante o IP e o User-Agent dele a um terceiro, antes de qualquer consentimento.
 *
 * Os binarios ficam ao lado deste arquivo, e nao em public/. O next/font/local
 * le o arquivo no build e o emite em .next/static/media com hash de conteudo e
 * Cache-Control immutable. Em public/, o mesmo .woff2 tambem seria copiado
 * verbatim para a raiz do deploy - duas copias do mesmo binario no bundle, uma
 * delas sem hash e portanto sem cache de longo prazo.
 */

/**
 * Ajuste de metrica do fallback: e o que separa CLS 0.15 de CLS 0.
 *
 * O Next mede ascent, descent, line-gap e largura media do arquivo real e gera
 * um @font-face de fallback com size-adjust calculado, para que o texto ocupe a
 * mesma caixa antes e depois do swap. Sem isso, a troca desloca tudo que vem
 * abaixo.
 */
export const geistSans = localFont({
  src: './fonts/Geist-Variable-latin.woff2',
  // Fonte variavel: um arquivo cobre os pesos 100-900. Declarar a faixa evita
  // que o navegador sintetize negrito quando o peso pedido nao existir.
  weight: '100 900',
  style: 'normal',
  variable: '--font-geist-sans',

  // swap, nunca block: o texto aparece imediatamente na fonte de sistema. Com o
  // ajuste de metrica abaixo, essa primeira pintura ja esta na posicao final.
  display: 'swap',

  // Preload so aqui. Esta e a fonte do LCP - o titulo do hero -, entao vale
  // gastar uma conexao antecipada com ela.
  preload: true,

  fallback: ['system-ui', '-apple-system', 'Segoe UI', 'Arial', 'sans-serif'],
  adjustFontFallback: 'Arial',
});

export const jetbrainsMono = localFont({
  src: './fonts/JetBrainsMono-Variable-latin.woff2',
  weight: '100 800',
  style: 'normal',
  variable: '--font-jetbrains-mono',
  display: 'swap',

  // Sem preload: a mono aparece em codigo, versoes e rotulos tecnicos, nada
  // disso acima da dobra na home. Baixa-la junto com a sans competiria por
  // banda com o recurso do LCP para pintar algo que ninguem esta vendo ainda.
  preload: false,

  fallback: ['ui-monospace', 'SFMono-Regular', 'Cascadia Code', 'Consolas', 'monospace'],

  // Aqui o ajuste automatico e desligado de proposito, e a razao e o oposto da
  // que vale para a sans. O next/font so oferece 'Arial' ou 'Times New Roman'
  // como base do fallback ajustado - ambas proporcionais. Escalar Arial para a
  // altura da JetBrains Mono acerta a altura da linha e erra a largura de cada
  // caractere, e e a largura que decide onde a linha quebra: o bloco inteiro
  // muda de altura no swap. A cadeia de fallback acima e monoespacada de ponta
  // a ponta, com avanco proximo de 0.6em - o mesmo da JetBrains Mono -, entao
  // nao ajustar desloca menos do que ajustar errado.
  adjustFontFallback: false,
});

/**
 * Aplicado no <html>. Expoe as duas variaveis que packages/ui/src/styles/tokens.css
 * ja consome em --font-sans e --font-mono desde o commit 10.
 */
export const fontVariables = `${geistSans.variable} ${jetbrainsMono.variable}`;
