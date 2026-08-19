import type { LanguageShare } from '@portfolio/api-client';
import type { ReactNode } from 'react';

/**
 * Abaixo de 1% a fatia deixa de ser desenhavel.
 *
 * Nao e regra de gosto: numa barra de 320px - a largura util de um telefone -
 * 1% da 3,2px, que e da ordem do vao entre as fatias. Abaixo disso a fatia some
 * na barra e a linha dela na legenda gasta uma linha inteira para nao informar
 * nada. No perfil real sao **nove** linguagens nessa faixa, e cinco delas estao
 * abaixo de 0,03%.
 */
const FATIA_MINIMA = 1;

/**
 * Seis nomeadas, no maximo - o tamanho da paleta categorica.
 *
 * O limite e da paleta, e nao do desenho: os seis tons de `tokens.css` foram
 * validados como conjunto, e um setimo teria de ser inventado em tempo de
 * render. Cor gerada por indice e exatamente o que quebra a separacao sob
 * daltonismo que o conjunto garante.
 */
const FATIAS_NOMEADAS = 6;

const FORMATO = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

interface Fatia {
  readonly nome: string;
  readonly share: number;
  readonly cor: string;
  /** Quantas linguagens a fatia representa. Maior que 1 so na cauda agrupada. */
  readonly linguagens: number;
}

/**
 * A barra de linguagens do perfil, em CSS puro, e a legenda que a explica.
 *
 * <p><strong>Barra empilhada horizontal, e nao rosca.</strong> O dado e
 * parte-do-todo com categoria de nome longo, e nessa combinacao a barra e a
 * forma que aceita rotulo sem cruzar linha de chamada. E tambem a forma que o
 * proprio GitHub usa, o que poupa o visitante de aprender a ler o grafico.
 *
 * <p><strong>Sem SVG, e a secao 16 do plano pedia SVG.</strong> A intencao ali
 * era nao carregar biblioteca de grafico - ~45 KB -, e isso continua valendo:
 * isto aqui custa zero. O que nao sobrevive ao SVG e o vao de 2px entre as
 * fatias: largura em porcentagem e vao em pixel nao convivem no mesmo
 * {@code viewBox} sem {@code calc()} em propriedade de geometria, que e CSS de
 * SVG 2. Em {@code flex}, {@code gap: 2px} com base percentual resolve exato, e
 * o navegador distribui a sobra entre as fatias.
 *
 * <p><strong>A barra e decorativa, e a legenda e o conteudo.</strong> Nao ha
 * alternativa textual escondida para leitor de tela: a legenda escreve nome e
 * porcentagem de cada fatia, visivel para todo mundo. E o que a WCAG 1.4.1 pede
 * e o que a validacao da paleta exige - tres dos seis tons ficam abaixo de 3:1
 * contra a superficie clara, e a identidade nunca pode depender so da cor.
 */
export function LanguageChart({ languages }: { languages: readonly LanguageShare[] }): ReactNode {
  const fatias = agruparCauda(languages);
  if (fatias.length === 0) return null;

  return (
    <div className="flex flex-col gap-4">
      {/*
        aria-hidden porque a legenda abaixo diz tudo o que esta barra mostra.
        Sem isso, quem usa leitor de tela ouviria a mesma informacao duas vezes -
        uma delas como uma fileira de elementos sem nome.
      */}
      <div
        aria-hidden="true"
        className="flex h-3 w-full gap-0.5 overflow-hidden rounded-full bg-surface-2"
      >
        {fatias.map((fatia) => (
          <span
            key={fatia.nome}
            // A largura e a fatia, e o flex-shrink deixa o navegador tirar os
            // vaos proporcionalmente de todas elas. Sem o shrink, a soma das
            // bases mais os vaos passaria de 100% e a ultima fatia sairia da
            // barra.
            style={{ flexBasis: `${String(fatia.share)}%`, backgroundColor: fatia.cor }}
            className="min-w-0 shrink"
          />
        ))}
      </div>

      {/* eslint-disable-next-line jsx-a11y/no-redundant-roles -- list-style: none medido; ver Timeline */}
      <ul role="list" className="grid grid-cols-2 gap-x-6 gap-y-2 sm:grid-cols-3">
        {fatias.map((fatia) => (
          <li key={fatia.nome} className="flex items-center gap-2 text-body-sm">
            <span
              aria-hidden="true"
              style={{ backgroundColor: fatia.cor }}
              className="size-2.5 shrink-0 rounded-full"
            />
            <span className="min-w-0 truncate text-fg">{rotular(fatia)}</span>
            {/*
              A porcentagem em text-fg-muted, e nao na cor da fatia: texto veste
              token de texto. Colorir o numero com a cor da serie derruba o
              contraste dele para o do proprio grafico, que nao foi medido como
              texto.
            */}
            <span className="ml-auto shrink-0 text-fg-muted tabular-nums">
              {FORMATO.format(fatia.share)}%
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

/** "Outras (9)" diz quantas foram dobradas; uma linguagem nomeada nao ganha sufixo. */
function rotular(fatia: Fatia): string {
  return fatia.linguagens > 1 ? `${fatia.nome} (${String(fatia.linguagens)})` : fatia.nome;
}

/**
 * As linguagens desenhaveis, com a cauda dobrada numa fatia neutra.
 *
 * <p>A ordem que chega e mantida - ela e do dominio, e reordenar aqui seria um
 * segundo lugar decidindo a mesma coisa.
 *
 * <p><strong>Quando sobra uma linguagem so, ela nao vira "Outras (1)".</strong>
 * A ultima posicao e sempre a do tom neutro; o que muda e o rotulo, que passa a
 * ser o nome dela. Agrupar um item so seria esconder um nome atras de uma
 * palavra mais longa do que ele.
 */
function agruparCauda(languages: readonly LanguageShare[]): readonly Fatia[] {
  const cores = [
    'var(--chart-1)',
    'var(--chart-2)',
    'var(--chart-3)',
    'var(--chart-4)',
    'var(--chart-5)',
    'var(--chart-6)',
  ];

  const nomeadas = languages
    .filter((linguagem) => linguagem.share >= FATIA_MINIMA)
    .slice(0, FATIAS_NOMEADAS);

  const cauda = languages.slice(nomeadas.length);

  const fatias: Fatia[] = nomeadas.map((linguagem, indice) => ({
    nome: linguagem.name,
    share: linguagem.share,
    // O indice e sempre menor que FATIAS_NOMEADAS por causa do slice acima, e
    // cores tem exatamente esse tamanho.
    cor: cores[indice] ?? 'var(--chart-rest)',
    linguagens: 1,
  }));

  if (cauda.length > 0) {
    fatias.push({
      nome: cauda.length === 1 ? (cauda[0]?.name ?? 'Outras') : 'Outras',
      share: cauda.reduce((soma, linguagem) => soma + linguagem.share, 0),
      cor: 'var(--chart-rest)',
      linguagens: cauda.length,
    });
  }

  return fatias;
}
