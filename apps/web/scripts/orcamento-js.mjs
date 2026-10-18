// O orcamento de JavaScript da secao 10.1 do plano, medido no build: falha acima de 130 KB.
//
// Mede o que o navegador baixa de cada pagina pre-renderizada - todo <script src> do HTML, menos
// os polyfills, que vao com `noModule` e nenhum navegador moderno busca - comprimido com gzip no
// nivel maximo. E o metodo da secao 4.3 do ESTADO, e por isso nao e o numero do Lighthouse: o
// tamanho transferido depende da compressao do servidor (gzip no `next start`, Brotli na Vercel),
// e o orcamento e do codigo, nao do servidor que o serve. O relatorio do `next build` tambem nao
// serve - ele nao conta os chunks dos componentes de cliente do layout.
//
// Uso: node scripts/orcamento-js.mjs, depois de `next build`.
import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { gzipSync } from 'node:zlib';

const LIMITE_KB = 130;
const PAGINAS = [
  'pt-BR.html',
  'pt-BR/projetos.html',
  'pt-BR/projetos/finai.html',
  'en-US.html',
  'en-US/projetos/finai.html',
];

const RAIZ = '.next';
let estourou = false;

for (const pagina of PAGINAS) {
  const html = readFileSync(join(RAIZ, 'server', 'app', pagina), 'utf8');
  let total = 0;
  for (const [, src] of html.matchAll(/<script src="([^"]+)"/g)) {
    if (!src.startsWith('/_next/') || src.includes('polyfills')) continue;
    // O caminho vem codificado na URL - [locale] vira %5Blocale%5D. Sem decodificar, o arquivo
    // "nao existe" e some da soma em silencio: foi o que fez o metodo antigo medir 6,6 KB a menos.
    const arquivo = join(RAIZ, decodeURIComponent(src.split('?')[0]).replace('/_next/', ''));
    if (!existsSync(arquivo)) throw new Error(`chunk nao encontrado: ${src}`);
    total += gzipSync(readFileSync(arquivo), { level: 9 }).length;
  }
  const kb = total / 1024;
  const passou = kb <= LIMITE_KB;
  if (!passou) estourou = true;
  console.log(`${passou ? 'ok  ' : 'ACIMA'} ${kb.toFixed(1).padStart(6)} KB  ${pagina}`);
}

console.log(`limite: ${String(LIMITE_KB)} KB comprimidos por pagina`);
if (estourou) process.exit(1);
