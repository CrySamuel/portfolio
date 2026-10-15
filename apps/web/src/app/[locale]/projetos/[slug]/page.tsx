import type { Metadata } from 'next';
import type { ReactNode } from 'react';

import { cartaoDoProjeto } from '@/app/[locale]/cartoes';
import {
  carregarProjeto,
  slugsDoCatalogo,
  type Parametros,
} from '@/app/[locale]/projetos/[slug]/projeto';
import { degrausDaListagem } from '@/app/[locale]/projetos/trilha';
import { JsonLd } from '@/components/seo/JsonLd';
import { codigoFonte, grafo, trilha } from '@/components/seo/structured-data';
import { ProjectDetail } from '@/features/projects/components/ProjectDetail';
import { fixarIdioma, idiomaDaRota } from '@/i18n/locale';
import { caminhoPublico } from '@/i18n/navigation';
import { getProfile } from '@/lib/api/profile';
import { metadataDaPagina } from '@/lib/metadata';

/**
 * Os slugs que o build pre-renderiza - ver {@link slugsDoCatalogo}, que explica
 * por que a imagem de compartilhamento ao lado declara o mesmo.
 *
 * <p><strong>{@code dynamicParams} fica no padrao, que e permitir o resto.</strong>
 * Desligar tornaria 404 todo slug fora desta lista, e o custo apareceria fora do
 * build: o conteudo vem do banco, entao um projeto novo entra por migracao e
 * deploy da API, sem que o site seja reconstruido. A listagem revalida de hora
 * em hora e mostraria o card novo; com {@code dynamicParams: false} o card
 * levaria a 404 ate o proximo build do web - que e a mesma secao com links
 * quebrados que segurou o commit anterior. Permitindo, a primeira visita
 * renderiza a pagina e as seguintes a recebem pronta.
 */
export function generateStaticParams(): Promise<{ slug: string }[]> {
  return slugsDoCatalogo();
}

/**
 * Metadata por projeto, que e o criterio de aceite da F07.
 *
 * <p>Titulo e descricao vem do proprio conteudo - nada de "Projeto | Portfolio"
 * repetido em todas as paginas, que e o que faz um resultado de busca nao dizer
 * nada. O {@code canonical} fixa o endereco unico da pagina, e ele e resolvido
 * contra o {@code metadataBase} do layout.
 *
 * <p><strong>A imagem e declarada aqui, e vence o arquivo ao lado de
 * proposito.</strong> O {@code opengraph-image.tsx} continua desenhando o PNG;
 * quem o anuncia e o {@code cartaoDoProjeto}, porque o endereco que a convencao
 * geraria e o interno, com o idioma, e o middleware o redireciona - ver
 * app/[locale]/cartoes.
 *
 * <p>A chamada nao custa uma segunda requisicao: o {@code cache} do React
 * memoiza por passagem de renderizacao, entao o projeto que este metodo carrega
 * e o mesmo que a pagina abaixo recebe.
 */
export async function generateMetadata({ params }: Parametros): Promise<Metadata> {
  const [locale, { slug }] = await Promise.all([idiomaDaRota(params), params]);
  const [projeto, perfil, cartao] = await Promise.all([
    carregarProjeto(slug),
    getProfile(),
    cartaoDoProjeto(locale, slug),
  ]);

  return metadataDaPagina(perfil, {
    titulo: projeto.title,
    descricao: projeto.summary,
    caminho: caminhoPublico({ href: `/projetos/${projeto.slug}`, locale }),
    artigo: { publicadoEm: projeto.publishedAt },
    cartao,
  });
}

/**
 * A pagina de um projeto.
 *
 * <p>Uma linha de conteudo, como a listagem: quem sabe desenhar o detalhe e a
 * feature, e a rota so resolve o endereco. O que este arquivo carrega e a
 * traducao do slug em conteudo - e, quando nao ha conteudo, a 404 ao lado.
 *
 * <p>Os dados estruturados descrevem o projeto como codigo-fonte e a posicao
 * dele no site, e saem do mesmo projeto que a pagina desenha.
 */
export default async function ProjetoPage({ params }: Parametros): Promise<ReactNode> {
  const [locale, { slug }] = await Promise.all([idiomaDaRota(params), params]);
  fixarIdioma(locale);

  const [projeto, perfil, degraus] = await Promise.all([
    carregarProjeto(slug),
    getProfile(),
    degrausDaListagem(locale),
  ]);

  return (
    <>
      <JsonLd
        dados={grafo(
          codigoFonte(projeto, perfil),
          trilha([
            ...degraus,
            {
              nome: projeto.title,
              caminho: caminhoPublico({ href: `/projetos/${projeto.slug}`, locale }),
            },
          ]),
        )}
      />
      <ProjectDetail project={projeto} />
    </>
  );
}
