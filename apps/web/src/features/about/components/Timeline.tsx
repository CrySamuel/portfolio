import type { Experience } from '@portfolio/api-client';
import type { ReactNode } from 'react';

import { TimelineItem } from '@/features/about/components/TimelineItem';

/**
 * A timeline profissional, em lista ordenada.
 *
 * <strong>`<ol>`, e nao `<div>` com cara de lista.</strong> A sequencia e a
 * informacao: cada item vem depois do anterior no tempo, e um leitor de tela
 * anuncia "item 2 de 3" a partir da marcacao. Trocar por divs produziria a mesma
 * tela e perderia a contagem, que e o quarto item da Definition of Done deste
 * MVP.
 *
 * <strong>O `role="list"` nao e redundante, e isso foi medido.</strong> O
 * preflight do Tailwind aplica `list-style: none`, e o WebKit remove a semantica
 * de lista de qualquer elemento sem marcador - VoiceOver deixa de anunciar a
 * contagem. Conferido no navegador: `getComputedStyle(ol).listStyleType` devolve
 * `none` aqui, e `disc` na lista de destaques do {@link TimelineItem}. Dai a
 * assimetria entre as duas - la o atributo seria mesmo redundante, e nao esta.
 *
 * O `jsx-a11y/no-redundant-roles` reprova sem olhar o CSS, entao a regra e
 * desligada nesta linha - com a medida acima como justificativa, e nao por
 * conveniencia.
 *
 * <strong>Nao ha ordenacao aqui.</strong> A lista chega pronta da API, que a
 * recebe pronta do dominio. Um `sort` neste arquivo seria um terceiro lugar
 * decidindo a mesma coisa - e este projeto ja mediu, no modulo de perfil, o
 * quanto e facil acreditar que a ordem vem de onde nao vem.
 *
 * Server Component sem estado: custo zero no bundle do cliente.
 */
export function Timeline({ experiences }: { experiences: readonly Experience[] }): ReactNode {
  // Lista vazia nao renderiza uma lista vazia. E o estado de quem ainda nao
  // preencheu a propria historia, e a API o distingue de falha respondendo 200
  // com `[]`; quem decide o que mostrar nesse caso e a secao, no commit 28.
  if (experiences.length === 0) return null;

  return (
    // A trilha e a borda esquerda do proprio <ol>, e nao um elemento a parte:
    // uma linha decorativa em <div> proprio seria mais um no na arvore de
    // acessibilidade para nao dizer nada.
    // eslint-disable-next-line jsx-a11y/no-redundant-roles -- list-style: none medido; ver o Javadoc
    <ol role="list" className="flex flex-col gap-10 border-l border-border">
      {experiences.map((experience) => (
        <TimelineItem
          // A chave e a mesma tripla que a migracao declara unica: empresa,
          // cargo e data de inicio identificam uma passagem. O id nao serve -
          // ele nao e publicado no contrato, de proposito.
          key={`${experience.company}|${experience.role}|${experience.startDate}`}
          experience={experience}
        />
      ))}
    </ol>
  );
}
