import { Suspense, type ReactNode } from 'react';

import { SectionHeading } from '@/components/common/SectionHeading';
import { Container } from '@/components/layout/Container';
import { Section } from '@/components/layout/Section';
import { ProjectCard } from '@/features/projects/components/ProjectCard';
import { ProjectFilter } from '@/features/projects/components/ProjectFilter';
import { listProjects } from '@/lib/api/projects';
import { listarTecnologias } from '@/lib/filter-projects';
import type { ProjectSummary } from '@portfolio/api-client';

const SECTION_ID = 'projetos';

/**
 * A secao de projetos.
 *
 * <p>Server Component assincrono que busca o proprio dado, como as demais
 * secoes (secao 8.5). O {@code cache} do React desduplica a chamada, entao a
 * home e a listagem pedirem a mesma coisa nao dobra a requisicao.
 *
 * <p>Duas variantes, e a diferenca entre elas e o que o cliente paga. Em
 * <strong>destaques</strong> - a home - so os projetos marcados aparecem, sem
 * filtro, e nenhum byte de JavaScript e adicionado. Em <strong>catalogo</strong>
 * - a rota /projetos - entram todos e o filtro junto, que e o unico Client
 * Component do MVP 3.
 *
 * <p><strong>Os cards sao renderizados aqui, no servidor, e entregues prontos
 * ao filtro.</strong> Elemento React passado como prop atravessa a fronteira ja
 * renderizado; se o filtro importasse o card, ele e tudo o que ele usa entrariam
 * no bundle. E a diferenca entre a lista custar zero e custar tudo.
 *
 * <p>O {@code id} vem de {@code NAV_ITEMS}: a navbar aponta para
 * {@code #projetos} desde o commit 15, e ate agora o link nao levava a lugar
 * nenhum.
 */
export async function ProjectsSection({
  variante = 'destaques',
}: {
  readonly variante?: 'destaques' | 'catalogo';
} = {}): Promise<ReactNode> {
  const todos = await listProjects();
  const projects = variante === 'destaques' ? todos.filter((project) => project.featured) : todos;

  // Catalogo vazio nao renderiza cabecalho sozinho. A API distingue "sem
  // conteudo" de falha respondendo 200 com lista vazia, e a secao respeita isso
  // sumindo em vez de anunciar uma area que nao tem o que mostrar.
  if (projects.length === 0) return null;

  const catalogo = variante === 'catalogo';

  return (
    <Section id={SECTION_ID}>
      <Container className="flex flex-col gap-10">
        {/*
          Na rota /projetos esta secao e a pagina inteira, entao o titulo dela e
          o h1 - sem isso a pagina fica sem cabecalho de nivel 1, e foi o que o
          axe acusou (page-has-heading-one) antes de o commit ser publicado. Na
          home o h1 e do hero, e aqui o titulo volta a ser h2. Os cards
          acompanham: h2 no catalogo, h3 na home.
        */}
        <SectionHeading
          sectionId={SECTION_ID}
          as={catalogo ? 'h1' : 'h2'}
          eyebrow="Prova de trabalho"
          title="Projetos"
          description={
            catalogo
              ? 'Cada um com o problema que resolvia, o que foi construído e o que mudou depois.'
              : 'O que foi construído, com o problema que motivou e o resultado que veio depois.'
          }
        />

        {catalogo ? (
          /*
            `useSearchParams` obriga um limite de Suspense em pagina estatica -
            sem ele o Next recusa o build. O fallback e a propria lista sem
            filtro, entao o que aparece antes da hidratacao e o conteudo
            completo, e nao um esqueleto que pisca.
          */
          <Suspense fallback={<Grade projects={projects} nivel={2} />}>
            <ProjectFilter
              tecnologias={listarTecnologias(projects)}
              itens={projects.map((project) => ({
                slug: project.slug,
                tecnologias: project.technologies.map((tecnologia) => tecnologia.slug),
                card: <ProjectCard project={project} nivel={2} />,
              }))}
            />
          </Suspense>
        ) : (
          <Grade projects={projects} />
        )}
      </Container>
    </Section>
  );
}

/**
 * A lista sem filtro - usada nos destaques e como fallback do Suspense.
 *
 * O nivel acompanha o de quem chama, e nao um padrao proprio: como fallback ela
 * substitui a lista do filtro por um instante, e um h3 piscando no lugar de um
 * h2 mudaria a estrutura da pagina no meio do carregamento.
 */
function Grade({
  projects,
  nivel = 3,
}: {
  readonly projects: readonly ProjectSummary[];
  readonly nivel?: 2 | 3;
}): ReactNode {
  return (
    // eslint-disable-next-line jsx-a11y/no-redundant-roles -- list-style: none medido; ver Timeline
    <ul role="list" className="grid grid-cols-1 items-stretch gap-4 sm:grid-cols-2">
      {projects.map((project) => (
        <ProjectCard key={project.slug} project={project} nivel={nivel} />
      ))}
    </ul>
  );
}
