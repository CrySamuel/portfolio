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

/**
 * Camadas que qualquer elemento acima delas pode consumir.
 *
 * `i18n` entrou no commit 51: features e componentes precisam do endereco de uma rota no idioma da
 * pagina - o link de cada card de projeto -, e o roteamento mora la. A camada continua de baixo: ela
 * so importa de `lib` e `type`, entao nenhum ciclo e possivel.
 */
const SHARED = ['component', 'hook', 'i18n', 'lib', 'type'];

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

      // O next-intl e so de servidor neste site, e duas portas de entrada dele
      // levam codigo para o navegador sem nenhum uso na tela. O pacote raiz
      // reexporta o NextIntlClientProvider, e o createNavigation cria o Link da
      // biblioteca: ambos sao componentes de cliente, e importar o modulo basta
      // para o build registra-los - com o nucleo do use-intl junto. Medido no
      // commit 51: +1,0 KB comprimido em toda pagina pelo pacote raiz e +0,3 KB
      // pelo createNavigation, de um orcamento com 8 KB de folga.
      'no-restricted-imports': [
        'error',
        {
          paths: [
            {
              // So o import de runtime: o de tipo e apagado no build, e i18n/routing
              // precisa de um - ver LocaleDoNextIntl.
              name: 'next-intl',
              allowTypeImports: true,
              message:
                'Leva o NextIntlClientProvider para o bundle. Use next-intl/server (getTranslations, getLocale) e ehIdioma de @/i18n/routing.',
            },
            {
              name: 'next-intl/navigation',
              message:
                'O createNavigation leva o Link do next-intl para o bundle. Use caminhoPublico de @/i18n/navigation.',
            },
          ],
        },
      ],
    },
  },

  {
    // Testes de unidade: rodam no Node e nunca chegam ao navegador.
    files: ['src/**/*.test.ts'],
    rules: {
      // O pacote raiz do next-intl e proibido pelo bundle, que teste nao tem - e e o
      // createTranslator dele que deixa conferir a frase traduzida de verdade, plural inclusive.
      'no-restricted-imports': 'off',
      // expect(api.metodo) e vi.mocked(api.metodo) so guardam a referencia do duble; ninguem a
      // chama fora do objeto, que e o risco que a regra existe para pegar.
      '@typescript-eslint/unbound-method': 'off',
    },
  },
);
