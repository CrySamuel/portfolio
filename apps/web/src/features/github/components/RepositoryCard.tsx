import type { Repository } from '@portfolio/api-client';
import type { ReactNode } from 'react';

/**
 * Formata o dia do ultimo push.
 *
 * <p><strong>Fuso fixado em UTC, e a razao e um defeito classico.</strong> O
 * campo chega como {@code 2026-08-18}, e {@code new Date} de uma data sem hora
 * a interpreta como meia-noite UTC. Formatada no fuso local do visitante, que em
 * Sao Paulo e UTC-3, ela vira <em>17</em> de agosto - a data anda um dia para
 * tras para metade do planeta. Fixar o fuso na formatacao e o que garante que o
 * dia exibido seja o dia que a API disse.
 */
const FORMATO_DE_DATA = new Intl.DateTimeFormat('pt-BR', {
  timeZone: 'UTC',
  day: '2-digit',
  month: 'short',
  year: 'numeric',
});

/**
 * Um repositorio em destaque.
 *
 * <p>Server Component, sem estado - custo zero no bundle do cliente.
 *
 * <p><strong>O card e desenhado para sobreviver a `description` nula, porque
 * hoje ela e nula nos seis.</strong> O contrato declara o campo como
 * {@code string | null} e o perfil real nao preenche nenhum: um card que
 * dependesse da descricao ficaria com um buraco em todos eles. O que sustenta o
 * card e o que sempre existe - nome, linguagem predominante e data do ultimo
 * push.
 *
 * <p><strong>As estrelas so aparecem quando ha alguma.</strong> "0 estrelas" nao
 * e informacao, e desenhar o zero em seis cards seguidos transforma um dado
 * ausente numa afirmacao sobre o autor. Hoje nenhum aparece; quando um
 * repositorio for estrelado, o numero entra sozinho.
 *
 * <p>O link e externo e leva ao GitHub, entao {@code rel="noreferrer"} - o
 * destino nao precisa saber de onde veio o clique.
 */
export function RepositoryCard({ repository }: { repository: Repository }): ReactNode {
  const { name, description, url, primaryLanguage, stars, lastPushedAt } = repository;

  return (
    <li className="h-full">
      <article className="relative flex h-full flex-col gap-2 rounded-lg border border-border bg-surface p-5 transition-colors focus-within:border-border-interactive hover:border-border-interactive">
        {/*
          h4, e nao h3. O card e conteudo do grupo "Repositorios em destaque",
          que ja assina um h3 - com os dois no mesmo nivel, quem navega por
          cabecalhos ouve os seis repositorios como irmaos do titulo do grupo, e
          nao como o que ele contem. A regra `heading-order` do axe nao pega
          isto, porque nao ha salto de nivel: ela reprova h2 seguido de h4, e
          nao h3 seguido de h3. O que denuncia e ler o sumario da pagina.

          A classe continua `text-h4` nos dois casos: nivel e estrutura, tamanho
          e desenho.
        */}
        <h4 className="text-h4 text-fg">
          {/*
            O mesmo padrao de link esticado dos cards de projeto: o <a> envolve
            so o nome, e o pseudo-elemento cobre o card. Uma parada de teclado,
            com o nome do repositorio como nome acessivel.
          */}
          <a
            href={url}
            target="_blank"
            rel="noreferrer"
            className="rounded-sm after:absolute after:inset-0 after:content-['']"
          >
            {name}
          </a>
        </h4>

        {description === null ? null : <p className="text-body-sm text-fg-muted">{description}</p>}

        {/*
          mt-auto alinha o rodape dos cards de uma mesma linha do grid, mesmo com
          descricoes de tamanhos diferentes - ou sem descricao nenhuma, que e o
          caso de hoje.
        */}
        <dl className="mt-auto flex flex-wrap items-center gap-x-4 gap-y-1 pt-2 text-caption text-fg-subtle">
          {primaryLanguage === null ? null : (
            <div className="flex items-center gap-1.5">
              <dt className="sr-only">Linguagem predominante</dt>
              <dd>{primaryLanguage}</dd>
            </div>
          )}

          {stars === 0 ? null : (
            <div className="flex items-center gap-1.5">
              <dt className="sr-only">Estrelas</dt>
              <dd>{stars === 1 ? '1 estrela' : `${String(stars)} estrelas`}</dd>
            </div>
          )}

          <div className="flex items-center gap-1.5">
            <dt className="sr-only">Último push</dt>
            {/*
              <time> com o valor cru no dateTime: o texto e para quem le, o
              atributo e para quem processa. Sao a mesma data em dois formatos.
            */}
            <dd>
              <time dateTime={lastPushedAt}>
                {FORMATO_DE_DATA.format(new Date(`${lastPushedAt}T00:00:00Z`))}
              </time>
            </dd>
          </div>
        </dl>
      </article>
    </li>
  );
}
