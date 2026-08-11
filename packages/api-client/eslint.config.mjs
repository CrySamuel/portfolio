import { baseConfig } from '@portfolio/eslint-config/base';

/**
 * O codigo gerado fica fora do lint de proposito.
 *
 * Ele nao e escrito por ninguem, entao nao ha quem corrija um aviso; e sera
 * reescrito na proxima geracao, entao qualquer correcao seria perdida. Quem o
 * verifica e o `tsc`, que continua alcancando o diretorio - erro de tipo em
 * codigo gerado significa contrato quebrado, e esse precisa aparecer.
 */
export default [
  ...baseConfig,
  { ignores: ['src/generated/**'] },

  /**
   * O script de geracao roda no Node, e nao no navegador nem no bundle.
   *
   * Os globais sao declarados a mao, e nao pelo pacote `globals`: sao quatro
   * nomes, e uma dependencia a mais para lista-los seria pior do que a lista.
   *
   * `no-console` fica desligado aqui porque a saida do console **e** a interface
   * do script - quem o roda esta olhando o terminal. A regra existe para o
   * codigo de aplicacao, onde console.log e depuracao esquecida.
   */
  {
    files: ['scripts/**/*.mjs'],
    languageOptions: {
      globals: {
        URL: 'readonly',
        console: 'readonly',
        fetch: 'readonly',
        process: 'readonly',
      },
    },
    rules: { 'no-console': 'off' },
  },
];
