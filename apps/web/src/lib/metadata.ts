import type { Profile } from '@portfolio/api-client';
import type { Metadata } from 'next';

import { CARTAO_DO_SITE, TAMANHO_DO_CARTAO } from '@/lib/share-card';

/** O idioma do site, no formato que o Open Graph espera - com sublinhado. */
const LOCALE_OPEN_GRAPH = 'pt_BR';

/**
 * O titulo da home, que e tambem o titulo padrao de qualquer rota sem um proprio.
 *
 * <p>Nome e cargo saem do perfil, e nao de um literal: e o mesmo dado do hero e
 * das imagens de compartilhamento, e uma segunda copia aqui seria o lugar onde o
 * nome do dono ficaria desatualizado sem ninguem notar. O separador e a barra, e
 * nao o travessao, porque o cargo que vem da API ja tem um - dois na mesma
 * linha leem como frase quebrada.
 */
export function tituloDoSite(perfil: Profile): string {
  return `${perfil.fullName} | ${perfil.headline}`;
}

/**
 * A descricao da home.
 *
 * <p><strong>Nao e a bio, e a diferenca e de tamanho.</strong> A bio tem mais de
 * trezentos caracteres e o resultado de busca corta em torno de 160 - o que
 * sobrasse seria cortado no meio de uma frase. Esta foi escrita para caber
 * inteira, com o que um recrutador procura na ordem em que procura.
 */
export function descricaoDoSite(perfil: Profile): string {
  return (
    `Portfólio de ${perfil.fullName}, desenvolvedor backend Java e Spring Boot: ` +
    'projetos em produção, arquitetura hexagonal, testes de integração e código aberto.'
  );
}

/** O que uma pagina precisa dizer sobre si mesma. */
export interface Pagina {
  readonly titulo: string;
  readonly descricao: string;
  /** O caminho canonico, relativo - o {@code metadataBase} do layout o torna absoluto. */
  readonly caminho: string;
  /** A home usa o titulo inteiro; as demais recebem o sufixo do template do layout. */
  readonly tituloAbsoluto?: boolean;
  /** Presente nas paginas que sao um artigo, como a de cada projeto. */
  readonly artigo?: { readonly publicadoEm: string | null };
  /**
   * Para a pagina sem {@code opengraph-image} no proprio segmento: usa o cartao
   * do site, que ela nao herda. A que tem arquivo nao deve pedir - ver
   * {@link metadataDaPagina}.
   */
  readonly cartaoDoSite?: boolean;
}

/**
 * Title, description, canonical e Open Graph de uma pagina.
 *
 * <p><strong>Existe por causa de como o Next junta metadata.</strong> A juncao
 * entre layout e pagina e rasa: a pagina que declara {@code openGraph} substitui
 * o objeto inteiro do layout, e nao so as chaves que repetiu. Sem este passo,
 * cada pagina com titulo proprio perderia {@code siteName} e {@code locale} em
 * silencio - nada reprova, o cartao compartilhado so sai mais pobre.
 *
 * <p><strong>O canonical e por pagina, e nunca no layout.</strong> No layout ele
 * seria herdado por toda rota que nao declarasse o seu, e uma pagina de projeto
 * esquecida passaria a dizer ao Google que e copia da home - que e a forma mais
 * silenciosa de tirar uma pagina do indice.
 *
 * <p><strong>A imagem so entra a pedido, e as duas medicoes que levaram a isso
 * apontam em sentidos opostos.</strong> A primeira: o {@code opengraph-image}
 * da raiz nao e herdado por uma pagina filha que declara {@code openGraph} - a
 * juncao rasa descarta a imagem junto com o resto, e a listagem saiu sem
 * {@code og:image}. A segunda: declarar a imagem em objeto em toda pagina
 * tampouco serve, porque a declarada <strong>vence</strong> o arquivo do
 * proprio segmento - os projetos perderam o cartao deles para o do site, e a
 * home perdeu o hash de cache do endereco. Por isso a imagem entra so onde nao
 * ha arquivo no segmento, e quem sabe disso e a pagina.
 */
export function metadataDaPagina(perfil: Profile, pagina: Pagina): Metadata {
  const comum = {
    siteName: perfil.fullName,
    locale: LOCALE_OPEN_GRAPH,
    url: pagina.caminho,
    title: pagina.titulo,
    description: pagina.descricao,
    ...(pagina.cartaoDoSite === true
      ? {
          images: [{ url: CARTAO_DO_SITE.caminho, alt: CARTAO_DO_SITE.alt, ...TAMANHO_DO_CARTAO }],
        }
      : {}),
  };

  return {
    title: pagina.tituloAbsoluto === true ? { absolute: pagina.titulo } : pagina.titulo,
    description: pagina.descricao,
    alternates: { canonical: pagina.caminho },
    openGraph:
      pagina.artigo === undefined
        ? { ...comum, type: 'website' }
        : {
            ...comum,
            type: 'article',
            // Espalhado condicionalmente porque a data e nulavel no contrato e
            // exactOptionalPropertyTypes esta ligado: `publishedTime: undefined`
            // nao e o mesmo que nao passar a chave.
            ...(pagina.artigo.publicadoEm === null
              ? {}
              : { publishedTime: pagina.artigo.publicadoEm }),
          },
  };
}
