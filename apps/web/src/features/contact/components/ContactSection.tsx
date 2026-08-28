import type { ReactNode } from 'react';

import { SectionHeading } from '@/components/common/SectionHeading';
import { Container } from '@/components/layout/Container';
import { Section } from '@/components/layout/Section';
import { ContactForm } from '@/features/contact/components/ContactForm';
import { ContactInfo } from '@/features/contact/components/ContactInfo';
import { env } from '@/lib/env';
import { getProfile } from '@/lib/api/profile';

/**
 * O id que a navbar aponta desde o commit 15.
 *
 * <p>Até aqui ele era **âncora morta**: o item "Contato" existia no `NAV_ITEMS`
 * e não havia nada no documento com este id, então o link não levava a lugar
 * nenhum e o `IntersectionObserver` do realce nunca o acendia. É o mesmo defeito
 * que a seção do GitHub produziria se entrasse na navbar (§4.56), com a
 * diferença de que este tinha prazo — e o prazo é este commit.
 */
const SECTION_ID = 'contato';

/**
 * A seção de contato: o fim do funil.
 *
 * <p>Server Component assíncrono que busca o próprio dado, como as demais
 * (§8.5). O que ele busca é o perfil, e só para o bloco ao lado do formulário —
 * o formulário em si não depende de nenhuma leitura.
 *
 * <p><strong>Ela não some quando a API está fora</strong>, e a diferença em
 * relação à seção do GitHub é deliberada. Lá, o dado <em>é</em> a seção: sem os
 * números não há o que desenhar. Aqui o dado é acessório e o formulário é o
 * conteúdo — e é justamente quando algo está fora do ar que alguém mais precisa
 * conseguir mandar uma mensagem. O `getProfile` não tem tratamento de erro por
 * decisão antiga: perfil indisponível derruba a página inteira, e não só esta
 * seção.
 *
 * <p><strong>A chave pública do Turnstile desce como prop.</strong> O
 * formulário é Client Component e poderia ler `process.env.NEXT_PUBLIC_…`
 * sozinho; ler aqui é o que faz a variável passar pela validação de ambiente do
 * `lib/env`, que derruba o boot quando ela falta em vez de deixar o widget
 * silenciosamente ausente em produção.
 */
export async function ContactSection(): Promise<ReactNode> {
  const profile = await getProfile();

  return (
    <Section id={SECTION_ID}>
      <Container className="flex flex-col gap-10">
        <SectionHeading
          sectionId={SECTION_ID}
          eyebrow="Contato"
          title="Vamos conversar"
          description="Escreva sobre uma vaga, um projeto ou uma dúvida técnica. A mensagem é gravada antes de qualquer tentativa de envio — se o e-mail falhar, ela é reenviada sozinha."
        />

        {/*
          O formulário primeiro no documento, e não só na tela larga. Em duas
          colunas o olho começa pela esquerda; em uma coluna, quem rola encontra
          o que veio antes no HTML. As duas ordens precisam concordar, e a que
          manda é a do documento — é ela que o teclado e o leitor de tela seguem.
        */}
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
          <ContactForm turnstileSiteKey={env.NEXT_PUBLIC_TURNSTILE_SITE_KEY} />
          <ContactInfo profile={profile} />
        </div>
      </Container>
    </Section>
  );
}
