import { cn } from '@portfolio/ui';
import type { ReactNode } from 'react';

import { headingId } from '@/components/layout/Section';

export interface SectionHeadingProps {
  /** O mesmo id passado ao <Section>. O id do <h2> e derivado dele. */
  sectionId: string;
  /** Rotulo curto acima do titulo. */
  eyebrow?: string;
  title: string;
  description?: string;
  className?: string;
}

/**
 * Eyebrow + titulo + descricao (secao 8.3).
 *
 * O componente existe para garantir **um unico <h2> por secao**. Nao ha prop
 * de nivel: se o nivel fosse configuravel, o primeiro uso apressado criaria
 * duas secoes com <h2> e <h3> para o mesmo papel, e a navegacao por titulos -
 * que e como muita gente le a pagina com leitor de tela - passaria a mentir
 * sobre a estrutura.
 */
export function SectionHeading({
  sectionId,
  eyebrow,
  title,
  description,
  className,
}: SectionHeadingProps): ReactNode {
  return (
    <div className={cn('flex flex-col gap-3', className)}>
      {/*
        O eyebrow fica fora do <h2>, e nao dentro. Dentro, ele entraria no nome
        acessivel do titulo e a secao passaria a se chamar "Portfolio Projetos
        em destaque" - duas ideias coladas numa frase que ninguem escreveu.
      */}
      {eyebrow ? <p className="text-caption text-accent uppercase">{eyebrow}</p> : null}

      <h2 id={headingId(sectionId)} className="text-h2 text-balance">
        {title}
      </h2>

      {/*
        max-w-reading e o token de 68 caracteres da secao 7.4 - nao max-w-prose,
        que e utilidade estatica do Tailwind fixada em 65ch. Linha longa demais
        faz o olho perder a proxima ao voltar: o limite e legibilidade, nao
        estetica.
      */}
      {description ? (
        <p className="max-w-reading text-body-lg text-pretty text-fg-muted">{description}</p>
      ) : null}
    </div>
  );
}
