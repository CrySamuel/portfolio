import jsxA11y from 'eslint-plugin-jsx-a11y';
import reactHooks from 'eslint-plugin-react-hooks';
import tseslint from 'typescript-eslint';

import { baseConfig } from './base.js';

/**
 * Configuracao para pacotes React.
 *
 * jsx-a11y entra no nivel strict, e nao no recommended: a acessibilidade e um
 * requisito do projeto (WCAG 2.2 AA, secao 12), nao um enfeite. Errar aqui em
 * tempo de escrita custa segundos; descobrir no axe do CI custa um ciclo
 * inteiro; descobrir com um leitor de tela custa a experiencia de alguem.
 *
 * @type {import('typescript-eslint').ConfigArray}
 */
export const reactConfig = tseslint.config(
  ...baseConfig,

  jsxA11y.flatConfigs.strict,
  // Em eslint-plugin-react-hooks 7, os presets de flat config vivem sob o
  // namespace configs.flat. configs['recommended-latest'] ainda existe, mas no
  // formato eslintrc antigo, com plugins como array de strings.
  reactHooks.configs.flat['recommended-latest'],

  {
    rules: {
      // Icone decorativo recebe aria-hidden; icone que E a acao recebe rotulo.
      // A regra abaixo exige que todo elemento interativo tenha nome acessivel.
      'jsx-a11y/control-has-associated-label': 'error',

      // "clique aqui" nao descreve destino nenhum (WCAG 2.4.4).
      'jsx-a11y/anchor-ambiguous-text': 'error',
    },
  },
);

export default reactConfig;
