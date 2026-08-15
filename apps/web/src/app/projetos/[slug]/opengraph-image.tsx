import { ImageResponse } from 'next/og';

import { carregarProjeto, slugsDoCatalogo, type Parametros } from '@/app/projetos/[slug]/projeto';
import { getProfile } from '@/lib/api/profile';

/**
 * Os mesmos slugs da pagina, declarados de novo porque o Next nao os herda do
 * arquivo vizinho - a medicao esta em {@link slugsDoCatalogo}.
 */
export function generateStaticParams(): Promise<{ slug: string }[]> {
  return slugsDoCatalogo();
}

/** O tamanho que Facebook, LinkedIn, WhatsApp e X esperam. */
export const size = { width: 1200, height: 630 };

export const contentType = 'image/png';

/**
 * O texto alternativo da imagem, e ele e o mesmo em todos os projetos.
 *
 * <p>Um alt por projeto seria possivel com {@code generateImageMetadata}, e foi
 * recusado: o {@code og:image:alt} aparece ao lado do titulo e da descricao que
 * as outras metas ja carregam, entao repetir o titulo aqui diria a mesma coisa
 * duas vezes. O que falta a quem nao ve a imagem nao e o titulo - e saber o que
 * ha nela alem do titulo.
 */
export const alt = 'Cartão do projeto, com o título, o resumo e as tecnologias usadas';

/**
 * Os tokens da secao 7 do plano, escritos como valor.
 *
 * <p>E a unica excecao a regra 7.8 - "nenhum componente escreve um valor
 * hexadecimal" -, e ela e imposta pela ferramenta: esta imagem e desenhada pelo
 * satori, fora do navegador, sem folha de estilo e sem {@code var()}. Nao ha
 * CSS custom property para consultar.
 *
 * <p>Por isso as cores ficam juntas e nomeadas aqui em vez de espalhadas pelo
 * JSX: quando um token mudar em {@code tokens.css}, o que precisa acompanhar
 * esta neste bloco, e nao em doze lugares. Sao os valores do tema escuro, que e
 * o padrao do site.
 */
const COR = {
  fundo: '#08080a',
  texto: '#fafafa',
  apagado: '#a1a1aa',
  borda: '#232329',
  destaque: '#8b7cf6',
} as const;

/**
 * A imagem de compartilhamento de um projeto.
 *
 * <p><strong>Ela e gerada no build, e nao por requisicao</strong> - e isso
 * custou o {@code generateStaticParams} declarado acima, que a primeira versao
 * supos herdado da pagina. Com os PNGs prontos, o crawler que buscar o
 * {@code og:image} recebe um arquivo estatico da CDN.
 *
 * <p><strong>Sem fonte propria, e a ausencia foi medida antes de tentar.</strong>
 * O satori aceita TTF, OTF e WOFF, e nao WOFF2 - que e o unico formato dos
 * binarios em {@code lib/fonts}. Converter significaria versionar uma segunda
 * copia de cada fonte so para esta imagem. A tipografia padrao do {@code next/og}
 * resolve a hierarquia por tamanho e cor, que e o que uma miniatura de 1200x630
 * comunica de qualquer forma.
 *
 * <p>O nome e o cargo saem do perfil, e nao de duas constantes escritas aqui: e
 * o mesmo dado do hero, e duplica-lo criaria um lugar onde o nome do dono do
 * portfolio pode ficar desatualizado sem ninguem notar.
 */
export default async function ImagemDoProjeto({ params }: Parametros): Promise<ImageResponse> {
  const { slug } = await params;
  const [projeto, perfil] = await Promise.all([carregarProjeto(slug), getProfile()]);

  return new ImageResponse(
    <div
      style={{
        width: '100%',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        backgroundColor: COR.fundo,
        color: COR.texto,
        padding: 72,
        // O gap e um piso de respiro, e ele existe por medicao: sem ele, o
        // space-between so afasta o que sobra de altura, e o projeto com resumo
        // de tres linhas e oito tecnologias saiu com os selos encostados no
        // texto. Com o piso, os dois blocos nunca se tocam.
        gap: 24,
      }}
    >
      {/*
        overflow oculto, e o corte e a degradacao escolhida. O resumo pode ter
        ate 280 caracteres - o limite da coluna -, e nessa largura isso da
        quatro linhas. Sem o corte, o excesso empurraria os selos e o rodape
        para fora do cartao; com ele, o que se perde e a ultima linha de um
        texto que ja aparece inteiro na pagina.
      */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 20, overflow: 'hidden' }}>
        <div style={{ display: 'flex', fontSize: 24, color: COR.destaque, letterSpacing: 2 }}>
          PROJETO
        </div>

        <div style={{ display: 'flex', fontSize: 68, lineHeight: 1.05 }}>{projeto.title}</div>

        <div style={{ display: 'flex', fontSize: 28, lineHeight: 1.4, color: COR.apagado }}>
          {projeto.summary}
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 28 }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
          {projeto.technologies.map((tecnologia) => (
            <div
              key={tecnologia.slug}
              style={{
                display: 'flex',
                fontSize: 22,
                color: COR.apagado,
                border: `1px solid ${COR.borda}`,
                borderRadius: 999,
                padding: '6px 18px',
              }}
            >
              {tecnologia.name}
            </div>
          ))}
        </div>

        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            borderTop: `1px solid ${COR.borda}`,
            paddingTop: 28,
            fontSize: 24,
          }}
        >
          <div style={{ display: 'flex' }}>{perfil.fullName}</div>
          <div style={{ display: 'flex', color: COR.apagado }}>{perfil.headline}</div>
        </div>
      </div>
    </div>,
    size,
  );
}
