import type { SkillCategory as SkillCategoryData } from '@portfolio/api-client';
import type { ReactNode } from 'react';

import { SkillCard } from '@/features/skills/components/SkillCard';

/**
 * Um grupo de competencias, com seu cabecalho.
 *
 * <p>O {@code h3} fecha a hierarquia: a pagina tem um h1 no hero, a seccao assina
 * o h2, e cada categoria e um h3. Nivel de titulo e ordem de leitura, e nao
 * tamanho de fonte - quem navega por cabecalhos usa isso como sumario.
 *
 * <p><strong>{@code role="list"} outra vez, e pela mesma razao medida no
 * Timeline.</strong> O preflight do Tailwind aplica {@code list-style: none}, e o
 * WebKit remove a semantica de lista de elemento sem marcador. Sem o atributo, o
 * leitor de tela deixa de anunciar quantas competencias ha no grupo.
 *
 * <p>O grid vai de uma a tres colunas conforme a largura, que e o terceiro aceite
 * da F05. Uma coluna no telefone porque nome e selo ja ocupam a linha inteira.
 */
export function SkillCategory({ category }: { category: SkillCategoryData }): ReactNode {
  return (
    <div className="flex flex-col gap-4">
      <h3 className="text-h4 text-fg">{category.name}</h3>

      {/* eslint-disable-next-line jsx-a11y/no-redundant-roles -- list-style: none medido; ver Timeline */}
      <ul role="list" className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {category.skills.map((skill) => (
          <SkillCard key={skill.name} skill={skill} />
        ))}
      </ul>
    </div>
  );
}
