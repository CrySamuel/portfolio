/**
 * Conventional Commits, conforme a secao 13.9 do plano.
 *
 * O historico deste repositorio e parte do entregavel: um avaliador tecnico le
 * `git log` antes de ler o codigo. Validar a mensagem no hook - e nao na
 * revisao - e o que garante que a regra sobreviva ao cansaco de quem commita.
 *
 * @type {import('@commitlint/types').UserConfig}
 */
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [
      2,
      'always',
      [
        'feat', // nova funcionalidade                 -> minor
        'fix', // correcao de bug                      -> patch
        'refactor', // mudanca sem alterar comportamento
        'perf', // melhoria de performance             -> patch
        'test', // testes
        'docs', // documentacao
        'style', // formatacao
        'build', // build ou dependencias
        'ci', // pipeline
        'chore', // manutencao
        // 'revert' nao consta na tabela da secao 13.9, mas e o tipo que o
        // proprio git gera em `git revert`. Sem ele, reverter um commit seria
        // impossivel sem burlar o hook.
        'revert',
      ],
    ],
    'scope-enum': [2, 'always', ['web', 'api', 'ui', 'db', 'infra', 'docs', 'deps']],
    // Escopo e opcional: mudancas na raiz do monorepo nao pertencem a nenhum.
    'scope-empty': [0],
    'subject-case': [2, 'always', 'lower-case'],
    'subject-full-stop': [2, 'never', '.'],
    'subject-empty': [2, 'never'],
    'header-max-length': [2, 'always', 72],
    'body-max-line-length': [2, 'always', 100],
  },
};
