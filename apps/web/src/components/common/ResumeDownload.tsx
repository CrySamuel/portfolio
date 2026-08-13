import { Button } from '@portfolio/ui';
import type { ReactNode } from 'react';

export interface ResumeDownloadProps {
  /** Endereco do arquivo. Relativo quando o proprio site o serve. */
  href: string;
  /** Tamanho em bytes, para o rotulo dizer o que sera baixado. */
  sizeInBytes: number;
  /** Nome sugerido ao salvar. Sem ele, o navegador usa o do caminho. */
  fileName?: string;
}

/**
 * Botao de download do curriculo.
 *
 * Recebe endereco e tamanho por prop e nao conhece perfil, API nem sistema de
 * arquivos: e um componente de `components/common`, e a regra da secao 3.6 e que
 * ele componha primitivos sem saber de onde o dado veio. Quem sabe e a secao.
 *
 * <strong>O rotulo diz formato e tamanho, e nao so o icone.</strong> E criterio
 * de aceite da F04, e a razao e concreta: quem usa leitor de tela nao ve a
 * extensao no fim da URL, e quem esta em conexao medida decide se vale a pena
 * antes de clicar. Os dois vao no texto do link, nao em `title` nem em
 * `aria-label` - atributo que so aparece no hover nao existe para quem navega
 * por teclado ou toque.
 *
 * <strong>`download` funciona porque a origem e a mesma.</strong> O atributo e
 * ignorado em endereco de outro dominio, por decisao dos navegadores; como o
 * arquivo e servido de `public/` pelo proprio site, ele vale. Se um dia o PDF for
 * para um CDN externo, o atributo para de funcionar em silencio e o link passa a
 * abrir o arquivo em vez de salva-lo.
 */
export function ResumeDownload({ href, sizeInBytes, fileName }: ResumeDownloadProps): ReactNode {
  return (
    <Button asChild variant="outline" size="lg">
      <a href={href} download={fileName ?? true} hrefLang="pt-BR" type="application/pdf">
        Baixar currículo (PDF, {formatarTamanho(sizeInBytes)})
      </a>
    </Button>
  );
}

/**
 * Bytes em KB ou MB, com o separador decimal do pt-BR.
 *
 * KB aqui e o de 1024 bytes, que e o que os sistemas operacionais mostram - o
 * numero precisa bater com o que a pessoa ve depois de salvar o arquivo, e nao
 * com a definicao do SI.
 *
 * Sem casa decimal abaixo de 1 MB: "77 KB" informa tanto quanto "76,7 KB" e le
 * melhor. Acima disso a casa volta, porque a diferenca entre 1 MB e 1,8 MB
 * importa para quem esta contando franquia de dados.
 */
function formatarTamanho(bytes: number): string {
  const kb = bytes / 1024;
  if (kb < 1024) return `${Math.round(kb).toLocaleString('pt-BR')} KB`;
  return `${(kb / 1024).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} MB`;
}
