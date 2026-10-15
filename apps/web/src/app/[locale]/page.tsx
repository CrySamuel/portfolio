import type { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import type { ReactNode } from 'react';

import { cartaoDoSite } from '@/app/[locale]/cartoes';
import { JsonLd } from '@/components/seo/JsonLd';
import { grafo, pessoa, site } from '@/components/seo/structured-data';
import { AboutSection } from '@/features/about/components/AboutSection';
import { ContactSection } from '@/features/contact/components/ContactSection';
import { GitHubSection } from '@/features/github/components/GitHubSection';
import { HeroSection } from '@/features/hero/components/HeroSection';
import { ProjectsSection } from '@/features/projects/components/ProjectsSection';
import { SkillsSection } from '@/features/skills/components/SkillsSection';
import { fixarIdioma, idiomaDaRota, type ParametrosDoIdioma } from '@/i18n/locale';
import { caminhoPublico } from '@/i18n/navigation';
import { getProfile } from '@/lib/api/profile';
import { listSkills } from '@/lib/api/skills';
import { metadataDaPagina, tituloDoSite } from '@/lib/metadata';

/**
 * O titulo da home e o titulo do site, por inteiro - sem o sufixo que o template
 * do layout poe nas demais rotas, que repetiria o nome duas vezes.
 */
export async function generateMetadata({ params }: ParametrosDoIdioma): Promise<Metadata> {
  const locale = await idiomaDaRota(params);
  const [t, perfil, cartao] = await Promise.all([
    getTranslations({ locale, namespace: 'Metadata' }),
    getProfile(),
    cartaoDoSite(locale),
  ]);

  return metadataDaPagina(perfil, {
    titulo: tituloDoSite(perfil),
    tituloAbsoluto: true,
    descricao: t('siteDescription', { nome: perfil.fullName }),
    caminho: caminhoPublico({ href: '/', locale }),
    cartao,
  });
}

/**
 * A home.
 *
 * Uma linha por secao, e nenhuma prop entre elas: cada secao busca o proprio
 * dado (secao 8.5 do plano). E o que permite acrescentar secao sem que esta
 * pagina cresca junto - "projetos" entrou com uma linha.
 *
 * A ordem segue a da navbar. Projetos vem depois de Sobre porque a leitura e
 * quem-sou, o-que-entreguei, o-que-sei: a prova de trabalho ganha mais peso
 * depois do contexto e antes da lista de competencias.
 *
 * Aqui a secao mostra so os projetos em destaque, e sem filtro. O catalogo
 * inteiro, com o filtro, vive em /projetos - e e la que o unico Client
 * Component do MVP 3 e carregado.
 *
 * Mora em `app/[locale]` desde o commit 51, como a secao 16 previa. O
 * portugues continua em `/`: o middleware reescreve o endereco sem prefixo para
 * esta rota (ver i18n/routing).
 *
 * Os dados estruturados da pessoa e do site moram aqui, e so aqui: a home e a
 * pagina sobre a pessoa. Perfil e skills ja sao buscados pelas secoes, e o
 * `cache` do React devolve as mesmas respostas - o JSON-LD nao custa requisicao.
 */
export default async function HomePage({ params }: ParametrosDoIdioma): Promise<ReactNode> {
  const locale = await idiomaDaRota(params);
  fixarIdioma(locale);

  const [perfil, competencias] = await Promise.all([getProfile(), listSkills()]);

  return (
    <>
      <JsonLd dados={grafo(pessoa(perfil, competencias), site(perfil))} />
      <HeroSection />
      <AboutSection />
      <ProjectsSection />
      <SkillsSection />
      <GitHubSection />
      <ContactSection />
    </>
  );
}
