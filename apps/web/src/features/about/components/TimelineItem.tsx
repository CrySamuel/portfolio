import { Badge } from '@portfolio/ui';
import type { Experience } from '@portfolio/api-client';
import type { ReactNode } from 'react';

/**
 * Locale fixo enquanto nao ha i18n.
 *
 * O `next-intl` so chega no commit 51, e ate la o site e monolingue. Usar `Intl`
 * agora, em vez de escrever os meses a mao, e o que torna aquele commit uma
 * troca de parametro: o mesmo codigo passa a produzir `Jan 2026` em en-US sem
 * ninguem traduzir nome de mes.
 */
const LOCALE = 'pt-BR';

/**
 * <strong>UTC explicito, e nao detalhe.</strong>
 *
 * A API manda `2026-01-01`, que o construtor de `Date` interpreta como meia-noite
 * <em>UTC</em>. Formatado no fuso de quem renderiza - `America/Sao_Paulo`, tres
 * horas atras - isso vira 31 de dezembro de 2025, e a data de inicio aparece um
 * mes antes do que deveria.
 *
 * Medido nesta maquina antes de escrever: sem `timeZone`, `2026-01-01` sai como
 * `dez. de 2025`. O defeito nao quebra nada, nao aparece em teste de tipo e some
 * em qualquer maquina que rode em UTC - inclusive o runner do CI. So apareceria
 * na tela de quem visita o site.
 */
const FORMATADOR_DE_MES = new Intl.DateTimeFormat(LOCALE, {
  month: 'short',
  year: 'numeric',
  timeZone: 'UTC',
});

/**
 * Uma passagem da timeline.
 *
 * Server Component: nao ha estado, efeito nem API de navegador aqui, entao o
 * componente custa **zero** no bundle do cliente. E a regra padrao da secao 3.6,
 * e ela importa mais neste ponto do projeto do que antes - restam cerca de 12 KB
 * do orcamento de 130 KB para animacao, i18n, formulario e analytics.
 *
 * O elemento e um `<li>` porque a lista mora no {@link Timeline}: e o pai que
 * decide a semantica de sequencia, e um item que se declarasse `<article>` solto
 * quebraria a contagem que o leitor de tela anuncia.
 */
export function TimelineItem({ experience }: { experience: Experience }): ReactNode {
  const { company, role, startDate, endDate, description, highlights } = experience;

  // A ausencia de data de saida **e** o que significa cargo atual - o mesmo
  // criterio da coluna, do dominio e do contrato. Nao ha booleano paralelo a
  // consultar, e por isso nao ha como os dois divergirem.
  const atual = endDate === null;

  return (
    <li className="relative pl-8">
      {/*
        O marcador na trilha. Preenchido no cargo atual, vazado nos encerrados -
        e a distincao visual que a Definition of Done pede.

        aria-hidden porque ele nao carrega informacao: quem diz "atual" para
        quem nao ve a cor e o texto do selo abaixo (WCAG 1.4.1). O ponto so
        repete, para quem enxerga, o que a frase ja garante.
      */}
      <span
        aria-hidden
        className={
          atual
            ? 'absolute top-1.5 -left-[6.5px] size-3 rounded-full border-2 border-accent bg-accent'
            : 'absolute top-1.5 -left-[6.5px] size-3 rounded-full border-2 border-border bg-bg'
        }
      />

      <div className="flex flex-col gap-3">
        <div className="flex flex-col gap-1.5">
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5">
            {/*
              h3 porque a secao Sobre assina o h2 (commit 28) e a pagina so tem
              um h1, no hero. Nivel de titulo e ordem de leitura, nao tamanho de
              fonte - pular de h2 para h4 confundiria a navegacao por cabecalhos.
            */}
            <h3 className="text-h4 text-fg">{role}</h3>
            {atual ? <Badge variant="accent">Atual</Badge> : null}
          </div>

          <p className="text-body text-fg-muted">{company}</p>

          {/*
            <time> com dateTime da a data em forma legivel por maquina, que e o
            que o JSON-LD do commit 50 vai consumir. O texto visivel fica no
            formato do locale; o atributo fica em ISO, como o padrao exige.
          */}
          <p className="text-body-sm text-fg-subtle">
            <time dateTime={startDate}>{formatarMes(startDate)}</time>
            {' – '}
            {endDate === null ? 'presente' : <time dateTime={endDate}>{formatarMes(endDate)}</time>}
          </p>
        </div>

        <p className="max-w-reading text-body text-pretty text-fg-muted">{description}</p>

        {/*
          A lista de destaques so aparece quando ha destaques. A coluna tem
          default '[]' justamente para que "sem destaques" seja um caso normal, e
          nao um nulo a tratar - entao aqui basta nao renderizar a lista vazia,
          que produziria um <ul> sem itens no HTML.

          Sem `role="list"` aqui, e a diferenca em relacao ao <ol> do Timeline e
          medivel: esta lista declara `list-disc`, entao conserva o marcador e a
          semantica que vem com ele. Declarar a role seria de fato redundante, e
          o eslint-plugin-jsx-a11y reprova - com razao neste caso.
        */}
        {highlights.length > 0 ? (
          <ul className="flex list-disc flex-col gap-1.5 pl-5">
            {highlights.map((destaque) => (
              <li key={destaque} className="max-w-reading text-body-sm text-pretty text-fg-muted">
                {destaque}
              </li>
            ))}
          </ul>
        ) : null}
      </div>
    </li>
  );
}

/** `2026-01-01` vira `jan. de 2026`, sempre em UTC - ver o comentario do formatador. */
function formatarMes(data: string): string {
  return FORMATADOR_DE_MES.format(new Date(data));
}
