import { getRequestConfig } from 'next-intl/server';

import { ehIdioma, routing, type Idioma } from '@/i18n/routing';

import ptBR from './messages/pt-BR.json';

/**
 * As mensagens de cada idioma. Import estatico, e nao {@code import(`./messages/${locale}.json`)}:
 * o caminho montado em tempo de execucao passa pelo type-check sem conferir que o arquivo existe, e
 * o idioma esquecido apareceria como erro de build so na rota dele. Aqui, faltar um arquivo e erro
 * de compilacao.
 */
const MENSAGENS = { 'pt-BR': ptBR } satisfies Record<Idioma, unknown>;

/**
 * A configuracao de cada requisicao: o idioma, as mensagens e o fuso.
 *
 * <p><strong>O fuso e UTC, e a razao ja foi paga uma vez.</strong> As datas do conteudo sao so de
 * dia - inicio de cargo, publicacao de projeto -, e meia-noite UTC formatada em
 * {@code America/Sao_Paulo} vira o dia anterior: janeiro sai como dezembro. Os formatadores do site
 * ja levavam {@code timeZone: 'UTC'} por isso; aqui a escolha vale para tudo que o next-intl formatar.
 */
// O requestLocale esta deprecado pelo mesmo motivo do setRequestLocale, e fica pelo mesmo - ver
// fixarIdioma, em i18n/locale.
// eslint-disable-next-line @typescript-eslint/no-deprecated -- o substituto e experimental no Next 15.5
export default getRequestConfig(async ({ requestLocale }) => {
  const pedido = await requestLocale;
  const locale = ehIdioma(pedido) ? pedido : routing.defaultLocale;

  return {
    locale,
    messages: MENSAGENS[locale],
    timeZone: 'UTC',
  };
});
