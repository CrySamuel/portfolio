'use server';

import { ApiError } from '@portfolio/api-client';
import { headers } from 'next/headers';
import { getTranslations } from 'next-intl/server';

import {
  CAMPOS,
  CAMPO_ARMADILHA,
  CAMPO_SEM_SCRIPT,
  CAMPO_TURNSTILE,
  type CampoDoContato,
  type EstadoDoContato,
} from '@/features/contact/schemas/contact-fields';
import { criarContactSchema } from '@/features/contact/schemas/contact-schema';
import { api } from '@/lib/api/client';
import { verificarTurnstile } from '@/lib/turnstile';

/**
 * Espera longa, e sem retentativa.
 *
 * <p>O plano gratuito do Render hiberna, e uma mensagem enviada logo depois de
 * um período parado cai no cold start de cerca de um minuto (ADR-0006). O
 * `submitContact` não retenta de propósito — repetir um POST pode gravar duas
 * mensagens —, então o único amortecedor é o tempo de uma tentativa só.
 *
 * <p>Vinte segundos é o compromisso: cobre a maior parte dos despertares e é
 * bem menos do que os 90s que o `getProfile` se permite, porque ali quem espera
 * é o build e aqui é uma pessoa olhando um botão girar.
 */
const TIMEOUT_MS = 20_000;

/*
 * As frases desta ação moram nas mensagens, em `Contact`, uma versão por
 * idioma. O porquê de cada uma fica aqui, junto do código que escolhe entre
 * elas.
 *
 * <p>`genericFailure` aparece quando não há nada útil a dizer sobre a falha, e
 * sempre oferece a saída alternativa. Um formulário que falha sem dizer
 * "escreva direto para este endereço" transforma um problema nosso em uma
 * oportunidade perdida — e o e-mail está logo ao lado, na mesma seção.
 *
 * <p>`turnstileRefused` e `turnstileMissing` são as duas frases do antirrobô, e
 * **separá-las foi correção de um defeito medido**.
 *
 * <p>A primeira versão tinha uma frase só — "a verificação não passou,
 * recarregue a página" — para os dois casos. Exercitando o formulário,
 * apareceu o cenário em que ela mente: enviar antes de o widget terminar de
 * carregar produz submissão sem token, e o conselho de recarregar não conserta
 * nada. Pior, ele é o conselho errado justamente para quem tem o script da
 * Cloudflare barrado por bloqueador ou por proxy corporativo — para essa pessoa
 * o token nunca vai existir, e recarregar dez vezes dá no mesmo.
 *
 * <p>Os dois casos são diferentes e agora dizem coisas diferentes: token
 * recusado é sobre <em>esta</em> tentativa; token ausente é sobre o widget não
 * ter chegado, e a saída é o e-mail ao lado.
 */

/**
 * Recebe o formulário de contato — **o único caminho de escrita do site**.
 *
 * <p><strong>Esta Server Action é o BFF, e não há Route Handler ao lado
 * dela.</strong> A §16 do plano prevê os dois (`app/api/contact/route.ts` e esta
 * ação), e entregar ambos seria abrir duas portas para a mesma sala. A ação já
 * cumpre inteiramente o papel que o plano atribui ao handler: roda no servidor,
 * guarda a `TURNSTILE_SECRET_KEY` e a `SERVICE_API_KEY`, e o navegador nunca
 * fala com a API Java. O handler acrescentaria só uma diferença, e ela é
 * negativa — um `POST` público e sem identificação, que qualquer robô alcança
 * direto, enquanto o endpoint de uma Server Action exige o identificador que o
 * Next gera a cada build.
 *
 * <p><strong>A ordem das checagens é deliberada.</strong> Primeiro o formato,
 * porque é o erro que uma pessoa comete e precisa ver apontado no campo; depois
 * o Turnstile, que custa uma requisição a terceiro e não deve ser gasto com
 * corpo malformado; por último a API, que aplica limite de taxa e campo-armadilha
 * — os dois já escritos e testados do lado Java, e que não são reimplementados
 * aqui para não existirem em dois lugares que precisem concordar.
 *
 * <p><strong>O campo-armadilha atravessa esta função sem ser lido.</strong> Quem
 * decide o que fazer com ele é o `ContactController`, que responde 202 e não
 * grava nada — em silêncio, porque devolver erro ensinaria ao robô qual campo
 * evitar. Duplicar a decisão aqui criaria duas respostas possíveis para o mesmo
 * corpo.
 */
export async function submitContact(
  _anterior: EstadoDoContato,
  formulario: FormData,
): Promise<EstadoDoContato> {
  // O idioma é o da requisição: o middleware também atende o POST do
  // formulário, inclusive o envio sem JavaScript.
  const [t, validacao] = await Promise.all([
    getTranslations('Contact'),
    getTranslations('ContactValidation'),
  ]);
  const bruto = lerCampos(formulario);
  const analise = criarContactSchema((chave, valores) => validacao(chave, valores)).safeParse(
    bruto,
  );

  if (!analise.success) {
    return {
      estado: 'recusado',
      mensagem: t('checkFields'),
      erros: primeiroErroPorCampo(analise.error.issues),
      valores: bruto,
    };
  }

  const origem = await ipDaOrigem();
  const antirrobo = await passouNoTurnstile(formulario, origem);

  if (antirrobo !== 'aprovado') {
    return {
      estado: 'recusado',
      mensagem: antirrobo === 'sem-token' ? t('turnstileMissing') : t('turnstileRefused'),
      erros: {},
      valores: bruto,
    };
  }

  // O identificador que liga este log ao da API - o CorrelationIdFilter de lá o
  // põe em cada linha da requisição. Só aqui, e não nas leituras de conteúdo: a
  // chave do cache de dados do Next inclui os cabeçalhos, e um valor novo a cada
  // chamada desligaria o cache.
  const correlacao = crypto.randomUUID();

  try {
    await api.submitContact(
      {
        ...analise.data,
        // O valor vai como veio, sem `trim` e sem leitura. Normalizá-lo aqui
        // seria começar a tratar o campo como dado.
        [CAMPO_ARMADILHA]: texto(formulario, CAMPO_ARMADILHA),
      },
      {
        timeoutMs: TIMEOUT_MS,
        // Sem isto, o limite de cinco mensagens por hora contaria por servidor
        // da Vercel em vez de por remetente — e o site inteiro dividiria um
        // balde só. O cabeçalho é falsificável, e o `ContactController`
        // documenta por que isso é aceito: contra adversário dedicado quem
        // trabalha é o Turnstile, que já rodou acima.
        headers: {
          'X-Correlation-Id': correlacao,
          ...(origem === null ? {} : { 'X-Forwarded-For': origem }),
        },
      },
    );
  } catch (causa) {
    return {
      estado: 'recusado',
      mensagem: mensagemDaFalha(causa, t, correlacao),
      erros: {},
      valores: bruto,
    };
  }

  return { estado: 'enviado' };
}

function lerCampos(formulario: FormData): Record<CampoDoContato, string> {
  return Object.fromEntries(CAMPOS.map((campo) => [campo, texto(formulario, campo)])) as Record<
    CampoDoContato,
    string
  >;
}

/**
 * Um campo do formulário como texto, sem confiar no tipo.
 *
 * <p>`FormData.get` devolve `string | File | null`, e o `File` não é hipótese
 * acadêmica: basta alguém montar o POST à mão com um `<input type="file">` de
 * mesmo `name`. Um `String(valor)` gravaria `[object File]` no banco como se
 * fosse o que a pessoa escreveu; o teste de tipo devolve vazio, que é o que a
 * validação logo abaixo sabe recusar.
 */
function texto(formulario: FormData, campo: string): string {
  const valor = formulario.get(campo);
  return typeof valor === 'string' ? valor : '';
}

/**
 * Um erro por campo, o primeiro de cada.
 *
 * <p>O Zod pode devolver vários por campo — vazio e curto demais chegam juntos.
 * Mostrar os dois embaixo do mesmo input daria à pessoa duas frases para
 * resolver um problema só, e a segunda quase sempre some quando a primeira é
 * corrigida.
 */
function primeiroErroPorCampo(
  problemas: readonly { readonly path: readonly PropertyKey[]; readonly message: string }[],
): Partial<Record<CampoDoContato, string>> {
  const erros: Partial<Record<CampoDoContato, string>> = {};

  for (const problema of problemas) {
    const campo = problema.path[0];
    if (typeof campo !== 'string') continue;
    if (!ehCampoDoContato(campo)) continue;
    erros[campo] ??= problema.message;
  }

  return erros;
}

function ehCampoDoContato(valor: string): valor is CampoDoContato {
  return (CAMPOS as readonly string[]).includes(valor);
}

/**
 * O IP de quem enviou, ou `null` quando não dá para saber.
 *
 * <p>O primeiro valor de `X-Forwarded-For` é o cliente; os seguintes são os
 * saltos. Em produção quem escreve esse cabeçalho é a Vercel, na borda dela.
 * Localmente ele não existe, e `null` é a resposta honesta — melhor do que
 * inventar `127.0.0.1` e fazer o Turnstile conferir um endereço que ninguém usou.
 */
async function ipDaOrigem(): Promise<string | null> {
  const cabecalhos = await headers();
  const encaminhado = cabecalhos.get('x-forwarded-for');
  if (encaminhado === null) return null;

  const primeiro = encaminhado.split(',')[0]?.trim();
  return primeiro === undefined || primeiro === '' ? null : primeiro;
}

/**
 * O veredito do Turnstile para esta submissão.
 *
 * <p><strong>Sem token e sem JavaScript, passa — e este é o buraco conhecido
 * desta função.</strong> O widget do Turnstile é JavaScript puro: com script
 * desligado, nenhum token existe. A Definition of Done do MVP 5 exige que o
 * formulário funcione sem JavaScript, e as duas exigências não cabem juntas —
 * uma delas cede.
 *
 * <p>Cedeu o Turnstile, e o custo está medido: um robô que envie o marcador do
 * `<noscript>` pula a verificação. O que sobra contra ele são as outras camadas,
 * que não dependem de script nenhum — o campo-armadilha e o limite de cinco
 * mensagens por hora por IP, ambos do lado Java. O prejuízo máximo é cinco
 * mensagens por hora por endereço, e não uma enxurrada.
 *
 * <p><strong>Reverter é apagar o `<noscript>` do `ContactForm`.</strong> Sem o
 * marcador, toda submissão sem token é recusada e o formulário passa a exigir
 * JavaScript. A decisão é do dono; ela está aqui em vez de escondida porque o
 * custo de cada lado é conhecido e nenhum dos dois é obviamente maior.
 *
 * <p><strong>`sem-token` não é `recusado`</strong>, e a distinção existe porque
 * a mensagem que cada um merece é diferente — ver `turnstileMissing`, no topo.
 *
 * <p><strong>Cloudflare fora do ar deixa passar</strong>, e isso é escolha
 * separada da anterior. Bloquear seria trocar uma indisponibilidade de terceiro
 * por um formulário quebrado — exatamente o que o ADR-0008 recusa na integração
 * com o GitHub. Um portfólio que perde a mensagem de um recrutador porque a
 * Cloudflare piscou perdeu mais do que ganharia barrando o robô daquele minuto.
 */
async function passouNoTurnstile(
  formulario: FormData,
  origem: string | null,
): Promise<'aprovado' | 'sem-token' | 'recusado'> {
  const token = texto(formulario, CAMPO_TURNSTILE);

  if (token === '') {
    if (formulario.get(CAMPO_SEM_SCRIPT) === null) return 'sem-token';

    console.warn('Mensagem aceita sem Turnstile: o remetente declarou estar sem JavaScript');
    return 'aprovado';
  }

  // `indisponivel` cai aqui como aprovação, e é o parágrafo da Cloudflare fora
  // do ar, acima.
  return (await verificarTurnstile(token, origem)) === 'recusado' ? 'recusado' : 'aprovado';
}

/**
 * A frase que a pessoa lê, a partir do erro que a API devolveu.
 *
 * <p><strong>O `detail` da API nunca é repassado como está.</strong> Ele traz a
 * URL chamada, e ela é interna: publicá-la entregaria o endereço da API Java a
 * quem só vê o site. O que atravessa é o status, traduzido.
 */
function mensagemDaFalha(causa: unknown, t: Tradutor, correlacao: string): string {
  if (!(causa instanceof ApiError)) {
    console.error('Falha inesperada ao enviar o contato [correlationId %s]', correlacao, causa);
    return t('genericFailure');
  }

  console.error('A API recusou o contato [correlationId %s]: %s', correlacao, causa.message);

  if (causa.status === 429) {
    return t('rateLimited', { espera: esperaLegivel(causa.retryAfterSeconds, t) });
  }

  // 400 aqui significa que o Zod e a Bean Validation discordaram — o formulário
  // aceitou algo que a API recusa. É defeito nosso, não da pessoa, e por isso a
  // frase não manda "conferir os campos": não há o que ela possa corrigir.
  if (causa.status === 400) {
    return t('rejectedByServer');
  }

  return t('genericFailure');
}

/**
 * "Tente de novo em 12 minutos", e não "em 720 segundos".
 *
 * <p>Segundos acima de um minuto são um número que a pessoa tem de converter na
 * cabeça para saber se vale esperar. Arredonda para cima porque arredondar para
 * baixo produz a única versão errada: quem volta no minuto anunciado leva o
 * mesmo 429 de novo.
 */
function esperaLegivel(segundos: number | null, t: Tradutor): string {
  if (segundos === null || segundos <= 0) return t('retryLater');
  if (segundos < 60) return t('retryInSeconds', { segundos });

  return t('retryInMinutes', { minutos: Math.ceil(segundos / 60) });
}

/** O tradutor de `Contact`, do jeito que o next-intl o entrega à ação. */
type Tradutor = Awaited<ReturnType<typeof getTranslations<'Contact'>>>;
