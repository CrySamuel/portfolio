import type { ReactNode } from 'react';
import type { Graph } from 'schema-dts';

/**
 * Dados estruturados de uma pagina, num {@code <script type="application/ld+json">}.
 *
 * <p><strong>Sempre em {@code @graph}, mesmo com uma entidade so.</strong> Um
 * formato unico e o que deixa as entidades da mesma pagina se referirem por
 * {@code @id} - o site aponta para a pessoa como autor sem repeti-la -, e e o
 * que mantem este componente sem variacao.
 *
 * <p><strong>O {@code <} e escapado, e o motivo e o fim do elemento.</strong> O
 * HTML encerra o {@code <script>} no primeiro {@code </script>} que encontrar,
 * esteja ele em codigo ou dentro de uma string JSON. O conteudo vem do banco, e
 * um titulo de projeto com essa sequencia fecharia o elemento no meio e
 * despejaria o resto como HTML. Escrito como {@code <}, o caractere continua
 * o mesmo para quem le o JSON e deixa de existir para quem le o HTML.
 *
 * <p>A CSP nao interfere: {@code application/ld+json} e bloco de dados, e nao
 * script - o navegador nao o executa, entao o {@code script-src} nao se aplica.
 *
 * <p>Server Component, e o custo no bundle e zero: o JSON vai no HTML e nenhum
 * byte dele vira JavaScript no cliente.
 */
export function JsonLd({ dados }: { readonly dados: Graph }): ReactNode {
  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: JSON.stringify(dados).replace(/</g, '\\u003c') }}
    />
  );
}
