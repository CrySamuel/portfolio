import { describe, expect, it } from 'vitest';

import { IDIOMAS } from '@/i18n/idiomas';
import {
  caminhoInterno,
  caminhoPublico,
  caminhosPorIdioma,
  caminhoTraduzido,
} from '@/i18n/navigation';

/** Os caminhos internos que o site tem - os de pasta, em portugues e sem idioma. */
const ROTAS = [
  '/',
  '/projetos',
  '/projetos/finai',
  '/projetos/finai/opengraph-image',
  '/opengraph-image',
  '/rota-inexistente',
];

describe('caminhoPublico', () => {
  it('deixa o idioma padrao sem prefixo e com os nomes da pasta', () => {
    expect(caminhoPublico({ href: '/', locale: 'pt-BR' })).toBe('/');
    expect(caminhoPublico({ href: '/projetos/finai', locale: 'pt-BR' })).toBe('/projetos/finai');
  });

  it('prefixa os demais idiomas e traduz o primeiro segmento', () => {
    expect(caminhoPublico({ href: '/', locale: 'en-US' })).toBe('/en-US');
    expect(caminhoPublico({ href: '/projetos', locale: 'en-US' })).toBe('/en-US/projects');
    expect(caminhoPublico({ href: '/projetos/finai', locale: 'en-US' })).toBe(
      '/en-US/projects/finai',
    );
  });

  it('nao traduz o slug, mesmo quando ele coincide com um segmento traduzido', () => {
    expect(caminhoPublico({ href: '/projetos/projetos', locale: 'en-US' })).toBe(
      '/en-US/projects/projetos',
    );
  });
});

describe('caminhoTraduzido', () => {
  it('traduz moldes de rota sem prefixo - a forma dos pathnames do next-intl', () => {
    expect(caminhoTraduzido('/projetos/[slug]', 'en-US')).toBe('/projects/[slug]');
    expect(caminhoTraduzido('/projetos/[slug]', 'pt-BR')).toBe('/projetos/[slug]');
  });
});

describe('caminhosPorIdioma', () => {
  it('da o endereco de cada idioma - as alternativas do hreflang', () => {
    expect(caminhosPorIdioma('/projetos/finai')).toEqual({
      'pt-BR': '/projetos/finai',
      'en-US': '/en-US/projects/finai',
    });
  });
});

describe('caminhoInterno', () => {
  it.each(IDIOMAS.flatMap((locale) => ROTAS.map((href) => [locale, href] as const)))(
    'desfaz o caminhoPublico em %s para %s',
    (locale, href) => {
      expect(caminhoInterno(caminhoPublico({ href, locale }))).toEqual({ locale, href });
    },
  );

  /*
   * A forma que a pre-renderizacao ve: a rota, com o prefixo inclusive no idioma padrao e com o
   * nome da pasta. E o que o usePathname devolve no servidor, e o seletor de idioma depende de as
   * duas formas darem o mesmo resultado - senao o href muda na hidratacao.
   */
  it('aceita a forma interna, que e a que o servidor ve', () => {
    expect(caminhoInterno('/pt-BR/projetos/finai')).toEqual({
      locale: 'pt-BR',
      href: '/projetos/finai',
    });
    expect(caminhoInterno('/en-US/projetos/finai')).toEqual({
      locale: 'en-US',
      href: '/projetos/finai',
    });
    expect(caminhoInterno('/pt-BR')).toEqual({ locale: 'pt-BR', href: '/' });
  });
});
