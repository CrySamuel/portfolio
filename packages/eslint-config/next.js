import nextPlugin from '@next/eslint-plugin-next';
import tseslint from 'typescript-eslint';

import { reactConfig } from './react.js';

/**
 * Configuracao para a aplicacao Next.js.
 *
 * As regras core-web-vitals nao sao estilisticas: elas pegam padroes que
 * degradam LCP e CLS diretamente - <img> sem otimizacao, <a> para rota interna
 * sem prefetch, fonte carregada por <link> de terceiro. Sao o orcamento de
 * performance da secao 10 aplicado em tempo de escrita.
 *
 * @type {import('typescript-eslint').ConfigArray}
 */
export const nextConfig = tseslint.config(
  // Artefatos gerados pelo Next. O proprio next-env.d.ts traz o aviso de que
  // nao deve ser editado, entao nao faz sentido submete-lo ao lint.
  { ignores: ['.next/**', 'next-env.d.ts'] },

  ...reactConfig,
  {
    plugins: {
      '@next/next': nextPlugin,
    },
    rules: {
      ...nextPlugin.configs.recommended.rules,
      ...nextPlugin.configs['core-web-vitals'].rules,
    },
  },
);

export default nextConfig;
