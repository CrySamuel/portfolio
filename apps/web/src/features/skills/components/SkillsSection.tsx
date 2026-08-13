import type { ReactNode } from 'react';

import { SectionHeading } from '@/components/common/SectionHeading';
import { Container } from '@/components/layout/Container';
import { Section } from '@/components/layout/Section';
import { SkillCategory } from '@/features/skills/components/SkillCategory';
import { listSkills } from '@/lib/api/skills';

const SECTION_ID = 'skills';

/**
 * A secao de competencias, agrupadas por categoria.
 *
 * <p>Server Component assincrono que busca o proprio dado, como as demais secoes
 * (secao 8.5): a pagina nao precisa saber do que cada secao precisa.
 *
 * <p><strong>Nao ha agrupamento nem ordenacao aqui.</strong> Os dois chegam
 * prontos da API, que os recebe prontos do dominio. A F05 e explicita ao dizer
 * que agrupar e regra de negocio, e nao formatacao - refazer isso no componente
 * criaria um segundo lugar decidindo a mesma coisa.
 *
 * <p>O {@code id} vem de {@code NAV_ITEMS}: a navbar aponta para {@code #skills}
 * desde o commit 15, e ate agora o link nao levava a lugar nenhum.
 */
export async function SkillsSection(): Promise<ReactNode> {
  const categories = await listSkills();

  // Catalogo vazio nao renderiza cabecalho sozinho. A API distingue "sem
  // conteudo" de falha respondendo 200 com lista vazia, e a secao respeita isso
  // sumindo em vez de anunciar uma area que nao tem o que mostrar.
  if (categories.length === 0) return null;

  return (
    <Section id={SECTION_ID}>
      <Container className="flex flex-col gap-10">
        <SectionHeading
          sectionId={SECTION_ID}
          eyebrow="Competências"
          title="Skills"
          description="Nível declarado em texto, sem barra de percentual — a escala é curta de propósito."
        />

        <div className="flex flex-col gap-10">
          {categories.map((category) => (
            <SkillCategory key={category.name} category={category} />
          ))}
        </div>
      </Container>
    </Section>
  );
}
