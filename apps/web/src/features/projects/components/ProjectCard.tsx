import type { ProjectSummary } from '@portfolio/api-client';
import Link from 'next/link';
import type { ReactNode } from 'react';

import { TechStack } from '@/features/projects/components/TechStack';

/**
 * Um projeto na listagem e na home.
 *
 * <p>Server Component, sem estado - custo zero no bundle do cliente.
 *
 * <p><strong>Uma unica area de foco, e ela e o titulo.</strong> O padrao e o do
 * link esticado: o `<a>` envolve so o titulo, e um pseudo-elemento
 * (`after:absolute after:inset-0`) cobre o card inteiro. O resultado e um alvo
 * de clique grande com <em>uma</em> parada de teclado, cujo nome acessivel e o
 * titulo do projeto - e nao "Leia mais", que nao diz de que projeto se trata.
 *
 * <p>A alternativa ingenua - envolver o card todo num `<a>` - poe titulo, resumo
 * e a lista de tecnologias dentro do link, e o leitor de tela le tudo isso como
 * o nome de um unico elemento. O card fica com um nome de vinte palavras.
 *
 * <p><strong>O card nao desenha repositorio nem site, e nao tem como.</strong>
 * `ProjectSummary` nao carrega esses campos, por decisao do contrato: link
 * dentro de card que ja e link cria alvo aninhado, que e invalido em HTML e
 * anunciado duas vezes. Os enderecos vivem na pagina de detalhe.
 *
 * <p><strong>Nao ha capa, e a ausencia foi medida.</strong> O objetivo escrito
 * para este commit pede imagem otimizada, e a implementacao com `next/image`
 * chegou a existir: ela custa <em>5,0 KB comprimidos</em> no bundle inicial -
 * 123,0 KB contra 118,0 KB na mesma pagina, com e sem o import. Restavam 12,5 KB
 * ate o teto rigido de 130, entao seriam 40% do que sobra por um componente que
 * hoje nao renderiza nada: `coverImage` e nulo nos dois projetos publicados, e
 * nao ha imagem alguma no repositorio.
 *
 * <p>E a mesma conta que recusou o Radix Dialog no menu mobile. Quando houver
 * uma capa de verdade para exibir, o commit que a trouxer paga os 5 KB sabendo o
 * numero - o campo continua no contrato, esperando.
 */
export function ProjectCard({ project }: { project: ProjectSummary }): ReactNode {
  const { slug, title, summary, technologies } = project;

  return (
    <li className="h-full">
      <article className="relative flex h-full flex-col gap-3 rounded-lg border border-border bg-surface p-5 transition-colors focus-within:border-border-interactive hover:border-border-interactive">
        <h3 className="text-h4 text-fg">
          {/*
            O pseudo-elemento estica o link sobre o card inteiro. Ele fica no
            <a>, e nao no <article>, para que o nome acessivel continue sendo o
            titulo.
          */}
          <Link
            href={`/projetos/${slug}`}
            className="rounded-sm after:absolute after:inset-0 after:content-['']"
          >
            {title}
          </Link>
        </h3>

        <p className="text-body-sm text-fg-muted">{summary}</p>

        {/*
          `mt-auto` empurra as tecnologias para o rodape do card. E o que faz
          cards de resumos com tamanhos diferentes terminarem alinhados na mesma
          linha do grid, sem truncar texto - o limite do resumo ja e imposto na
          escrita, pela coluna VARCHAR(280). Medido: 246px nos dois cards de uma
          mesma linha, com resumos de uma frase e de 240 caracteres.
        */}
        <div className="mt-auto pt-1">
          <TechStack technologies={technologies} label={`Tecnologias de ${title}`} />
        </div>
      </article>
    </li>
  );
}
