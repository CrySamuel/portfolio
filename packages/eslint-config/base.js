import js from '@eslint/js';
import prettier from 'eslint-config-prettier';
import tseslint from 'typescript-eslint';

/**
 * Configuracao base de TypeScript, compartilhada por todos os pacotes.
 *
 * Usa strictTypeChecked (e nao apenas recommended) porque as regras que
 * dependem de informacao de tipo sao as que pegam os erros que importam:
 * promessa nao aguardada, comparacao sempre verdadeira, acesso a membro de
 * valor possivelmente nulo. Sao justamente os bugs que chegam em producao.
 *
 * @type {import('typescript-eslint').ConfigArray}
 */
export const baseConfig = tseslint.config(
  {
    ignores: ['**/dist/**', '**/.next/**', '**/coverage/**', '**/node_modules/**', '**/target/**'],
  },

  js.configs.recommended,
  ...tseslint.configs.strictTypeChecked,
  ...tseslint.configs.stylisticTypeChecked,

  {
    languageOptions: {
      parserOptions: {
        projectService: true,
      },
    },
    rules: {
      // any anula a razao de existir do TypeScript. Use unknown e estreite.
      '@typescript-eslint/no-explicit-any': 'error',

      // Import de tipo explicito: com verbatimModuleSyntax ligado, o compilador
      // nao remove imports por conta propria - o que nao for marcado como tipo
      // vira import de runtime e entra no bundle.
      '@typescript-eslint/consistent-type-imports': [
        'error',
        { prefer: 'type-imports', fixStyle: 'inline-type-imports' },
      ],
      '@typescript-eslint/no-import-type-side-effects': 'error',

      // Assercao de tipo exige justificativa escrita (secao 13.5 do plano).
      '@typescript-eslint/no-unnecessary-type-assertion': 'error',

      // Variavel nao usada e erro, exceto quando prefixada com _ - o sublinhado
      // e a forma de declarar "descartei isto de proposito".
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
          caughtErrorsIgnorePattern: '^_',
        },
      ],

      // console.log em producao e ruido; warn e error sao sinal.
      'no-console': ['error', { allow: ['warn', 'error'] }],

      eqeqeq: ['error', 'always', { null: 'ignore' }],
      'prefer-const': 'error',
      'no-var': 'error',
      'object-shorthand': 'error',
    },
  },

  // Arquivos de configuracao em JavaScript ficam fora do programa TypeScript,
  // entao as regras que exigem informacao de tipo nao tem como rodar neles.
  {
    files: ['**/*.{js,mjs,cjs}'],
    extends: [tseslint.configs.disableTypeChecked],
    languageOptions: {
      parserOptions: {
        projectService: false,
      },
    },
  },

  // Precisa ser o ultimo: desliga as regras estilisticas que o Prettier resolve.
  prettier,
);

export default baseConfig;
