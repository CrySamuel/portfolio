import { cn } from '@portfolio/ui';
import type { ComponentPropsWithRef, ReactNode } from 'react';

/**
 * Sufixo do id do titulo de uma secao.
 *
 * Exportado, e nao repetido nos dois arquivos, porque e ele que liga o
 * aria-labelledby da secao ao <h2> do SectionHeading. Duas copias da string
 * seriam duas chances de divergir, e a divergencia e silenciosa: a secao
 * simplesmente perde o nome acessivel, sem erro em lugar nenhum.
 */
export function headingId(sectionId: string): string {
  return `${sectionId}-heading`;
}

export interface SectionProps extends ComponentPropsWithRef<'section'> {
  /** Ancora da navegacao e raiz do id do titulo. */
  id: string;
}

/**
 * Wrapper semantico com o ritmo vertical da secao 7.4: py-16 no mobile,
 * py-24 em md, py-32 em lg.
 *
 * Essa progressao e a decisao mais barata do design inteiro. Constancia de
 * espacamento e o que faz um site parecer desenhado por alguem - mais do que
 * qualquer detalhe visual isolado -, e centraliza-la aqui significa que
 * nenhuma secao futura precisa decidir de novo.
 *
 * A secao nao aplica largura: quem centraliza e o Container, por dentro. Sao
 * responsabilidades diferentes e separa-las permite que uma secao tenha fundo
 * de ponta a ponta com o conteudo contido.
 */
export function Section({ id, className, ...props }: SectionProps): ReactNode {
  return (
    <section
      id={id}
      // Um <section> so vira landmark quando tem nome acessivel; sem isso ele
      // e um agrupamento generico e some da lista de regioes do leitor de
      // tela. O padrao aponta para o id que o SectionHeading gera a partir do
      // mesmo sectionId.
      //
      // Se o titulo nao existir, a referencia fica pendurada - e o efeito e
      // exatamente o de nao ter atributo nenhum, entao o padrao nunca piora o
      // que ja haveria. Quem precisar de outro rotulo sobrescreve por props.
      aria-labelledby={headingId(id)}
      className={cn('py-16 md:py-24 lg:py-32', className)}
      {...props}
    />
  );
}
