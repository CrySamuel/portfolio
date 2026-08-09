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
  '*.{ts,tsx}': ['eslint --fix', 'prettier --write'],
  '*.{js,mjs,cjs,json,md,css,yml,yaml}': ['prettier --write'],

  // O wrapper e chamado pelo caminho, com -f apontando para o pom: apps/api nao
  // e um pacote do workspace pnpm (nao tem package.json), e o lint-staged nao
  // executa os comandos atraves de um shell, entao nem `pnpm --dir` nem
  // `cd apps/api && ...` funcionariam aqui.
  //
  // Spotless formata o modulo inteiro. O lint-staged reestagia os arquivos que
  // casaram com o padrao, entao as correcoes entram no commit.
  'apps/api/**/*.java': () => 'apps/api/mvnw -q -B -f apps/api/pom.xml spotless:apply',
};
