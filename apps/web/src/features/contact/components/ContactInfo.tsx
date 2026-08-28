import type { Profile } from '@portfolio/api-client';
import type { ReactNode } from 'react';

/**
 * O caminho alternativo ao formulário, ao lado dele.
 *
 * <p><strong>Existe porque o formulário pode falhar.</strong> A API hiberna, a
 * Cloudflare pisca, o limite de taxa fecha a porta por uma hora — e em todos
 * esses casos a mensagem de erro manda escrever "direto para o e-mail ao lado".
 * Essa frase só é honesta se o endereço estiver de fato ao lado, visível sem
 * rolar e sem depender de nada que possa estar fora do ar.
 *
 * <p>Server Component: nada aqui precisa de estado, então nada aqui custa
 * JavaScript.
 *
 * <p>O endereço vem do perfil na API, e não de uma constante neste arquivo — é a
 * mesma regra que tirou o `SOCIAL_LINKS` do `navigation.ts` no commit 22. Se o
 * perfil não publicar e-mail, a seção mostra só o formulário: um bloco de
 * contato com um endereço inventado seria pior que a ausência dele.
 */
export function ContactInfo({ profile }: { readonly profile: Profile }): ReactNode {
  const email = profile.socialLinks.find((link) => link.platform === 'email');
  const linkedin = profile.socialLinks.find((link) => link.platform === 'linkedin');

  if (email === undefined && linkedin === undefined) return null;

  return (
    <div className="flex flex-col gap-6 rounded-lg border border-border bg-surface p-6">
      <div className="flex flex-col gap-2">
        <h3 className="text-h4 text-fg">Prefere ir direto?</h3>
        <p className="text-body-sm text-pretty text-fg-muted">
          O formulário guarda a mensagem antes de tentar me avisar, então ela não se perde. Mas se
          preferir o caminho curto, os dois endereços abaixo chegam em mim do mesmo jeito.
        </p>
      </div>

      {/*
        <dl> e não uma lista de links soltos: cada linha é um par rótulo-valor,
        e é isso que faz o leitor de tela anunciar "E-mail" antes do endereço em
        vez de ler uma sequência de URLs sem contexto.
      */}
      <dl className="flex flex-col gap-4">
        {email === undefined ? null : (
          <Endereco rotulo="E-mail" href={email.url} texto={semMailto(email.url)} />
        )}

        {linkedin === undefined ? null : (
          <Endereco rotulo="LinkedIn" href={linkedin.url} texto={semEsquema(linkedin.url)} />
        )}
      </dl>

      {profile.location === null ? null : (
        <p className="text-caption text-fg-subtle">{profile.location}</p>
      )}
    </div>
  );
}

function Endereco({
  rotulo,
  href,
  texto,
}: {
  readonly rotulo: string;
  readonly href: string;
  readonly texto: string;
}): ReactNode {
  return (
    <div className="flex flex-col gap-1">
      <dt className="text-caption text-fg-subtle uppercase">{rotulo}</dt>
      <dd>
        {/*
          `break-all` porque um endereço longo numa coluna estreita é o caso
          clássico de estouro horizontal: sem ele, a 320px o card empurra a
          página inteira para o lado.
        */}
        <a
          href={href}
          rel="me noopener"
          className="text-body break-all text-accent hover:underline"
        >
          {texto}
        </a>
      </dd>
    </div>
  );
}

/**
 * O endereço sem o `mailto:`.
 *
 * <p>O prefixo é do protocolo, não do endereço: mostrá-lo faria a pessoa copiar
 * `mailto:fulano@exemplo.com` ao selecionar o texto. O `href` continua com ele,
 * que é onde ele significa alguma coisa.
 */
function semMailto(url: string): string {
  return url.replace(/^mailto:/i, '');
}

/** A URL sem `https://` e sem a barra final — ruído em texto de leitura. */
function semEsquema(url: string): string {
  return url.replace(/^https?:\/\//i, '').replace(/\/$/, '');
}
