import { ImageResponse } from 'next/og';

import { getProfile } from '@/lib/api/profile';
import { listSkills } from '@/lib/api/skills';
import { CARTAO_DO_SITE, COR_DO_CARTAO, TAMANHO_DO_CARTAO } from '@/lib/share-card';
import { SITE_URL } from '@/lib/site';

export const size = TAMANHO_DO_CARTAO;

export const contentType = 'image/png';

/**
 * Diz o que ha na imagem alem do que as outras metas ja dizem - o mesmo
 * criterio do texto alternativo da imagem de cada projeto.
 */
export const alt = CARTAO_DO_SITE.alt;

/** Quantas competencias cabem numa linha de selos sem quebrar para a segunda. */
const COMPETENCIAS_NO_CARTAO = 6;

/**
 * A imagem de compartilhamento do site.
 *
 * <p>E a que aparece quando alguem cola o endereco da home - no LinkedIn, no
 * WhatsApp, num e-mail de recrutador. Vale tambem para toda rota sem imagem
 * propria, mas <strong>nao por heranca</strong>: um segmento filho que declara
 * {@code openGraph} descarta a imagem herdada deste arquivo, e foi assim que a
 * listagem saiu sem {@code og:image} na primeira medicao. Quem a estende as
 * outras rotas e o {@code metadataDaPagina}. As paginas de projeto tem a delas,
 * ao lado.
 *
 * <p><strong>Gerada no build, como a dos projetos</strong>, e revalidada junto
 * com a pagina: o crawler recebe um PNG estatico da CDN, e nao espera o cold
 * start da API.
 *
 * <p>Nome, cargo e local saem do perfil; os selos sao as primeiras competencias
 * da secao de skills, na ordem editorial dela. Nada aqui e escrito a mao, pela
 * mesma razao de sempre: uma copia do nome do dono e um lugar onde ele fica
 * desatualizado.
 *
 * <p>O rodape mostra o endereco do site, e ele sai de {@code SITE_URL}: quando o
 * dominio proprio chegar, a imagem acompanha sem edicao.
 */
export default async function ImagemDoSite(): Promise<ImageResponse> {
  const [perfil, categorias] = await Promise.all([getProfile(), listSkills()]);
  const competencias = categorias
    .flatMap((categoria) => categoria.skills.map((skill) => skill.name))
    .slice(0, COMPETENCIAS_NO_CARTAO);

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
        gap: 24,
      }}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        <div
          style={{ display: 'flex', fontSize: 24, color: COR_DO_CARTAO.destaque, letterSpacing: 2 }}
        >
          PORTFÓLIO
        </div>

        <div style={{ display: 'flex', fontSize: 84, lineHeight: 1.05 }}>{perfil.fullName}</div>

        <div style={{ display: 'flex', fontSize: 36, color: COR_DO_CARTAO.apagado }}>
          {perfil.headline}
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 28 }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
          {competencias.map((competencia) => (
            <div
              key={competencia}
              style={{
                display: 'flex',
                fontSize: 22,
                color: COR_DO_CARTAO.apagado,
                border: `1px solid ${COR_DO_CARTAO.borda}`,
                borderRadius: 999,
                padding: '6px 18px',
              }}
            >
              {competencia}
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
          <div style={{ display: 'flex' }}>{SITE_URL.host}</div>
          <div style={{ display: 'flex', color: COR_DO_CARTAO.apagado }}>
            {perfil.location ?? ''}
          </div>
        </div>
      </div>
    </div>,
    size,
  );
}
