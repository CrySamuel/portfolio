import type { Metadata, Viewport } from 'next';
import type { ReactNode } from 'react';

import { SkipLink } from '@/components/common/SkipLink';
import { SOCIAL_LINK_LABELS } from '@/components/common/SocialLinks';
import { Footer } from '@/components/layout/Footer';
import { Navbar } from '@/components/layout/Navbar';
import { ThemeProvider } from '@/components/providers/ThemeProvider';
import { getProfile } from '@/lib/api/profile';
import { fontVariables } from '@/lib/fonts';
import { MAIN_ID } from '@/lib/navigation';
import { SITE_URL } from '@/lib/site';

import './globals.css';

/**
 * HTML pre-renderizado no build, revalidado de hora em hora (ISR).
 *
 * Nao e preferencia: e o que impede o visitante de esperar pelo cold start. O
 * servico gratuito do Render hiberna depois de 15 minutos sem trafego e leva
 * cerca de um minuto para voltar - com renderizacao por requisicao, quem
 * abrisse o site depois de um periodo parado ficaria esse minuto olhando para
 * uma tela de carregamento. Com ISR, a CDN entrega o HTML pronto e so a
 * revalidacao em segundo plano toca a API. E o raciocinio do ADR-0006, agora
 * com numero.
 *
 * Expirado o prazo, o Next serve a versao antiga na hora e revalida atras -
 * entao nem a revalidacao faz alguem esperar.
 */
export const revalidate = 3600;

export const metadata: Metadata = {
  // A raiz contra a qual o Next resolve todo endereco de metadata - canonical e
  // og:image inclusive. Sem ela, a imagem de compartilhamento sai relativa e o
  // crawler do WhatsApp ou do LinkedIn nao tem contra o que resolve-la: o link
  // aparece sem imagem. O raciocinio inteiro, e a ordem das quatro fontes, esta
  // em lib/site.ts.
  metadataBase: SITE_URL,
  title: 'Crystofer Demetino — Desenvolvedor Backend',
  description:
    'Portfólio de Crystofer Demetino, Desenvolvedor Backend especializado em Java e Spring Boot.',
};

/**
 * Duas metas, uma por preferencia de sistema: e o que tinge a barra do
 * navegador certo na primeira visita, sem depender de JavaScript. A partir da
 * hidratacao o ThemeProvider assume as duas, para que a escolha manual - que
 * nao mexe na preferencia do sistema - tambem seja refletida.
 *
 * As cores sao os --color-bg dos dois temas: <meta> nao aceita var().
 */
export const viewport: Viewport = {
  themeColor: [
    { media: '(prefers-color-scheme: dark)', color: '#08080a' },
    { media: '(prefers-color-scheme: light)', color: '#ffffff' },
  ],
};

export default async function RootLayout({ children }: { children: ReactNode }) {
  // O mesmo perfil que o hero usa: o `cache` do React devolve a promessa ja
  // resolvida, entao montar layout e pagina custa **uma** chamada a API.
  const profile = await getProfile();

  // A API manda plataforma e URL; o rotulo e texto de interface e mora no
  // front. Juntar as duas coisas aqui evita que o componente de apresentacao
  // precise conhecer o formato da resposta.
  const socialLinks = profile.socialLinks.map((link) => ({
    platform: link.platform,
    href: link.url,
    label: SOCIAL_LINK_LABELS[link.platform],
  }));

  return (
    // suppressHydrationWarning e obrigatorio aqui: o script anti-FOUC escreve
    // data-theme no <html> antes da hidratacao, entao o atributo que o React
    // encontra nao e o que ele renderizou. O aviso e esperado e vale so para
    // este elemento - nao silencia nada abaixo dele.
    //
    // As variaveis de fonte entram no <html>, e nao no <body>: os tokens de
    // tipografia sao resolvidos em :root, entao precisam estar visiveis no
    // elemento raiz.
    <html lang="pt-BR" className={fontVariables} suppressHydrationWarning>
      <body>
        <ThemeProvider>
          {/*
            Primeiro no DOM, e portanto primeiro na ordem de tabulacao - essa
            posicao e a razao de o componente existir. Skip link depois da navbar
            nao pula nada.
          */}
          <SkipLink />
          <Navbar />

          {/*
            tabIndex={-1} nao e detalhe: sem ele o pulo do skip link move a
            rolagem mas nao o foco, e o proximo Tab volta para o topo da navbar -
            o atalho parece funcionar e nao funciona.

            pt-16 compensa a altura do header fixo, que saiu do fluxo.
          */}
          <main id={MAIN_ID} tabIndex={-1} className="pt-16">
            {children}
          </main>

          <Footer links={socialLinks} />
        </ThemeProvider>
      </body>
    </html>
  );
}
