import type { ReactNode } from 'react';

import { AboutSection } from '@/features/about/components/AboutSection';
import { ContactSection } from '@/features/contact/components/ContactSection';
import { GitHubSection } from '@/features/github/components/GitHubSection';
import { HeroSection } from '@/features/hero/components/HeroSection';
import { ProjectsSection } from '@/features/projects/components/ProjectsSection';
import { SkillsSection } from '@/features/skills/components/SkillsSection';

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
 * O arquivo e `app/page.tsx`, e nao `app/[locale]/page.tsx` como a secao 16
 * lista. O segmento dinamico de idioma so faz sentido com o roteador, o
 * middleware e as mensagens do next-intl, que o plano agenda para o commit 51 -
 * criar a pasta antes deixaria `/` sem pagina, porque todo caminho passaria a
 * exigir prefixo de idioma sem nada para resolve-lo.
 */
export default function HomePage(): ReactNode {
  return (
    <>
      <HeroSection />
      <AboutSection />
      <ProjectsSection />
      <SkillsSection />
      <GitHubSection />
      <ContactSection />
    </>
  );
}
