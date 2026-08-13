import type { ReactNode } from 'react';

import { AboutSection } from '@/features/about/components/AboutSection';
import { HeroSection } from '@/features/hero/components/HeroSection';

/**
 * A home.
 *
 * Uma linha por secao, e nenhuma prop entre elas: cada secao busca o proprio
 * dado (secao 8.5 do plano). E o que permite acrescentar "sobre", "projetos" e
 * "skills" nos MVPs seguintes sem que esta pagina cresca junto.
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
    </>
  );
}
