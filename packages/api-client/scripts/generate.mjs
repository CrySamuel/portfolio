// @ts-check
import { mkdir, writeFile } from 'node:fs/promises';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

import openapiTS, { astToString } from 'openapi-typescript';

/**
 * Gera os tipos TypeScript do contrato da API.
 *
 * Duas operacoes, e a distincao entre elas e o ponto do arquivo:
 *
 *   node scripts/generate.mjs                    tipos a partir do openapi.json versionado
 *   node scripts/generate.mjs --from-api <url>   atualiza o openapi.json antes, lendo a API
 *
 * A primeira roda no build, em qualquer maquina e no CI, sem depender de nada
 * no ar. A segunda e manual e deliberada: mudar o contrato aparece como diff no
 * openapi.json, que e um arquivo revisavel, e nao como um tipo que mudou sozinho
 * entre dois builds.
 *
 * Por que o openapi.json e versionado e os tipos nao: o primeiro e a fonte, tem
 * autoria e merece revisao; os segundos sao derivados dele por uma funcao pura.
 * Versionar os dois convidaria a editar o derivado.
 */

const AQUI = dirname(fileURLToPath(import.meta.url));
const SPEC = new URL('../openapi.json', import.meta.url);
const SAIDA = `${AQUI}/../src/generated/api.ts`;

const CABECALHO = `/**
 * GERADO AUTOMATICAMENTE - NAO EDITAR.
 *
 * Fonte: packages/api-client/openapi.json
 * Comando: pnpm --filter @portfolio/api-client build
 *
 * Este arquivo nao e versionado. Editar aqui e perder a edicao na proxima
 * geracao; o que se edita e o contrato, do lado Java.
 */

`;

async function baixarSpec(baseUrl) {
  const url = `${baseUrl.replace(/\/+$/, '')}/v3/api-docs`;
  const resposta = await fetch(url);

  if (!resposta.ok) {
    throw new Error(`A API respondeu ${String(resposta.status)} em ${url}`);
  }

  // Reserializado com indentacao: o springdoc devolve tudo numa linha so, e um
  // contrato de uma linha so nao tem diff legivel - que e metade da razao de
  // versionar o arquivo.
  const spec = await resposta.json();
  await writeFile(SPEC, `${JSON.stringify(spec, null, 2)}\n`, 'utf8');
  console.log(`openapi.json atualizado a partir de ${url}`);
}

async function main() {
  const argumentos = process.argv.slice(2);
  const indice = argumentos.indexOf('--from-api');

  if (indice !== -1) {
    const baseUrl = argumentos[indice + 1];
    if (!baseUrl) {
      throw new Error('Informe a URL base: --from-api http://localhost:8080');
    }
    await baixarSpec(baseUrl);
  }

  const ast = await openapiTS(SPEC);
  await mkdir(dirname(SAIDA), { recursive: true });
  await writeFile(SAIDA, CABECALHO + astToString(ast), 'utf8');
  console.log('tipos gerados em src/generated/api.ts');
}

await main();
