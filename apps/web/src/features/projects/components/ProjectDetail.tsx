import type { ProjectDetail as Projeto } from '@portfolio/api-client';
import { Button } from '@portfolio/ui';
import Link from 'next/link';
import type { ReactNode } from 'react';

import { Container } from '@/components/layout/Container';
import { ProjectMetrics } from '@/features/projects/components/ProjectMetrics';
import { TechStack } from '@/features/projects/components/TechStack';

/**
 * Locale e fuso fixos, como no {@code TimelineItem} - e o segundo pelo mesmo
 * motivo que la.
 *
 * <p>A API manda {@code 2026-03-24}, que o construtor de {@code Date} le como
 * meia-noite <em>UTC</em>. Formatado em {@code America/Sao_Paulo}, tres horas
 * atras, vira 23 de marco - e, quando o dia e o primeiro do mes, o mes inteiro
 * anda para tras. O defeito nao quebra teste nenhum e some em maquina que roda
 * em UTC, inclusive o runner do CI: ele so apareceria na tela de quem visita.
 *
 * <p>O formatador vive aqui e nao em {@code lib} porque as duas telas pedem
 * formas diferentes - a timeline abrevia o mes, a pagina de projeto o escreve
 * por extenso - e o que se repetiria e a armadilha, nao o codigo. Ela esta
 * escrita nos dois lugares de proposito.
 */
const FORMATADOR_DE_MES = new Intl.DateTimeFormat('pt-BR', {
  month: 'long',
  year: 'numeric',
  timeZone: 'UTC',
});

/**
 * A pagina de um projeto: problema, solucao e resultado.
 *
 * <p>Server Component, sem estado - custo zero no bundle do cliente. O unico
 * JavaScript que a rota carrega e o que o App Router ja traz.
 *
 * <p><strong>A estrutura e a mitigacao do risco do MVP 3.</strong> A secao 16 do
 * plano nomeia o risco: escrever case generico do tipo "fiz uma API REST". Os
 * tres titulos fixos forcam o formato que separa junior de pleno - o que doia,
 * o que foi construido, o que mudou depois -, e as metricas exigem numero. Sao
 * colunas {@code NOT NULL} no banco: um projeto sem narrativa nao entra no
 * catalogo, e a regra e do schema, nao da boa vontade de quem escreve.
 *
 * <p><strong>Os enderecos vivem aqui, e nao no card.</strong> O card precisa de
 * uma unica area de foco, entao {@code ProjectSummary} nem sequer carrega
 * {@code repoUrl} e {@code liveUrl} - o tipo torna o erro impossivel. Nesta
 * pagina nao ha link envolvendo tudo, entao os dois cabem como botoes.
 *
 * <p>Nenhum deles abre aba nova. Mudar o contexto sem avisar contraria o
 * criterio 3.2.5 da WCAG, e anunciar a mudanca em cada rotulo poluiria a leitura
 * de todos - e a mesma decisao ja tomada nos links do rodape.
 */
export function ProjectDetail({ project }: { project: Projeto }): ReactNode {
  const {
    title,
    summary,
    problem,
    solution,
    outcome,
    repoUrl,
    liveUrl,
    publishedAt,
    technologies,
    metrics,
  } = project;

  return (
    <Container className="flex flex-col gap-10 py-16 md:gap-12 md:py-24">
      {/*
        <article> porque a pagina e um conteudo autocontido - e o mesmo criterio
        que faz cada card ser um <article> dentro da lista.
      */}
      <article className="flex flex-col gap-10 md:gap-12">
        <header className="flex flex-col gap-5">
          {/*
            O caminho de volta, antes do titulo. Quem chega por link
            compartilhado nao passou pela listagem, entao nao tem "voltar" no
            historico que leve a ela.

            A trilha completa com JSON-LD BreadcrumbList e do commit 50, que e
            onde o plano poe os dados estruturados.
          */}
          <p className="text-body-sm">
            <Link href="/projetos" className="rounded-sm text-fg-muted hover:text-fg">
              <span aria-hidden>← </span>
              Todos os projetos
            </Link>
          </p>

          <div className="flex flex-col gap-3">
            {/*
              O <h1> da rota. Na home ele e o do hero; aqui e o titulo do
              projeto, e ha um so por pagina - navegacao por cabecalhos e ordem
              de leitura, nao tamanho de fonte.
            */}
            <h1 className="text-h1 text-balance">{title}</h1>

            <p className="max-w-reading text-body-lg text-pretty text-fg-muted">{summary}</p>
          </div>

          {/*
            A data e nulavel no contrato - "nulo quando nao ha data honesta a
            declarar" -, entao a ausencia e um caso normal e nao um erro. O
            <time> guarda o ISO para maquina e mostra o texto do locale.
          */}
          {publishedAt === null ? null : (
            <p className="text-body-sm text-fg-subtle">
              <time dateTime={publishedAt}>{FORMATADOR_DE_MES.format(new Date(publishedAt))}</time>
            </p>
          )}

          <TechStack technologies={technologies} label={`Tecnologias de ${title}`} />

          {/*
            Os dois enderecos sao nulaveis, e os dois casos sao reais: o FinAI
            tem repositorio e nao publica endereco, por decisao do dono - o bot
            responde a qualquer um que tenha o link, e cada conversa consome cota
            da chave dele e grava dado de terceiro no servidor dele.

            Sem endereco nenhum, nem o <div> aparece: uma barra de botoes vazia
            deixaria um vao no lugar de nada.
          */}
          {repoUrl === null && liveUrl === null ? null : (
            <div className="flex flex-wrap gap-3">
              {repoUrl === null ? null : (
                <Button asChild variant="outline">
                  <a href={repoUrl}>Ver o repositório</a>
                </Button>
              )}
              {liveUrl === null ? null : (
                <Button asChild variant="primary">
                  <a href={liveUrl}>Ver no ar</a>
                </Button>
              )}
            </div>
          )}
        </header>

        {/*
          Os tres blocos saem de uma lista, e nao de tres trechos escritos a mao.
          Repetir a marcacao tres vezes e como titulo e texto passam a divergir
          em espacamento no primeiro ajuste apressado.
        */}
        <div className="flex flex-col gap-8">
          {[
            { titulo: 'Problema', texto: problem },
            { titulo: 'Solução', texto: solution },
            { titulo: 'Resultado', texto: outcome },
          ].map((bloco) => (
            <section key={bloco.titulo} className="flex flex-col gap-2">
              {/*
                <section> sem nome acessivel de proposito: com aria-labelledby
                cada bloco viraria uma regiao na lista de landmarks, e tres
                regioes para tres paragrafos e ruido. O <h2> ja da a estrutura
                que a navegacao por cabecalhos usa.
              */}
              <h2 className="text-h3 text-fg">{bloco.titulo}</h2>
              <p className="max-w-reading text-body text-pretty text-fg-muted">{bloco.texto}</p>
            </section>
          ))}
        </div>

        {/*
          As metricas fecham a narrativa, logo depois do resultado que elas
          sustentam. Antes dele seriam numeros sem a frase que os explica.
        */}
        <ProjectMetrics metrics={metrics} label={`Números de ${title}`} />
      </article>
    </Container>
  );
}
