import { ApiError } from '@portfolio/api-client';
import { headers } from 'next/headers';
import { createTranslator } from 'next-intl';
import { getTranslations } from 'next-intl/server';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { submitContact } from '@/features/contact/actions/submit-contact';
import {
  CAMPO_ARMADILHA,
  CAMPO_SEM_SCRIPT,
  CAMPO_TURNSTILE,
} from '@/features/contact/schemas/contact-fields';
import enUS from '@/i18n/messages/en-US.json';
import ptBR from '@/i18n/messages/pt-BR.json';
import { api } from '@/lib/api/client';
import { verificarTurnstile } from '@/lib/turnstile';

/*
 * Os cinco ramos do envio, que ate o commit 52 so tinham sido exercitados a mao (secao 8 do
 * ESTADO). As fronteiras sao dubles - a API, a Cloudflare, o cabecalho da requisicao -, e as
 * mensagens sao as de verdade, pelo tradutor do proprio next-intl: o teste confere a frase que a
 * pessoa le, plural inclusive, e nao o nome da chave.
 */
vi.mock('next/headers', () => ({ headers: vi.fn() }));
vi.mock('next-intl/server', () => ({ getTranslations: vi.fn() }));
vi.mock('@/lib/api/client', () => ({ api: { submitContact: vi.fn() } }));
vi.mock('@/lib/turnstile', () => ({ verificarTurnstile: vi.fn() }));

const MENSAGENS = { 'pt-BR': ptBR, 'en-US': enUS };

/** O idioma da requisicao, que o next-intl daria a Server Action. */
function falarEm(locale: keyof typeof MENSAGENS): void {
  vi.mocked(getTranslations).mockImplementation(((namespace: 'Contact' | 'ContactValidation') =>
    Promise.resolve(
      createTranslator({ locale, messages: MENSAGENS[locale], namespace }),
    )) as unknown as typeof getTranslations);
}

function formulario(campos: Record<string, string>): FormData {
  const dados = new FormData();
  for (const [nome, valor] of Object.entries(campos)) dados.set(nome, valor);
  return dados;
}

/** Um envio valido, com token do Turnstile e a armadilha vazia. */
function envioValido(extras: Record<string, string> = {}): FormData {
  return formulario({
    name: '  Ana Souza  ',
    email: 'ana@exemplo.com',
    subject: 'Vaga backend',
    message: 'Ola! Vi o portfolio e queria conversar.',
    [CAMPO_ARMADILHA]: '',
    [CAMPO_TURNSTILE]: 'token-do-widget',
    ...extras,
  });
}

const INICIAL = { estado: 'inicial' } as const;

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

beforeEach(() => {
  vi.resetAllMocks();
  falarEm('pt-BR');
  vi.mocked(headers).mockResolvedValue(new Headers({ 'x-forwarded-for': '203.0.113.7, 10.0.0.1' }));
  vi.mocked(verificarTurnstile).mockResolvedValue('aprovado');
  vi.mocked(api.submitContact).mockResolvedValue(undefined);
});

describe('submitContact', () => {
  it('envia, repassando a origem e a armadilha como vieram', async () => {
    const estado = await submitContact(INICIAL, envioValido({ [CAMPO_ARMADILHA]: '  x ' }));

    expect(estado).toEqual({ estado: 'enviado' });
    expect(api.submitContact).toHaveBeenCalledWith(
      {
        name: 'Ana Souza',
        email: 'ana@exemplo.com',
        subject: 'Vaga backend',
        message: 'Ola! Vi o portfolio e queria conversar.',
        website: '  x ',
      },
      expect.objectContaining({
        headers: {
          'X-Correlation-Id': expect.stringMatching(UUID) as unknown,
          'X-Forwarded-For': '203.0.113.7',
        },
      }),
    );
  });

  it('registra a falha com o mesmo identificador que mandou a API', async () => {
    const erro = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.mocked(api.submitContact).mockRejectedValue(
      new ApiError(400, '/api/v1/contact', 'Bad Request'),
    );

    await submitContact(INICIAL, envioValido());

    const enviado = (
      vi.mocked(api.submitContact).mock.calls[0]?.[1]?.headers as Record<string, string> | undefined
    )?.['X-Correlation-Id'];
    expect(enviado).toMatch(UUID);
    expect(erro).toHaveBeenCalledWith(
      expect.stringContaining('correlationId'),
      enviado,
      'Bad Request',
    );
  });

  it('recusa a validacao com o primeiro erro de cada campo, sem chamar a API', async () => {
    const estado = await submitContact(
      INICIAL,
      envioValido({ name: '   ', email: 'sem-arroba', message: 'x'.repeat(5_001) }),
    );

    expect(estado).toEqual({
      estado: 'recusado',
      mensagem: 'Confira os campos marcados abaixo.',
      erros: {
        name: 'Escreva seu nome.',
        email: 'Confira o e-mail: parece faltar alguma coisa.',
        message: 'A mensagem precisa caber em 5000 caracteres.',
      },
      valores: {
        name: '   ',
        email: 'sem-arroba',
        subject: 'Vaga backend',
        message: 'x'.repeat(5_001),
      },
    });
    expect(verificarTurnstile).not.toHaveBeenCalled();
    expect(api.submitContact).not.toHaveBeenCalled();
  });

  it('responde no idioma da requisicao', async () => {
    falarEm('en-US');

    const estado = await submitContact(INICIAL, envioValido({ name: '' }));

    expect(estado).toMatchObject({
      mensagem: 'Check the fields marked below.',
      erros: { name: 'Enter your name.' },
    });
  });

  it('sem token e sem o marcador de sem-JavaScript, diz que a verificacao nao carregou', async () => {
    const estado = await submitContact(INICIAL, envioValido({ [CAMPO_TURNSTILE]: '' }));

    expect(estado).toMatchObject({
      estado: 'recusado',
      mensagem: expect.stringContaining('A verificação anti-robô não carregou') as unknown,
    });
    expect(api.submitContact).not.toHaveBeenCalled();
  });

  it('sem token, mas sem JavaScript, aceita - o limite por hora segura o robo', async () => {
    vi.spyOn(console, 'warn').mockImplementation(() => undefined);

    const estado = await submitContact(
      INICIAL,
      envioValido({ [CAMPO_TURNSTILE]: '', [CAMPO_SEM_SCRIPT]: '1' }),
    );

    expect(estado).toEqual({ estado: 'enviado' });
    expect(verificarTurnstile).not.toHaveBeenCalled();
  });

  it('token recusado pela Cloudflare, nao envia', async () => {
    vi.mocked(verificarTurnstile).mockResolvedValue('recusado');

    const estado = await submitContact(INICIAL, envioValido());

    expect(estado).toMatchObject({
      estado: 'recusado',
      mensagem: 'A verificação anti-robô recusou esta tentativa. Tente enviar de novo.',
    });
    expect(api.submitContact).not.toHaveBeenCalled();
  });

  it('Cloudflare fora do ar deixa passar', async () => {
    vi.mocked(verificarTurnstile).mockResolvedValue('indisponivel');

    expect(await submitContact(INICIAL, envioValido())).toEqual({ estado: 'enviado' });
  });

  it('transforma o 429 em espera legivel, arredondando para cima', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.mocked(api.submitContact).mockRejectedValue(
      new ApiError(429, '/api/v1/contact', 'Too Many Requests', { retryAfterSeconds: 740 }),
    );

    const estado = await submitContact(INICIAL, envioValido());

    expect(estado).toMatchObject({
      mensagem: 'Você já enviou algumas mensagens hoje. Tente de novo em 13 minutos.',
    });
  });

  it('usa o singular no ingles quando falta um minuto', async () => {
    falarEm('en-US');
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.mocked(api.submitContact).mockRejectedValue(
      new ApiError(429, '/api/v1/contact', 'Too Many Requests', { retryAfterSeconds: 60 }),
    );

    const estado = await submitContact(INICIAL, envioValido());

    expect(estado).toMatchObject({
      mensagem: "You've already sent a few messages today. Try again in 1 minute.",
    });
  });

  it('400 da API e defeito nosso, e a frase nao manda conferir os campos', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.mocked(api.submitContact).mockRejectedValue(
      new ApiError(400, '/api/v1/contact', 'Bad Request'),
    );

    const estado = await submitContact(INICIAL, envioValido());

    expect(estado).toMatchObject({
      mensagem: expect.stringContaining('não passou na validação do servidor') as unknown,
      erros: {},
    });
  });

  it('falha inesperada vira a frase generica, com a saida pelo e-mail', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.mocked(api.submitContact).mockRejectedValue(new Error('rede caiu'));

    const estado = await submitContact(INICIAL, envioValido());

    expect(estado).toMatchObject({
      mensagem: expect.stringContaining('escreva direto para o e-mail ao lado') as unknown,
    });
  });
});
