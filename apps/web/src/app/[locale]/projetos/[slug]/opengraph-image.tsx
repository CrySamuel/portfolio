import { ImageResponse } from 'next/og';
import { getTranslations } from 'next-intl/server';

import {
  carregarProjeto,
  idiomasESlugsDoCatalogo,
  type Parametros,
} from '@/app/[locale]/projetos/[slug]/projeto';
import { idiomaDaRota } from '@/i18n/locale';
import { getProfile } from '@/lib/api/profile';
import { COR_DO_CARTAO, TAMANHO_DO_CARTAO } from '@/lib/share-card';

/**
 * Os mesmos slugs da pagina, em cada idioma, declarados aqui porque o Next nao
 * os herda nem do arquivo vizinho nem do layout - ver
 * {@link idiomasESlugsDoCatalogo}.
 */
export function generateStaticParams(): Promise<{ locale: string; slug: string }[]> {
  return idiomasESlugsDoCatalogo();
}

export const size = TAMANHO_DO_CARTAO;

export const contentType = 'image/png';

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
  const [locale, { slug }] = await Promise.all([idiomaDaRota(params), params]);
  const [projeto, perfil, t] = await Promise.all([
    carregarProjeto(slug, locale),
    getProfile(locale),
    getTranslations({ locale, namespace: 'ShareCard' }),
  ]);

  return new ImageResponse(
    <div
      style={{
        width: '100%',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        backgroundColor: COR_DO_CARTAO.fundo,
        color: COR_DO_CARTAO.texto,
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
        <div
          style={{ display: 'flex', fontSize: 24, color: COR_DO_CARTAO.destaque, letterSpacing: 2 }}
        >
          {t('projectEyebrow')}
        </div>

        <div style={{ display: 'flex', fontSize: 68, lineHeight: 1.05 }}>{projeto.title}</div>

        <div
          style={{ display: 'flex', fontSize: 28, lineHeight: 1.4, color: COR_DO_CARTAO.apagado }}
        >
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
                color: COR_DO_CARTAO.apagado,
                border: `1px solid ${COR_DO_CARTAO.borda}`,
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
            borderTop: `1px solid ${COR_DO_CARTAO.borda}`,
            paddingTop: 28,
            fontSize: 24,
          }}
        >
          <div style={{ display: 'flex' }}>{perfil.fullName}</div>
          <div style={{ display: 'flex', color: COR_DO_CARTAO.apagado }}>{perfil.headline}</div>
        </div>
      </div>
    </div>,
    size,
  );
}
