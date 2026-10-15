import type { Metadata, Viewport } from 'next';
import { getTranslations } from 'next-intl/server';
import type { ReactNode } from 'react';

import { SkipLink } from '@/components/common/SkipLink';
import { Footer } from '@/components/layout/Footer';
import { Navbar } from '@/components/layout/Navbar';
import { ThemeProvider } from '@/components/providers/ThemeProvider';
import {
  fixarIdioma,
  idiomaDaRota,
  parametrosDosIdiomas,
  type ParametrosDoIdioma,
} from '@/i18n/locale';
import { caminhoPublico } from '@/i18n/navigation';
import { routing } from '@/i18n/routing';
import { getProfile } from '@/lib/api/profile';
import { fontVariables } from '@/lib/fonts';
import { localeOpenGraph, tituloDoSite } from '@/lib/metadata';
import { MAIN_ID, NAV_IDS } from '@/lib/navigation';
import { SITE_URL } from '@/lib/site';

import '../globals.css';

/**
 * Um build por idioma, e e isto que mantem as rotas estaticas: sem a lista, o segmento
 * {@code [locale]} so seria resolvido por requisicao.
 */
export function generateStaticParams(): { locale: string }[] {
  return parametrosDosIdiomas();
}

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

/**
 * O que toda rota herda: a raiz dos enderecos, o titulo padrao com o template
 * das demais, e o Open Graph de base.
 *
 * <p>O template e o que faltava ate aqui: a aba da pagina de um projeto mostrava
 * so "FinAI", sem dizer de quem era o site. Agora toda rota com titulo proprio
 * sai como "FinAI | Crystofer Demetino", e a home - que nao tem titulo proprio -
 * fica com o padrao.
 *
 * <p>O canonical nao mora aqui, e a razao esta em {@code metadataDaPagina}: no
 * layout ele seria herdado por toda rota que esquecesse o seu.
 *
 * <p>A chamada ao perfil nao custa uma segunda requisicao - o mesmo {@code cache}
 * do React que o corpo do layout usa logo abaixo.
 */
export async function generateMetadata({ params }: ParametrosDoIdioma): Promise<Metadata> {
  const locale = await idiomaDaRota(params);
  const t = await getTranslations({ locale, namespace: 'Metadata' });
  const perfil = await getProfile(locale);
  const titulo = tituloDoSite(perfil);
  const descricao = t('siteDescription', { nome: perfil.fullName });

  return {
    // A raiz contra a qual o Next resolve todo endereco de metadata - canonical
    // e og:image inclusive. Sem ela, a imagem de compartilhamento sai relativa e
    // o crawler do WhatsApp ou do LinkedIn nao tem contra o que resolve-la: o
    // link aparece sem imagem. O raciocinio inteiro, e a ordem das quatro
    // fontes, esta em lib/site.ts.
    metadataBase: SITE_URL,
    title: { default: titulo, template: `%s | ${perfil.fullName}` },
    description: descricao,
    applicationName: perfil.fullName,
    authors: [{ name: perfil.fullName, url: '/' }],
    creator: perfil.fullName,
    openGraph: {
      type: 'website',
      siteName: perfil.fullName,
      locale: localeOpenGraph(locale),
      title: titulo,
      description: descricao,
    },
    twitter: { card: 'summary_large_image' },
    // Indexavel e seguivel e o padrao sem a meta; o que ela acrescenta e a
    // permissao para a miniatura grande e o trecho sem limite no resultado.
    robots: {
      index: true,
      follow: true,
      googleBot: { index: true, follow: true, 'max-image-preview': 'large', 'max-snippet': -1 },
    },
  };
}

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

export default async function RootLayout({
  children,
  params,
}: ParametrosDoIdioma & { readonly children: ReactNode }) {
  const locale = await idiomaDaRota(params);

  // Antes de qualquer traducao, e cada pagina repete - ver fixarIdioma.
  fixarIdioma(locale);

  // O mesmo perfil que o hero usa: o `cache` do React devolve a promessa ja
  // resolvida, entao montar layout e pagina custa **uma** chamada a API.
  const profile = await getProfile(locale);
  const t = await getTranslations();

  // A API manda plataforma e URL; o rotulo e texto de interface e mora nas
  // mensagens. A chave e montada com a plataforma, e as mensagens sao tipadas:
  // uma plataforma nova no contrato sem rotulo reprova a compilacao, como
  // reprovava quando o rotulo era um Record no proprio componente.
  const socialLinks = profile.socialLinks.map((link) => ({
    platform: link.platform,
    href: link.url,
    label: t(`SocialLinks.${link.platform}`),
  }));

  // A navbar e componente de cliente, e recebe tudo ja traduzido: o next-intl
  // no navegador traria o formatador de mensagens, varios KB do orcamento de JS
  // para repetir o que o servidor ja fez.
  // Os textos do seletor vem no idioma de destino: e "Read in English" que
  // aparece numa pagina em portugues.
  const idiomas = await Promise.all(
    routing.locales
      .filter((outro) => outro !== locale)
      .map(async (outro) => {
        const tOutro = await getTranslations({ locale: outro, namespace: 'LocaleSwitcher' });
        return { locale: outro, sigla: tOutro('short'), rotulo: tOutro('label') };
      }),
  );

  const navegacao = {
    nome: profile.fullName,
    idiomas,
    inicio: caminhoPublico({ href: '/', locale }),
    itens: NAV_IDS.map((id) => ({ id, label: t(`Navigation.${id}`) })),
    textos: {
      principal: t('Navigation.main'),
      abrirMenu: t('Navigation.openMenu'),
      fecharMenu: t('Navigation.closeMenu'),
      menu: t('Navigation.menu'),
      mobile: t('Navigation.mobile'),
      temaClaro: t('ThemeToggle.toLight'),
      temaEscuro: t('ThemeToggle.toDark'),
    },
  };

  return (
    // suppressHydrationWarning e obrigatorio aqui: o script anti-FOUC escreve
    // data-theme no <html> antes da hidratacao, entao o atributo que o React
    // encontra nao e o que ele renderizou. O aviso e esperado e vale so para
    // este elemento - nao silencia nada abaixo dele.
    //
    // As variaveis de fonte entram no <html>, e nao no <body>: os tokens de
    // tipografia sao resolvidos em :root, entao precisam estar visiveis no
    // elemento raiz.
    <html lang={locale} className={fontVariables} suppressHydrationWarning>
      <body>
        <ThemeProvider>
          {/*
            Primeiro no DOM, e portanto primeiro na ordem de tabulacao - essa
            posicao e a razao de o componente existir. Skip link depois da navbar
            nao pula nada.
          */}
          <SkipLink />
          <Navbar {...navegacao} />

          {/*
            tabIndex={-1} nao e detalhe: sem ele o pulo do skip link move a
            rolagem mas nao o foco, e o proximo Tab volta para o topo da navbar -
            o atalho parece funcionar e nao funciona.

            pt-16 compensa a altura do header fixo, que saiu do fluxo.
          */}
          <main id={MAIN_ID} tabIndex={-1} className="pt-16">
            {children}
          </main>

          <Footer nome={profile.fullName} links={socialLinks} />
        </ThemeProvider>
      </body>
    </html>
  );
}
