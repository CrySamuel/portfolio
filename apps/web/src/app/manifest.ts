import type { MetadataRoute } from 'next';

import { getTranslations } from 'next-intl/server';

import { routing } from '@/i18n/routing';
import { getProfile } from '@/lib/api/profile';

/**
 * O manifest do site.
 *
 * <p><strong>Sem service worker, e a ausencia e decisao registrada na secao 9.5
 * do plano.</strong> Um site servido por CDN com HTML estatico nao ganha nada
 * mensuravel com cache offline, e ganha um vetor de defeito dificil de depurar -
 * o service worker que serve a versao antiga depois do deploy. O manifest fica
 * pelo que ele faz sozinho: nome, cores e icone de quem salva o site na tela
 * inicial.
 *
 * <p>O icone e o mesmo SVG da aba, declarado com {@code sizes: 'any'}. O plano
 * pedia tambem PNGs de 192 e 512 e um icone {@code maskable}; eles servem a
 * instalacao como aplicativo, que sem service worker nao e objetivo deste site.
 *
 * <p>As cores sao o {@code --color-bg} do tema escuro, escritas como valor pela
 * mesma razao das metas {@code theme-color} do layout: o manifest nao le CSS.
 */
export default async function manifest(): Promise<MetadataRoute.Manifest> {
  const perfil = await getProfile();
  // O manifest e um so para o site inteiro, e fala o idioma padrao: e o
  // endereco sem prefixo que quem instala estava visitando.
  const t = await getTranslations({ locale: routing.defaultLocale, namespace: 'Metadata' });
  const [primeiroNome] = perfil.fullName.split(' ');

  return {
    name: perfil.fullName,
    short_name: primeiroNome ?? perfil.fullName,
    description: t('siteDescription', { nome: perfil.fullName }),
    lang: routing.defaultLocale,
    start_url: '/',
    display: 'standalone',
    background_color: '#08080a',
    theme_color: '#08080a',
    icons: [{ src: '/icon.svg', sizes: 'any', type: 'image/svg+xml' }],
  };
}
