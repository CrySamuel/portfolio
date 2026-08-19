import { Badge } from '@portfolio/ui';
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
 * {@code string | null} e o perfil real nao preenche nenhum. O que sustenta o
 * card e o que sempre existe - nome, linguagem predominante e data do ultimo
 * push.
 *
 * <p><strong>Nao ha buraco, e isso foi medido antes de tentar consertar um.</strong>
 * A suspeita era que o {@code mt-auto} deixasse um vao entre o titulo e o rodape.
 * Ele nao deixa: sem descricao <em>nenhum</em> card tem o que empurrar, os seis
 * ficam com a mesma altura de 104px e o vao entre titulo e rodape e de 8px - o
 * card e compacto, e nao oco. O que faltava era peso de sinal, nao preenchimento,
 * e e por isso que a linguagem virou selo em vez de ganhar texto de enchimento.
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
        <dl className="mt-auto flex flex-wrap items-center gap-x-3 gap-y-2 pt-2 text-caption text-fg-subtle">
          {primaryLanguage === null ? null : (
            <div className="flex items-center gap-1.5">
              <dt className="sr-only">Linguagem predominante</dt>
              {/*
                Selo, e nao texto solto, e a razao foi medida. Sem descricao - o
                caso dos seis repositorios de hoje -, a linguagem e a unica
                informacao do card alem do nome, e estava no menor tamanho e na
                cor mais fraca da escala. Como selo ela vira o que se le ao
                varrer a grade: quatro Java e dois Python aparecem de relance, e
                a leitura conversa com a barra de linguagens logo acima.

                Variante `outline`, e nao `accent`: linguagem predominante e uma
                etiqueta, e nao um destaque - o mesmo criterio que manteve o
                nivel `basic` fora do selo aceso no SkillCard.
              */}
              <dd>
                <Badge variant="outline">{primaryLanguage}</Badge>
              </dd>
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
