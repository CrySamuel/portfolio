import { Button } from '@portfolio/ui';
import Link from 'next/link';
import type { ReactNode } from 'react';

import { Container } from '@/components/layout/Container';

interface NotFoundMessageProps {
  readonly eyebrow: string;
  readonly title: string;
  readonly description: string;
  /** Para onde mandar quem chegou aqui - ja no idioma da pagina. */
  readonly href: string;
  readonly linkLabel: string;
}

/**
 * O corpo de uma tela de 404: o que aconteceu e para onde ir.
 *
 * <p>Um so desenho para as duas 404 do site - a de projeto e a de endereco sem rota. Cada uma diz a
 * propria frase e o proprio destino; o que nao pode divergir e a forma, e por isso ela mora aqui.
 */
export function NotFoundMessage({
  eyebrow,
  title,
  description,
  href,
  linkLabel,
}: NotFoundMessageProps): ReactNode {
  return (
    <Container className="flex flex-col items-start gap-5 py-24 md:py-32">
      <p className="text-caption text-accent uppercase">{eyebrow}</p>

      {/*
        O <h1> desta tela. Ele diz o que aconteceu em vez de repetir o codigo do
        erro: "404" e a informacao para a maquina, e o titulo e para a pessoa.
      */}
      <h1 className="text-h1 text-balance">{title}</h1>

      <p className="max-w-reading text-body-lg text-pretty text-fg-muted">{description}</p>

      <Button asChild variant="outline">
        {/*
          <Link>, e nao <a>: a volta e navegacao interna, entao vale a transicao
          sem recarregar a pagina - e o destino ja e HTML pre-renderizado.
        */}
        <Link href={href}>{linkLabel}</Link>
      </Button>
    </Container>
  );
}
