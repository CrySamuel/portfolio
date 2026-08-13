import { statSync } from 'node:fs';
import { join } from 'node:path';

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
            <ResumeDownload
              href={profile.resumeUrl}
              sizeInBytes={tamanhoDoArquivoPublico(profile.resumeUrl)}
              fileName="crystofer-demetino-backend-2026.pdf"
            />
          </div>
        )}
      </Container>
    </Section>
  );
}

/**
 * O tamanho do arquivo, lido do disco em tempo de build.
 *
 * <p><strong>Medido, e nao escrito a mao.</strong> Um "77 KB" fixo no codigo vira
 * mentira na primeira vez que o curriculo for atualizado - e ele ja foi trocado
 * uma vez antes mesmo de entrar no repositorio. Duas fontes de verdade para o
 * mesmo fato divergem; aqui so ha uma, que e o proprio arquivo.
 *
 * <p>A leitura acontece no servidor, durante a pre-renderizacao, entao nao custa
 * nada ao visitante nem vai para o bundle. O caminho sai do `resumeUrl` porque
 * ele ja e relativo a raiz publica - a mesma premissa que faz o atributo
 * `download` funcionar, ja que ele exige mesma origem.
 *
 * <p>Sem try/catch de proposito. Arquivo ausente derruba o build, e e o que se
 * quer: a alternativa seria publicar um botao que responde 404, e defeito que
 * aparece so para o visitante e pior do que build vermelho. E a mesma escolha que
 * `getProfile` faz ao nao engolir falha de API.
 */
function tamanhoDoArquivoPublico(caminhoPublico: string): number {
  return statSync(join(process.cwd(), 'public', caminhoPublico)).size;
}
