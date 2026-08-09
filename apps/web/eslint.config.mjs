import { nextConfig } from '@portfolio/eslint-config/next';
import boundaries from 'eslint-plugin-boundaries';
import tseslint from 'typescript-eslint';

/**
 * As regras de dependencia da secao 5.2 do plano, aplicadas pelo lint.
 *
 * O equivalente frontend do ArchUnit: a arquitetura deixa de ser convencao
 * documentada e passa a ser propriedade verificada do build. As setas apontam
 * sempre para baixo -
 *
 *   app -> feature -> component -> hook -> lib -> type
 *
 * e uma feature nunca importa de outra feature. Se duas precisam do mesmo
 * codigo, ele sobe para components/ ou lib/.
 */

/** Camadas que qualquer elemento acima delas pode consumir. */
const SHARED = ['component', 'hook', 'lib', 'type'];

export default tseslint.config(
  ...nextConfig,

  {
    plugins: { boundaries },

    settings: {
      // Fora de src/ ficam arquivos de configuracao, que nao pertencem a camada
      // nenhuma e nao devem ser avaliados por esta regra.
      'boundaries/include': ['src/**/*'],

      // Elementos sao pastas: o padrao casa com o diretorio, e todo arquivo sob
      // ele pertence aquele elemento.
      'boundaries/elements': [
        { type: 'app', pattern: 'src/app' },
        { type: 'feature', pattern: 'src/features/*', capture: ['featureName'] },
        { type: 'component', pattern: 'src/components' },
        { type: 'hook', pattern: 'src/hooks' },
        { type: 'lib', pattern: 'src/lib' },
        { type: 'i18n', pattern: 'src/i18n' },
        { type: 'style', pattern: 'src/styles' },
        { type: 'type', pattern: 'src/types' },
      ],

      // Sem resolver, o alias @/ nao chega a um arquivo e a dependencia e
      // tratada como externa - a regra silenciaria justamente nos imports que o
      // projeto mais usa.
      'import/resolver': {
        typescript: { project: './tsconfig.json' },
      },
    },

    rules: {
      'boundaries/dependencies': [
        'error',
        {
          default: 'disallow',
          message: '{{from.type}} nao pode importar de {{to.type}} (secao 5.2 do plano).',
          policies: [
            {
              // 'app' se auto-permite: uma rota co-localiza layout, page, error
              // e o proprio globals.css, e importar o irmao nao cruza camada.
              from: { element: { type: 'app' } },
              allow: {
                to: {
                  element: { types: { anyOf: ['app', 'feature', 'i18n', 'style', ...SHARED] } },
                },
              },
            },
            {
              from: { element: { type: 'feature' } },
              allow: { to: { element: { types: { anyOf: SHARED } } } },
            },
            {
              // Uma feature so enxerga a si mesma - nunca uma feature vizinha.
              from: { element: { type: 'feature' } },
              allow: {
                to: {
                  element: {
                    type: 'feature',
                    captured: { featureName: '{{from.captured.featureName}}' },
                  },
                },
              },
            },
            {
              from: { element: { type: 'component' } },
              allow: { to: { element: { types: { anyOf: SHARED } } } },
            },
            {
              from: { element: { type: 'hook' } },
              allow: { to: { element: { types: { anyOf: ['hook', 'lib', 'type'] } } } },
            },
            {
              from: { element: { type: 'lib' } },
              allow: { to: { element: { types: { anyOf: ['lib', 'type'] } } } },
            },
            {
              from: { element: { type: 'i18n' } },
              allow: { to: { element: { types: { anyOf: ['i18n', 'lib', 'type'] } } } },
            },
            {
              from: { element: { type: 'style' } },
              allow: { to: { element: { type: 'style' } } },
            },
            {
              from: { element: { type: 'type' } },
              allow: { to: { element: { type: 'type' } } },
            },
          ],
        },
      ],
    },
  },
);
