import type { ProjectMetric } from '@portfolio/api-client';
import type { ReactNode } from 'react';

/**
 * Os numeros que sustentam o resultado de um projeto.
 *
 * <p>Server Component, sem estado - custo zero no bundle do cliente.
 *
 * <p><strong>A marcacao e uma lista de definicoes, e nao uma grade de caixas.</strong>
 * Cada metrica e um par rotulo/valor - "Economia em um mes" / "R$ 800+" -, que e
 * exatamente o que {@code <dl>} descreve. Com {@code <div>} e texto solto, quem
 * usa leitor de tela ouviria seis pedacos sem saber quais dois andam juntos; com
 * a lista, o par e anunciado como par. O {@code <div>} entre a {@code <dl>} e
 * cada {@code <dt>}/{@code <dd>} e permitido pela especificacao justamente para
 * agrupar, e e o que permite estilizar cada metrica como um cartao.
 *
 * <p><strong>O rotulo vem antes do valor, e nao o contrario.</strong> A ordem
 * visual comum em painel de metricas poe o numero grande em cima; aqui isso
 * exigiria inverter a ordem por CSS, deixando a leitura visual e a do DOM em
 * sentidos diferentes por um ganho puramente estetico. Rotulo em cima le igual e
 * nao cria a divergencia.
 *
 * <p>Lista vazia nao renderiza nada. O contrato declara {@code metrics} como
 * array sempre presente - "lista vazia quando nao ha" -, entao a ausencia e um
 * caso normal, e nao um erro a sinalizar. Um {@code <dl>} sem itens seria uma
 * regiao anunciada e vazia.
 *
 * @param label nome acessivel da lista; a pagina passa o titulo do projeto para
 *   que o leitor de tela diga de quem sao os numeros
 */
export function ProjectMetrics({
  metrics,
  label,
}: {
  readonly metrics: readonly ProjectMetric[];
  readonly label: string;
}): ReactNode {
  if (metrics.length === 0) return null;

  return (
    <dl aria-label={label} className="grid grid-cols-1 gap-3 sm:grid-cols-2">
      {metrics.map((metric) => (
        <div
          key={metric.label}
          className="flex flex-col gap-1 rounded-lg border border-border bg-surface p-4"
        >
          <dt className="text-body-sm text-fg-muted">{metric.label}</dt>
          {/*
            O valor chega com a unidade dentro ("R$ 800+"), por decisao do
            contrato: separar numero e unidade em duas colunas obrigaria o front
            a remonta-los, e a remontagem e onde "800 R$" nasce.

            tabular-nums para que trocar um valor nao mude a largura do cartao.
          */}
          <dd className="font-mono text-h3 text-fg tabular-nums">{metric.value}</dd>
        </div>
      ))}
    </dl>
  );
}
