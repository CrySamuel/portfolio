import { cn } from '@portfolio/ui';
import type { ReactNode } from 'react';

import { headingId } from '@/components/layout/Section';

export interface SectionHeadingProps {
  /** O mesmo id passado ao <Section>. O id do titulo e derivado dele. */
  sectionId: string;
  /** Rotulo curto acima do titulo. */
  eyebrow?: string;
  title: string;
  description?: string;
  className?: string;
  /**
   * O nivel do titulo. Padrao {@code h2}; {@code h1} so quando a secao **e** a
   * pagina - ver o comentario do componente.
   */
  as?: 'h1' | 'h2';
}

/**
 * Eyebrow + titulo + descricao (secao 8.3).
 *
 * <p>O componente existe para garantir **um unico titulo por secao**, e o padrao
 * e {@code h2}: numa pagina composta de secoes, o {@code h1} pertence a pagina.
 *
 * <p><strong>A prop {@code as} e uma excecao estreita, e ela foi aberta por
 * medicao.</strong> A versao anterior nao tinha prop de nivel nenhuma, pelo
 * receio de que o primeiro uso apressado criasse duas secoes irmas com niveis
 * diferentes. O custo apareceu na rota {@code /projetos}: uma pagina cujo
 * conteudo inteiro e uma secao ficou <strong>sem {@code h1}</strong> - o axe
 * acusa {@code page-has-heading-one}, e quem navega por cabecalhos cai num
 * {@code h2} sem nunca ouvir o titulo da pagina.
 *
 * <p>A alternativa considerada foi um {@code h1} visualmente escondido acima da
 * secao. Ele resolveria o mesmo e deixaria a pagina anunciando duas vezes o
 * mesmo nome, um deles invisivel - conserto que se explica pela ferramenta, e
 * nao pela estrutura. O invariante que importa continua garantido: um titulo por
 * secao, com o id que o {@code aria-labelledby} do {@code <Section>} aponta.
 */
export function SectionHeading({
  sectionId,
  eyebrow,
  title,
  description,
  className,
  as = 'h2',
}: SectionHeadingProps): ReactNode {
  const Titulo = as;

  return (
    <div className={cn('flex flex-col gap-3', className)}>
      {/*
        O eyebrow fica fora do titulo, e nao dentro. Dentro, ele entraria no nome
        acessivel dele e a secao passaria a se chamar "Portfolio Projetos
        em destaque" - duas ideias coladas numa frase que ninguem escreveu.
      */}
      {eyebrow ? <p className="text-caption text-accent uppercase">{eyebrow}</p> : null}

      {/*
        A classe nao acompanha o nivel: `text-h2` vale para os dois. Nivel de
        titulo e ordem de leitura, tamanho de fonte e desenho - amarrar um ao
        outro faria a pagina de listagem mudar de aparencia por uma correcao de
        semantica.
      */}
      <Titulo id={headingId(sectionId)} className="text-h2 text-balance">
        {title}
      </Titulo>

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
