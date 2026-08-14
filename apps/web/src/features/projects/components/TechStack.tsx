import { Badge } from '@portfolio/ui';
import type { Technology } from '@portfolio/api-client';
import type { ReactNode } from 'react';

/**
 * As tecnologias de um projeto, como selos.
 *
 * <p>Server Component, sem estado - custo zero no bundle do cliente.
 *
 * <p><strong>A ordem chega pronta e nao se refaz aqui.</strong> Ela e alfabetica
 * por nome, decidida no dominio, e reordenar seria um segundo lugar decidindo a
 * mesma coisa - o modo pelo qual duas telas do mesmo sistema passam a mostrar
 * ordens diferentes. Em particular, <em>nao</em> se ordena por
 * `category`: o enum do backend declara explicitamente que a ordem das
 * familias nao tem peso semantico, e ordenar por ela aqui inventaria um
 * significado que o outro lado nega ter.
 *
 * <p>Os selos nao sao interativos neste commit. O filtro por tecnologia chega no
 * 38 e vive na listagem, nao no card: dentro de um card que ja e um link,
 * qualquer controle criaria um segundo alvo de foco - exatamente o que a
 * Definition of Done proibe.
 *
 * @param label nome acessivel da lista; o card passa o titulo do projeto para
 *   que o leitor de tela diga de quem sao as tecnologias
 */
export function TechStack({
  technologies,
  label,
}: {
  technologies: readonly Technology[];
  label: string;
}): ReactNode {
  if (technologies.length === 0) return null;

  return (
    /*
      role="list" outra vez, e pela mesma razao ja medida no Timeline e no
      SkillCategory: o preflight do Tailwind aplica list-style: none, e o WebKit
      remove a semantica de lista de elemento sem marcador. Sem o atributo, o
      leitor de tela deixa de anunciar quantas tecnologias ha.
    */
    // eslint-disable-next-line jsx-a11y/no-redundant-roles -- list-style: none medido; ver Timeline
    <ul role="list" aria-label={label} className="flex flex-wrap gap-1.5">
      {technologies.map((technology) => (
        <li key={technology.slug}>
          <Badge variant="outline">{technology.name}</Badge>
        </li>
      ))}
    </ul>
  );
}
