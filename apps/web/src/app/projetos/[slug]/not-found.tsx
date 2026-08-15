import { Button } from '@portfolio/ui';
import Link from 'next/link';
import type { ReactNode } from 'react';

import { Container } from '@/components/layout/Container';

/**
 * A 404 de projeto - a que o {@code notFound()} da pagina ao lado renderiza.
 *
 * <p>Vive dentro de {@code [slug]} porque o Next escolhe a
 * {@code not-found.tsx} mais proxima do segmento que chamou. Uma unica 404 na
 * raiz atenderia todo o site com o mesmo texto generico; aqui a pagina sabe o
 * que a pessoa procurava e para onde mandar, que e o que separa uma 404 util de
 * um beco sem saida.
 *
 * <p><strong>O status HTTP e 404 de verdade.</strong> O Next serve este arquivo
 * com o codigo certo, e isso importa alem da etiqueta: pagina de erro respondida
 * como 200 e indexada pelo buscador como conteudo valido, e o endereco morto
 * passa a aparecer nos resultados.
 *
 * <p>Sem {@code metadata} aqui. O {@code generateMetadata} da rota nunca chega a
 * devolver nada quando o projeto nao existe - ele mesmo e quem chama
 * {@code notFound()} -, entao o titulo que aparece na aba e o do layout. Um
 * titulo proprio para tela de erro entra no commit 50, junto com o resto da
 * metadata por rota.
 */
export default function ProjetoNaoEncontrado(): ReactNode {
  return (
    <Container className="flex flex-col items-start gap-5 py-24 md:py-32">
      <p className="text-caption text-accent uppercase">Erro 404</p>

      {/*
        O <h1> desta tela. Ele diz o que aconteceu em vez de repetir o codigo do
        erro: "404" e a informacao para a maquina, e o titulo e para a pessoa.
      */}
      <h1 className="text-h1 text-balance">Este projeto não existe</h1>

      <p className="max-w-reading text-body-lg text-pretty text-fg-muted">
        O endereço pode ter mudado, ou o projeto pode ter saído do catálogo. A lista completa
        continua no lugar.
      </p>

      <Button asChild variant="outline">
        {/*
          <Link>, e nao <a>: a volta para a listagem e navegacao interna, entao
          vale a transicao sem recarregar a pagina - e o HTML ja pre-renderizado
          da rota /projetos.
        */}
        <Link href="/projetos">Ver todos os projetos</Link>
      </Button>
    </Container>
  );
}
