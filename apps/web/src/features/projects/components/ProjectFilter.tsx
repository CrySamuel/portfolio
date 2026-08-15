'use client';

import type { Technology } from '@portfolio/api-client';
import { cn } from '@portfolio/ui';
import { useSearchParams } from 'next/navigation';
import { Fragment, useCallback, useEffect, useState, type ReactNode } from 'react';

import { PARAMETRO_TECNOLOGIA, filtrarPorTecnologia, lerFiltro } from '@/lib/filter-projects';

/**
 * Um projeto ja renderizado, com o que o filtro precisa saber sobre ele.
 *
 * <p><strong>`card` chega pronto do servidor.</strong> E o que mantem o
 * `ProjectCard` e o `TechStack` como Server Components mesmo sendo exibidos por
 * um Client Component: elemento React passado como prop atravessa a fronteira ja
 * renderizado, e nao vira codigo no bundle. Importar o card aqui teria o efeito
 * oposto - ele e tudo o que ele usa entrariam no JS do cliente.
 */
export interface ItemFiltravel {
  readonly slug: string;
  readonly tecnologias: readonly string[];
  readonly card: ReactNode;
}

/**
 * O filtro por tecnologia, e a lista que ele governa.
 *
 * <p>Este e o unico Client Component do MVP 3, e a fronteira foi desenhada para
 * ser a menor possivel: ele nao conhece projeto, nao busca dado e nao sabe
 * desenhar card. Recebe cards prontos e decide quais mostrar.
 *
 * <p><strong>Sem requisicao de rede ao filtrar</strong>, que e o que a secao 16
 * exige. A pagina e estatica e ja trouxe todos os projetos; clicar num chip
 * troca estado local e reescreve a URL com `history.pushState`, sem navegacao.
 * A alternativa - ler `searchParams` no servidor - tornaria a pagina dinamica e
 * devolveria ao visitante a espera pelo cold start da API, que e exatamente o
 * que o ISR existe para evitar.
 *
 * <p><strong>Botoes, e nao links, e o custo disso esta assumido.</strong> Um
 * `<a href="?tecnologia=java">` seria rastreavel por buscador; um botao com
 * `aria-pressed` diz ao leitor de tela que aquilo e um estado que liga e desliga,
 * que e o que de fato acontece. A indexacao por filtro exigiria uma pagina por
 * tecnologia, e ela nao existiria de graca: a pagina precisaria ser dinamica ou
 * pre-gerada por slug, e nenhum dos dois esta no plano. O endereco continua
 * compartilhavel, que e o outro metade do requisito.
 */
export function ProjectFilter({
  tecnologias,
  itens,
}: {
  readonly tecnologias: readonly Technology[];
  readonly itens: readonly ItemFiltravel[];
}): ReactNode {
  const parametros = useSearchParams();

  /*
    O estado nasce da URL e depois governa a tela. Ler `searchParams` a cada
    render funcionaria no primeiro carregamento e nao no clique, porque
    `pushState` nao passa pelo roteador do Next.
  */
  const [selecionada, setSelecionada] = useState<string | null>(() => lerFiltro(parametros));

  const escolher = useCallback((slug: string | null) => {
    setSelecionada(slug);

    const url = new URL(window.location.href);
    if (slug === null) url.searchParams.delete(PARAMETRO_TECNOLOGIA);
    else url.searchParams.set(PARAMETRO_TECNOLOGIA, slug);

    // pushState, e nao replaceState: cria entrada no historico, entao Voltar
    // desfaz o filtro em vez de sair da pagina.
    window.history.pushState(null, '', url);
  }, []);

  /*
    O listener existe porque a falta dele foi medida.

    A primeira versao dispensou popstate como "peca a mais". Com pushState
    criando entradas de historico e ninguem ouvindo a volta, apertar Voltar
    mudava a URL para o filtro anterior e deixava a tela no filtro atual - o
    endereco na barra passava a descrever outra coisa que nao a pagina, e quem
    copiasse aquela URL compartilharia um resultado que nunca viu.

    A alternativa seria `replaceState`, que nao cria entradas e portanto nao tem
    como divergir. Perde-se o Voltar desfazendo o filtro, que e o gesto que
    qualquer pessoa tenta primeiro.
  */
  useEffect(() => {
    const aoNavegarNoHistorico = (): void => {
      setSelecionada(lerFiltro(new URLSearchParams(window.location.search)));
    };

    window.addEventListener('popstate', aoNavegarNoHistorico);
    return () => {
      window.removeEventListener('popstate', aoNavegarNoHistorico);
    };
  }, []);

  const visiveis = filtrarPorTecnologia(itens, selecionada);

  return (
    <div className="flex flex-col gap-6">
      {/* eslint-disable-next-line jsx-a11y/no-redundant-roles -- list-style: none medido; ver Timeline */}
      <ul role="list" aria-label="Filtrar por tecnologia" className="flex flex-wrap gap-2">
        <li>
          <Chip
            aceso={selecionada === null}
            onClick={() => {
              escolher(null);
            }}
          >
            Todos
          </Chip>
        </li>
        {tecnologias.map((tecnologia) => (
          <li key={tecnologia.slug}>
            <Chip
              aceso={selecionada === tecnologia.slug}
              onClick={() => {
                escolher(tecnologia.slug);
              }}
            >
              {tecnologia.name}
            </Chip>
          </li>
        ))}
      </ul>

      {/*
        `aria-live="polite"` porque o conteudo troca sem navegacao: sem ele, quem
        usa leitor de tela clica no chip e nao recebe nada, ja que o foco
        permanece no botao e a lista muda em silencio.
      */}
      <div aria-live="polite">
        {visiveis.length === 0 ? (
          <p className="rounded-lg border border-dashed border-border px-5 py-8 text-center text-body text-fg-muted">
            Nenhum projeto com essa tecnologia.
          </p>
        ) : (
          // eslint-disable-next-line jsx-a11y/no-redundant-roles -- list-style: none medido
          <ul role="list" className="grid grid-cols-1 items-stretch gap-4 sm:grid-cols-2">
            {/*
              Fragment, e nao <li>: o card ja renderiza o proprio <li>, e
              envolve-lo produziria <li> dentro de <li> - HTML invalido, e uma
              lista que o leitor de tela anuncia com o dobro de itens.
            */}
            {visiveis.map((item) => (
              <Fragment key={item.slug}>{item.card}</Fragment>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

/**
 * Um chip do filtro.
 *
 * <p>`aria-pressed` e o que comunica o estado a quem nao ve a cor - o mesmo
 * criterio 1.4.1 que fez o nivel das competencias virar texto. A cor e reforco.
 */
function Chip({
  aceso,
  onClick,
  children,
}: {
  readonly aceso: boolean;
  readonly onClick: () => void;
  readonly children: ReactNode;
}): ReactNode {
  return (
    <button
      type="button"
      aria-pressed={aceso}
      onClick={onClick}
      className={cn(
        'inline-flex h-11 items-center rounded-full border px-4 text-body-sm transition-colors',
        aceso
          ? // text-white, e nao um token de primeiro plano. A primeira versao
            // escreveu `text-fg-on-accent`, e esse token nao existe: o Tailwind
            // nao gera classe para variavel ausente, entao a regra simplesmente
            // nao entrava no CSS e o chip herdava a cor do corpo. No tema escuro
            // isso da branco sobre violeta e passa despercebido; no claro da
            // #09090b sobre #5b4bd6 - **3,24:1**, medido, contra os 4,5:1 que a
            // WCAG 1.4.3 exige. E o mesmo par que o Button primary usa, e
            // tokens.css anota o contraste dele: 6,14:1.
            'border-accent-solid bg-accent-solid text-white'
          : 'border-border text-fg-muted hover:border-border-interactive hover:text-fg',
      )}
    >
      {children}
    </button>
  );
}
