/**
 * Formatacao e lint apenas dos arquivos staged.
 *
 * Nota sobre o formato: o plano previa .lintstagedrc.json, mas o caso do Java
 * exige uma funcao. O lint-staged anexa a lista de arquivos ao final de cada
 * comando, e o Maven interpretaria esses caminhos como goals - `./mvnw
 * spotless:apply Foo.java` quebra. Uma funcao que devolve a string ignora a
 * lista, que e exatamente o que se precisa aqui.
 *
 * @type {import('lint-staged').Configuration}
 */
export default {
  // O ESLint roda por pacote, e nao uma vez na raiz. A config flat da versao 9
  // e resolvida a partir do cwd, entao rodar da raiz usaria uma config que nao
  // existe - e as regras de fronteira do apps/web seriam silenciosamente
  // ignoradas. Pacote novo com lint proprio precisa de uma linha aqui.
  'apps/web/**/*.{ts,tsx}': ['pnpm --filter @portfolio/web exec eslint --fix'],
  'packages/ui/**/*.{ts,tsx}': ['pnpm --filter @portfolio/ui exec eslint --fix'],

  // Formatacao vale para tudo, inclusive os arquivos ja cobertos acima. O
  // ESLint nao formata (eslint-config-prettier desliga essas regras), entao os
  // dois nao competem - mas precisam rodar em sequencia, nao em paralelo. Dai o
  // --concurrent false no hook de pre-commit.
  '*.{ts,tsx,js,mjs,cjs,json,md,css,yml,yaml}': ['prettier --write'],

  // O wrapper e chamado pelo caminho, com -f apontando para o pom: apps/api nao
  // e um pacote do workspace pnpm (nao tem package.json), e o lint-staged nao
  // executa os comandos atraves de um shell, entao nem `pnpm --dir` nem
  // `cd apps/api && ...` funcionariam aqui.
  //
  // Spotless formata o modulo inteiro. O lint-staged reestagia os arquivos que
  // casaram com o padrao, entao as correcoes entram no commit.
  'apps/api/**/*.java': () => 'apps/api/mvnw -q -B -f apps/api/pom.xml spotless:apply',
};
