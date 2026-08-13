import type { ReactNode } from 'react';

import { ResumeDownload } from '@/components/common/ResumeDownload';
import { SectionHeading } from '@/components/common/SectionHeading';
import { Container } from '@/components/layout/Container';
import { Section } from '@/components/layout/Section';
import { Timeline } from '@/features/about/components/Timeline';
import { listExperiences } from '@/lib/api/experiences';
import { getProfile } from '@/lib/api/profile';

const SECTION_ID = 'sobre';

/**
 * A secao Sobre: trajetoria profissional e download do curriculo.
 *
 * Server Component assincrono que busca o proprio dado, como o hero (secao 8.5):
 * a secao sabe do que precisa, entao a pagina nao precisa saber por ela.
 *
 * <p>As duas leituras custam duas requisicoes, e nao quatro. O `cache` do React
 * memoiza por passagem de renderizacao, entao o perfil pedido aqui e o mesmo que
 * o layout e o hero ja pediram - a chamada acontece uma vez para a tela inteira.
 *
 * <p>O `id` vem de `NAV_ITEMS`: a navbar ja aponta para `#sobre` desde o commit
 * 15, e ate agora o link levava a lugar nenhum. Este commit fecha essa ponta.
 */
export async function AboutSection(): Promise<ReactNode> {
  const [profile, experiences] = await Promise.all([getProfile(), listExperiences()]);

  return (
    <Section id={SECTION_ID}>
      <Container className="flex flex-col gap-10">
        <SectionHeading
          sectionId={SECTION_ID}
          eyebrow="Trajetória"
          title="Sobre"
          description="De suporte técnico a desenvolvimento backend, com automação no meio do caminho."
        />

        <Timeline experiences={experiences} />

        {/*
          O botao so existe quando ha curriculo publicado. O contrato declara
          resumeUrl como `string | null`, entao o tipo gerado obriga a tratar o
          caso - e o caso e real: o campo ficou nulo do commit 16 ate este.

          Renderizar um botao que baixa 404 seria pior do que nao ter botao.
        */}
        {profile.resumeUrl === null ? null : (
          <div>
            <ResumeDownload href={profile.resumeUrl} fileName="crystofer-demetino-cv.pdf" />
          </div>
        )}
      </Container>
    </Section>
  );
}
