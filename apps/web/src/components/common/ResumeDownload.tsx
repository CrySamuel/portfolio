import { Button } from '@portfolio/ui';
import type { ReactNode } from 'react';

export interface ResumeDownloadProps {
  /** Endereco do arquivo. Relativo quando o proprio site o serve. */
  href: string;
  /** Nome sugerido ao salvar. Sem ele, o navegador usa o do caminho. */
  fileName?: string;
}

/**
 * Botao de download do curriculo.
 *
 * Recebe o endereco por prop e nao conhece perfil, API nem dominio: e um
 * componente de `components/common`, e a regra da secao 3.6 e que ele componha
 * primitivos sem saber de onde o dado veio. Quem sabe e a secao.
 *
 * <strong>O formato aparece no rotulo, e nao so no icone.</strong> Link que abre
 * ou baixa arquivo precisa dizer o que e antes de ser clicado - quem usa leitor
 * de tela nao ve a extensao no fim da URL, e quem esta em conexao medida quer
 * saber o que vai buscar. E o mesmo criterio do rodape, onde cada link social
 * tem rotulo proprio.
 *
 * <strong>`download` funciona porque a origem e a mesma.</strong> O atributo e
 * ignorado em endereco de outro dominio, por decisao dos navegadores; como o
 * arquivo e servido de `public/` pelo proprio site, ele vale. Se um dia o PDF for
 * para um CDN externo, o atributo para de funcionar em silencio e o link passa a
 * abrir o arquivo em vez de salva-lo.
 */
export function ResumeDownload({ href, fileName }: ResumeDownloadProps): ReactNode {
  return (
    <Button asChild variant="outline" size="lg">
      <a href={href} download={fileName ?? true} hrefLang="pt-BR" type="application/pdf">
        Baixar currículo (PDF)
      </a>
    </Button>
  );
}
