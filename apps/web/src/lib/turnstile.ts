import 'server-only';

import { env } from '@/lib/env';

/**
 * O endereço de verificação do Turnstile.
 *
 * <p>Fixo, e não configurável: uma variável de ambiente aqui seria um lugar por
 * onde alguém com acesso ao painel redireciona a verificação para um servidor
 * que responde `success: true` a tudo. Endereço de guarda de segurança não é
 * configuração.
 */
const SITEVERIFY = 'https://challenges.cloudflare.com/turnstile/v0/siteverify';

/**
 * Quanto se espera pela Cloudflare.
 *
 * <p>Curto de propósito. A verificação está no caminho de alguém que já apertou
 * o botão, e o Turnstile é uma camada entre várias — deixar o visitante
 * esperando trinta segundos por ela seria trocar spam por abandono.
 */
const TIMEOUT_MS = 8_000;

/**
 * O veredito da Cloudflare sobre um token.
 *
 * <p>Três estados, e não dois, porque `indisponivel` não é `recusado`: o
 * primeiro é falha nossa — rede, timeout, chave errada — e o segundo é um
 * julgamento sobre quem está enviando. Confundi-los faria uma instabilidade da
 * Cloudflare virar acusação de robô contra uma pessoa real.
 */
export type VereditoDoTurnstile = 'aprovado' | 'recusado' | 'indisponivel';

/**
 * A resposta do siteverify, na parte que interessa.
 *
 * <p>O campo `error-codes` é lido só para o log. Ele nunca chega ao visitante:
 * `invalid-input-secret` diria a quem tenta que a chave do servidor está errada,
 * e `timeout-or-duplicate` diria que tokens são reaproveitáveis até expirar —
 * duas dicas para quem está sondando, e nenhuma informação para quem só queria
 * mandar uma mensagem.
 */
interface RespostaDoSiteverify {
  readonly success?: unknown;
  readonly 'error-codes'?: unknown;
}

/**
 * Confere um token do Turnstile com a Cloudflare.
 *
 * <p><strong>O `remoteip` vai junto, e é por isso que esta função recebe o IP.</strong>
 * A Cloudflare emite o token para um endereço; conferir contra outro é o que
 * detecta um token legítimo capturado numa máquina e reenviado de outra. Sem
 * ele, a verificação continua respondendo — só deixa de enxergar essa classe.
 *
 * <p><strong>Falha de rede resulta em `indisponivel`, e quem chama decide.</strong>
 * Esta função não escolhe entre bloquear e deixar passar quando a Cloudflare não
 * responde: a escolha depende do que está do outro lado, e quem sabe isso é o
 * caso de uso, não o adaptador. A decisão está registrada no `submit-contact`.
 */
export async function verificarTurnstile(
  token: string,
  ip: string | null,
): Promise<VereditoDoTurnstile> {
  const corpo = new URLSearchParams({ secret: env.TURNSTILE_SECRET_KEY, response: token });
  if (ip !== null) corpo.set('remoteip', ip);

  let resposta: Response;
  try {
    resposta = await fetch(SITEVERIFY, {
      method: 'POST',
      body: corpo,
      signal: AbortSignal.timeout(TIMEOUT_MS),
      // O veredito vale para um token e um instante. Cachear seria guardar a
      // resposta de um token para servi-la a outro — e o Next cacheia fetch em
      // Server Action por padrão em algumas versões, então dizer "não" aqui é
      // mais barato do que descobrir a versão em que ele diz "sim".
      cache: 'no-store',
    });
  } catch (causa) {
    console.error('Turnstile inalcancavel', causa);
    return 'indisponivel';
  }

  if (!resposta.ok) {
    console.error('Turnstile respondeu %d', resposta.status);
    return 'indisponivel';
  }

  let json: RespostaDoSiteverify;
  try {
    json = (await resposta.json()) as RespostaDoSiteverify;
  } catch (causa) {
    console.error('Turnstile respondeu algo que nao e JSON', causa);
    return 'indisponivel';
  }

  // `=== true`, e não coerção: a resposta é JSON de terceiro, então `success`
  // chega como `unknown`. Um `if (json.success)` trataria a string "false" como
  // aprovação — e é exatamente assim que uma guarda passa a autorizar tudo sem
  // que nada quebre.
  if (json.success === true) return 'aprovado';

  console.warn('Turnstile recusou o token: %o', json['error-codes']);
  return 'recusado';
}
