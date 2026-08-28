'use client';

import { Button, cn } from '@portfolio/ui';
import { useActionState, useEffect, useId, useRef, type ReactNode } from 'react';

import { submitContact } from '@/features/contact/actions/submit-contact';
import {
  CAMPOS,
  CAMPO_ARMADILHA,
  CAMPO_SEM_SCRIPT,
  ESTADO_INICIAL,
  LIMITES,
  type CampoDoContato,
} from '@/features/contact/schemas/contact-fields';

/**
 * O que o Turnstile expõe no `window` depois que o script dele carrega.
 *
 * <p>Declarado aqui, e no mínimo: o pacote de tipos oficial traria a API inteira
 * para três chamadas. O `?.` em cada uso cobre o script não ter carregado —
 * bloqueador de conteúdo, rede ruim, ou a própria Cloudflare fora.
 */
declare global {
  interface Window {
    turnstile?: {
      render: (
        alvo: HTMLElement,
        opcoes: {
          sitekey: string;
          theme?: 'auto' | 'light' | 'dark';
          language?: string;
        },
      ) => string | undefined;
      reset: (widget?: string) => void;
      remove: (widget?: string) => void;
    };
  }
}

/**
 * O endereço do script, com `render=explicit`.
 *
 * <p><strong>O parâmetro desliga a renderização automática, e ele entrou por
 * causa de um defeito medido — não por preferência.</strong>
 *
 * <p>A primeira versão usava a renderização implícita: o script varre a página
 * atrás de `.cf-turnstile` e monta o widget sozinho, sem custar uma linha de
 * JavaScript nosso. O que ela custa é uma corrida. O script insere um
 * {@code <iframe>} dentro do {@code <div>} que o React declara vazio, e quando
 * essa inserção acontece <em>antes</em> da hidratação o React encontra um filho
 * que não estava no HTML do servidor: <strong>erro #418</strong>. A hidratação
 * falha, o React descarta a árvore e redesenha tudo do zero no cliente — o que
 * joga fora o {@code <iframe>} junto. O widget que a Cloudflare ainda acha que
 * existe passa a apontar para um nó solto, e o {@code reset()} seguinte lança
 * {@code Nothing to reset found for provided container}.
 *
 * <p><strong>E aí a página inteira caía</strong>, porque a exceção estourava
 * dentro de um efeito do React. Observado: `Application error: a client-side
 * exception has occurred`, com o site substituído por uma tela em branco depois
 * de uma recusa de validação. Intermitente por construção — é uma corrida —, e
 * foi vista em duas de cinco recargas.
 *
 * <p>Com a renderização explícita a corrida deixa de existir: o React comita um
 * {@code <div>} vazio, a hidratação compara vazio com vazio, e o widget só entra
 * depois — pela nossa mão, num efeito. O preço são as ~15 linhas do efeito
 * abaixo, e elas se pagam de novo no {@code reset}, que passa a receber o
 * identificador do widget em vez de procurá-lo por conta própria.
 */
const SCRIPT_DO_TURNSTILE = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';

/** De quanto em quanto tempo se olha se o script já chegou. */
const INTERVALO_DE_ESPERA_MS = 100;

/** Rótulo e forma de cada campo. A ordem de exibição é a de `CAMPOS`. */
const DESENHO: Record<CampoDoContato, { readonly rotulo: string; readonly area: boolean }> = {
  name: { rotulo: 'Nome', area: false },
  email: { rotulo: 'E-mail', area: false },
  subject: { rotulo: 'Assunto', area: false },
  message: { rotulo: 'Mensagem', area: true },
};

/**
 * O formulário de contato.
 *
 * <p><strong>O único Client Component desta seção, e ele existe por dois
 * motivos que um Server Component não cobre:</strong> o estado que volta da ação
 * — para desenhar erro por campo sem recarregar — e o `reset` do widget do
 * Turnstile depois de uma recusa.
 *
 * <p><strong>Funciona sem JavaScript.</strong> É um `<form action={…}>` ligado a
 * uma Server Action: sem script, o navegador faz o POST nativo, o servidor
 * responde a página inteira já com os erros no lugar, e os valores digitados
 * voltam pelo `defaultValue`. O que se perde é o botão girando, o foco indo para
 * o campo errado e — este é o custo real — a verificação do Turnstile, que é
 * JavaScript por natureza. O `<noscript>` abaixo é o que declara essa situação
 * ao servidor, e o `submit-contact` documenta o buraco que ela abre.
 *
 * <p><strong>Sem validação em JavaScript além da nativa.</strong> Os atributos
 * `required` e `maxLength` fazem o navegador barrar o erro comum antes de
 * qualquer requisição, e custam zero byte. O Zod roda no servidor, onde a
 * validação vale para quem desligou o script e para quem manda o POST à mão —
 * subir o schema para o cliente acrescentaria peso ao bundle para repetir uma
 * checagem que o servidor tem de fazer de novo de qualquer jeito.
 *
 * <p><strong>Não há `gap` no contêiner externo, e a ausência é a decisão de
 * acessibilidade mais fácil de desfazer sem querer.</strong> As duas regiões
 * vivas precisam existir no documento <em>antes</em> de terem conteúdo — um
 * leitor de tela só anuncia o que entra num nó que ele já estava observando. Com
 * `gap`, cada região vazia cobraria um espaço visível o tempo todo, e a saída
 * óbvia seria escondê-las com `display: none` — que as tira da árvore de
 * acessibilidade e desliga justamente o que elas existem para fazer. Sem `gap`,
 * região vazia tem altura zero, e quem separa é a margem de quem tem conteúdo.
 */
export function ContactForm({
  turnstileSiteKey,
}: {
  readonly turnstileSiteKey: string;
}): ReactNode {
  const [estado, enviar, enviando] = useActionState(submitContact, ESTADO_INICIAL);
  const prefixo = useId();
  const formulario = useRef<HTMLFormElement>(null);
  const caixaDoWidget = useRef<HTMLDivElement>(null);
  const idDoWidget = useRef<string | undefined>(undefined);

  /*
    Monta o widget depois da hidratação — ver SCRIPT_DO_TURNSTILE para o defeito
    que essa ordem existe para evitar.

    A espera é por sondagem, e não pelo `onload=` que a documentação da
    Cloudflare sugere. O callback só funciona se estiver definido *antes* de o
    script executar, e quem decide essa ordem aqui é o React: ele iça o
    <script> no mesmo commit em que roda este efeito. Apostar em quem chega
    primeiro é o mesmo tipo de corrida que acabou de ser removida.
  */
  useEffect(() => {
    const alvo = caixaDoWidget.current;
    if (alvo === null) return;

    let desmontado = false;
    let agendamento: ReturnType<typeof setTimeout> | undefined;

    const montar = (): void => {
      if (desmontado) return;

      const turnstile = window.turnstile;
      if (turnstile === undefined) {
        agendamento = setTimeout(montar, INTERVALO_DE_ESPERA_MS);
        return;
      }

      // `theme: 'auto'` segue o `prefers-color-scheme` do sistema, e não o tema
      // escolhido no site: quem forçar o claro com o sistema no escuro vê o
      // widget em desacordo com a página. Casar os dois exigiria remontar o
      // widget a cada troca de tema, e remontar é justamente o que produz
      // token descartado no meio de um preenchimento.
      idDoWidget.current = turnstile.render(alvo, {
        sitekey: turnstileSiteKey,
        theme: 'auto',
        language: 'pt-br',
      });
    };

    montar();

    return () => {
      desmontado = true;
      if (agendamento !== undefined) clearTimeout(agendamento);

      // Sem isto, o modo estrito do React — que monta, desmonta e monta de novo
      // em desenvolvimento — deixaria dois widgets na tela.
      if (idDoWidget.current !== undefined) window.turnstile?.remove(idDoWidget.current);
      idDoWidget.current = undefined;
    };
  }, [turnstileSiteKey]);

  /*
    Duas coisas depois de uma recusa, e as duas só existem com script.

    A primeira é o token: o Turnstile emite um por submissão e o invalida no
    uso. Sem `reset`, a segunda tentativa manda o token gasto, a Cloudflare
    responde `timeout-or-duplicate`, e a pessoa vê "a verificação não passou"
    para um formulário que ela acabou de corrigir — um erro que se repete para
    sempre e cuja causa não aparece em lugar nenhum da tela.

    A segunda é o foco. Depois do envio ele fica no botão, e quem usa leitor de
    tela ouve o alerta mas não sabe onde está o campo citado. Levar o foco ao
    primeiro campo com erro é o que o critério 3.3.1 espera.
  */
  useEffect(() => {
    if (estado.estado !== 'recusado') return;

    /*
      O try/catch não é zelo excessivo: foi uma exceção deste `reset` que
      derrubou a página inteira durante a verificação. A causa direta está
      corrigida em SCRIPT_DO_TURNSTILE, e mesmo assim ele fica — quem lança
      aqui é código de terceiro rodando dentro de um efeito, e efeito que
      estoura leva a árvore junto. Um widget antispam que não conseguiu se
      reciclar é um problema pequeno; a página em branco é o maior que existe.
    */
    try {
      window.turnstile?.reset(idDoWidget.current);
    } catch (causa) {
      console.warn('Nao foi possivel reciclar o widget do Turnstile', causa);
    }

    const primeiroComErro = CAMPOS.find((campo) => estado.erros[campo] !== undefined);
    if (primeiroComErro === undefined) return;

    formulario.current?.querySelector<HTMLElement>(`[name="${primeiroComErro}"]`)?.focus();
  }, [estado]);

  return (
    <div>
      <div aria-live="polite">
        {estado.estado === 'enviado' ? (
          <div className="flex flex-col gap-2 rounded-lg border border-success/40 bg-surface p-6">
            <p className="text-h4 text-fg">Mensagem enviada.</p>
            <p className="text-body text-fg-muted">
              Ela já está guardada e a notificação saiu para o meu e-mail. Respondo no endereço que
              você deixou.
            </p>
          </div>
        ) : null}
      </div>

      {/*
        `role="alert"` e não `aria-live="polite"`: uma recusa interrompe o que a
        pessoa estava fazendo e precisa ser dita na hora, não quando o leitor de
        tela terminar a frase atual.
      */}
      <div role="alert">
        {estado.estado === 'recusado' ? (
          <p className="mb-5 rounded-lg border border-danger/40 bg-surface px-4 py-3 text-body-sm text-fg">
            {estado.mensagem}
          </p>
        ) : null}
      </div>

      {/*
        O formulário some depois do sucesso em vez de reaparecer vazio. Mantê-lo
        convidaria a um segundo envio idêntico — e o widget do Turnstile teria de
        ser reciclado no mesmo movimento, que é a armadilha do token gasto outra
        vez, agora no caminho feliz. Quem quiser escrever de novo recarrega; o
        limite é de cinco por hora de qualquer forma.
      */}
      {estado.estado === 'enviado' ? null : (
        <form ref={formulario} action={enviar} className="flex flex-col gap-5">
          {CAMPOS.map((campo) => (
            <Campo
              key={campo}
              nome={campo}
              prefixo={prefixo}
              erro={estado.estado === 'recusado' ? estado.erros[campo] : undefined}
              valor={estado.estado === 'recusado' ? (estado.valores[campo] ?? '') : ''}
            />
          ))}

          <ArmadilhaParaRobos prefixo={prefixo} />

          {/*
            A caixa do widget. Vazia para o React, e continua vazia: quem põe o
            iframe dentro dela é o efeito lá em cima, depois da hidratação. Um
            filho declarado aqui faria a reconciliação disputar o mesmo nó com a
            Cloudflare — que é a forma antiga do defeito descrito em
            SCRIPT_DO_TURNSTILE.
          */}
          <div ref={caixaDoWidget} />

          {/*
            O React 19 iça este <script> para o <head> e o deduplica sozinho, o
            que dispensa o next/script e o peso dele. `async` porque nada nesta
            página espera por ele: sem o script o formulário continua desenhado,
            e o que falta é o token — caso que a ação trata com uma frase
            própria.
          */}
          <script src={SCRIPT_DO_TURNSTILE} async />

          {/*
            O marcador que só existe sem script — ver `passouNoTurnstile`.

            `dangerouslySetInnerHTML` porque o navegador com script ligado trata
            o interior de <noscript> como **texto**, enquanto o React o
            renderizaria como elemento: a hidratação encontraria um nó de texto
            onde esperava um <input> e reclamaria de divergência. Passando a
            string crua, os dois lados concordam. Não há entrada de usuário
            aqui — é HTML literal escrito neste arquivo.
          */}
          <noscript
            dangerouslySetInnerHTML={{
              __html: `<input type="hidden" name="${CAMPO_SEM_SCRIPT}" value="1" />`,
            }}
          />

          <div className="flex flex-wrap items-center gap-4">
            <Button type="submit" size="lg" loading={enviando}>
              Enviar mensagem
            </Button>

            <p className="text-caption text-fg-subtle">Protegido pelo Turnstile, da Cloudflare.</p>
          </div>
        </form>
      )}
    </div>
  );
}

/**
 * Um campo, com rótulo, erro e as duas amarras que ligam os três.
 *
 * <p>`aria-describedby` aponta para o parágrafo do erro, e `aria-invalid` diz
 * que o campo está recusado. São coisas diferentes e as duas são necessárias: a
 * primeira faz o leitor de tela <strong>ler</strong> o motivo ao entrar no
 * campo; a segunda faz o campo constar como inválido. Só a cor da borda não
 * comunica nada a quem não a vê — é o critério 1.4.1 outra vez.
 *
 * <p>O `aria-describedby` é omitido quando não há erro, em vez de apontar para
 * um id que não existe: referência pendurada é o tipo de coisa que o axe acusa e
 * que envelhece mal.
 */
function Campo({
  nome,
  prefixo,
  erro,
  valor,
}: {
  readonly nome: CampoDoContato;
  readonly prefixo: string;
  readonly erro: string | undefined;
  readonly valor: string;
}): ReactNode {
  const { rotulo, area } = DESENHO[nome];
  const id = `${prefixo}-${nome}`;
  const idDoErro = `${id}-erro`;

  const classe = cn(
    'w-full rounded-md border bg-surface px-3 py-2.5 text-body text-fg',
    'transition-colors duration-(--duration-fast)',
    erro === undefined ? 'border-border-interactive' : 'border-danger',
  );

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className="text-body-sm font-medium text-fg">
        {rotulo}
      </label>

      {area ? (
        <textarea
          id={id}
          name={nome}
          defaultValue={valor}
          required
          maxLength={LIMITES[nome]}
          rows={6}
          autoComplete="off"
          aria-invalid={erro === undefined ? undefined : true}
          aria-describedby={erro === undefined ? undefined : idDoErro}
          className={classe}
        />
      ) : (
        <input
          id={id}
          name={nome}
          defaultValue={valor}
          required
          maxLength={LIMITES[nome]}
          // `type="email"` faz o navegador barrar o endereço malformado antes
          // de qualquer requisição, e ainda troca o teclado no celular. A
          // validação de verdade continua no servidor: a do navegador aceita
          // `a@b`, que a API recusa.
          type={nome === 'email' ? 'email' : 'text'}
          // `name` e `email` são os dois valores que o navegador já conhece de
          // quem digita, e preencher sozinho poupa a parte chata. `subject` não
          // tem token de autofill que sirva, então fica de fora.
          autoComplete={nome === 'name' ? 'name' : nome === 'email' ? 'email' : 'off'}
          aria-invalid={erro === undefined ? undefined : true}
          aria-describedby={erro === undefined ? undefined : idDoErro}
          className={classe}
        />
      )}

      {erro === undefined ? null : (
        <p id={idDoErro} className="text-body-sm text-danger">
          {erro}
        </p>
      )}
    </div>
  );
}

/**
 * O campo-armadilha: invisível para pessoas, comum para robôs.
 *
 * <p><strong>Fora da tela, e não `display: none`.</strong> Robô que se dá ao
 * trabalho de olhar o CSS pula o que está escondido pelo caminho óbvio; o que
 * está a dez mil pixels à esquerda continua parecendo um campo de verdade.
 *
 * <p>`aria-hidden` e `tabIndex={-1}` são o outro lado: quem usa leitor de tela
 * ou navega por teclado nunca chega aqui, então não existe o risco de uma pessoa
 * preencher a armadilha sem querer e ter a mensagem descartada em silêncio.
 *
 * <p>O rótulo existe, ainda que ninguém o leia, porque um `<input>` sem rótulo é
 * o que denuncia a armadilha a quem estiver procurando por ela.
 */
function ArmadilhaParaRobos({ prefixo }: { readonly prefixo: string }): ReactNode {
  const id = `${prefixo}-${CAMPO_ARMADILHA}`;

  return (
    <div aria-hidden="true" className="absolute -left-[9999px] h-px w-px overflow-hidden">
      <label htmlFor={id}>Site</label>
      <input id={id} type="text" name={CAMPO_ARMADILHA} tabIndex={-1} autoComplete="off" />
    </div>
  );
}
