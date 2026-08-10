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
    // A secao 13.9 pedia 'always: lower-case', que exige o assunto inteiro em
    // minusculo. So que a secao 16 especifica quatro titulos com metodo HTTP
    // maiusculo - "expose GET /api/v1/profile...", "expose POST
    // /api/v1/contact..." -, e o hook recusava todos eles. As duas secoes do
    // plano se contradiziam.
    //
    // Esta e a formulacao do @commitlint/config-conventional: em vez de exigir
    // um caso, proibir os que descaracterizam um assunto. Continua barrando
    // "Expose o endpoint" (sentence-case), "Expose O Endpoint" (start-case) e
    // "EXPOSE" (upper-case) - que era o alvo real da regra -, e passa a aceitar
    // sigla e metodo HTTP no meio da frase, que e como se escreve o nome deles.
    'subject-case': [2, 'never', ['sentence-case', 'start-case', 'pascal-case', 'upper-case']],
    'subject-full-stop': [2, 'never', '.'],
    'subject-empty': [2, 'never'],
    'header-max-length': [2, 'always', 72],
    'body-max-line-length': [2, 'always', 100],
  },
};
