import type { ReactNode } from 'react';

import { SectionHeading } from '@/components/common/SectionHeading';
import { Container } from '@/components/layout/Container';
import { Section } from '@/components/layout/Section';
import { LanguageChart } from '@/features/github/components/LanguageChart';
import { RepositoryCard } from '@/features/github/components/RepositoryCard';
import { getGitHubStats, retratoVazio } from '@/lib/api/github';

const SECTION_ID = 'github';

/**
 * A secao de atividade no GitHub.
 *
 * <p>Server Component assincrono que busca o proprio dado, como as demais
 * (secao 8.5): a pagina nao precisa saber do que cada secao precisa.
 *
 * <p><strong>Ela some quando o GitHub esta fora, e essa e a decisao mais
 * importante do arquivo.</strong> A alternativa era um estado vazio explicito -
 * "GitHub temporariamente indisponivel". Ele contaria ao visitante uma falha
 * interna sobre a qual ele nao pode fazer nada, exatamente no momento em que o
 * site precisa parecer solido; e o ADR-0008 promete que <em>nenhuma falha do
 * GitHub alcanca o usuario</em>, o que inclui nao lhe dar a noticia. Sumir e o
 * que as outras secoes ja fazem quando nao ha conteudo, e o visitante que nunca
 * viu a secao nao sente falta dela.
 *
 * <p>O custo dessa escolha e conhecido: quem <em>ja tinha visto</em> a secao
 * percebe o sumico. Com o cache de seis horas do adaptador, a janela em que isso
 * acontece e curta - o retrato anterior continua servindo enquanto o circuito
 * esta aberto.
 *
 * <p><strong>E e por poder sumir que ela nao entra na navbar.</strong> A primeira
 * versao acrescentou {@code GitHub} ao {@code NAV_ITEMS}, e o teste com o GitHub
 * inalcancavel mostrou o preco: o link continuava la, apontando para um
 * {@code #github} que ja nao existia no documento. Um item de navbar e uma
 * afirmacao sobre a estrutura da pagina, e essa afirmacao nao pode depender de
 * terceiro. A secao mantem o {@code id}, entao link direto continua valendo.
 *
 * <p><strong>Nao ha grafico de contribuicoes, e a secao 16 do plano previa
 * um.</strong> O calendario dia a dia so existe no GraphQL do GitHub e nao esta
 * no contrato: o que a API publica e um total anual. Um numero nao vira mapa de
 * calor, entao ele entra como numero - e so quando ha token, porque sem token o
 * GraphQL nao responde e o total chega zero.
 */
export async function GitHubSection(): Promise<ReactNode> {
  const stats = await getGitHubStats();

  if (retratoVazio(stats)) return null;

  return (
    <Section id={SECTION_ID}>
      <Container className="flex flex-col gap-10">
        <SectionHeading
          sectionId={SECTION_ID}
          eyebrow="Atividade"
          title="No GitHub"
          description="Os números vêm da API do GitHub em tempo de execução, atrás de cache, disjuntor e retentativa — não de uma captura de tela."
        />

        <dl className="flex flex-wrap gap-x-10 gap-y-4">
          <Numero rotulo="Repositórios públicos" valor={stats.publicRepositories} />
          <Numero rotulo="Linguagens" valor={stats.languages.length} />

          {/*
            Zero aqui nao e "nenhuma contribuicao": e "sem token, e o GraphQL do
            GitHub nao responde sem ele". Desenhar o zero afirmaria sobre o dono
            algo que o dado nao diz.
          */}
          {stats.contributionsLastYear === 0 ? null : (
            <Numero rotulo="Contribuições no último ano" valor={stats.contributionsLastYear} />
          )}
        </dl>

        {stats.languages.length === 0 ? null : (
          <div className="flex flex-col gap-4">
            <h3 className="text-h4 text-fg">Linguagens</h3>
            <LanguageChart languages={stats.languages} />
          </div>
        )}

        {stats.repositories.length === 0 ? null : (
          <div className="flex flex-col gap-4">
            <h3 className="text-h4 text-fg">Repositórios em destaque</h3>

            {/* eslint-disable-next-line jsx-a11y/no-redundant-roles -- list-style: none medido; ver Timeline */}
            <ul role="list" className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {stats.repositories.map((repository) => (
                <RepositoryCard key={repository.name} repository={repository} />
              ))}
            </ul>
          </div>
        )}
      </Container>
    </Section>
  );
}

/**
 * Um numero com seu rotulo.
 *
 * <p>{@code <dt>} depois do {@code <dd>} na tela e antes dele no documento: a
 * ordem visual poe o numero em cima, e a ordem de leitura precisa do rotulo
 * primeiro para que "17" nao seja anunciado sozinho. `flex-col-reverse` resolve
 * as duas de uma vez, sem `order` em cada filho.
 */
function Numero({ rotulo, valor }: { readonly rotulo: string; readonly valor: number }): ReactNode {
  return (
    <div className="flex flex-col-reverse gap-1">
      <dt className="text-caption text-fg-subtle uppercase">{rotulo}</dt>
      <dd className="text-h3 text-fg tabular-nums">{valor}</dd>
    </div>
  );
}
