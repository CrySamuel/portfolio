import { Badge, Button } from '@portfolio/ui';
import type { ReactNode } from 'react';

import { Container } from '@/components/layout/Container';
import { headingId, Section } from '@/components/layout/Section';
import { getProfile } from '@/lib/api/profile';

const SECTION_ID = 'inicio';

/**
 * A primeira dobra, com o conteudo vindo do banco.
 *
 * Server Component assincrono que busca o proprio dado (secao 8.5): a secao
 * sabe do que precisa, entao nenhuma pagina acima dela precisa saber - e mover a
 * secao de lugar nao arrasta uma prop atras.
 *
 * <p>Aqui esta o LCP da pagina, e ele e texto. Nada nesta secao carrega imagem,
 * fonte extra ou animacao de entrada: o titulo pinta na primeira passagem. A
 * unica animacao e o ponto do selo de disponibilidade, que nao participa da
 * medida e ainda respeita `prefers-reduced-motion`.
 */
export async function HeroSection(): Promise<ReactNode> {
  const profile = await getProfile();

  const github = profile.socialLinks.find((link) => link.platform === 'github');
  const email = profile.socialLinks.find((link) => link.platform === 'email');

  return (
    <Section id={SECTION_ID}>
      <Container className="flex flex-col items-start gap-8">
        {profile.availableForWork ? <AvailabilityBadge /> : null}

        <div className="flex flex-col gap-4">
          {/*
            O <h1> da pagina, e o unico. O id fecha o aria-labelledby que o
            Section aponta por padrao, entao a regiao passa a se chamar pelo
            nome de quem assina o portfolio.
          */}
          <h1 id={headingId(SECTION_ID)} className="text-display text-balance">
            {profile.fullName}
          </h1>

          <p className="max-w-reading text-body-lg text-pretty text-fg-muted">{profile.headline}</p>
        </div>

        <p className="max-w-reading text-body text-pretty text-fg-muted">{profile.bio}</p>

        {/*
          location e resumeUrl sao os dois campos que a API declara nulaveis, e
          o tipo gerado obriga a tratar isso - `string | null`, e nao `string`.
          O contrato preciso vira codigo que nao esquece o caso.
        */}
        {profile.location === null ? null : (
          <p className="text-body-sm text-fg-subtle">{profile.location}</p>
        )}

        <div className="flex flex-wrap items-center gap-3">
          {/*
            As chamadas para acao saem dos proprios links do perfil, em vez de
            apontarem para ancoras de secoes que ainda nao existem. Link que nao
            leva a lugar nenhum e pior do que link ausente.
          */}
          {github ? (
            <Button asChild size="lg">
              <a href={github.url} rel="me noopener">
                Ver o código no GitHub
              </a>
            </Button>
          ) : null}

          {email ? (
            <Button asChild variant="outline" size="lg">
              <a href={email.url}>Falar comigo</a>
            </Button>
          ) : null}
        </div>
      </Container>
    </Section>
  );
}

/**
 * Disponibilidade anunciada por ponto **e** texto.
 *
 * Nunca so por cor: a secao 11 do plano segue o criterio 1.4.1 da WCAG, e um
 * ponto verde sozinho nao diz nada a quem nao distingue verde de vermelho - nem
 * a quem usa leitor de tela. O ponto e decorativo e esta marcado como tal; quem
 * carrega o significado e a frase.
 */
function AvailabilityBadge(): ReactNode {
  return (
    <Badge variant="success">
      <span aria-hidden className="size-2 rounded-full bg-success motion-safe:animate-pulse" />
      Disponível para propostas
    </Badge>
  );
}
