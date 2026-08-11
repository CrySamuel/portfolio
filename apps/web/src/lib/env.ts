import 'server-only';

import { z } from 'zod';

/**
 * As variaveis de ambiente do servidor, validadas na primeira importacao.
 *
 * A secao 12.4 do plano quer que variavel ausente derrube a aplicacao **na
 * inicializacao**, e nao em producao as 3h da manha. E a diferenca entre um erro
 * que aparece no deploy, com a mensagem dizendo qual variavel falta, e um
 * `undefined` que atravessa tres camadas e vira `fetch failed` sem contexto.
 *
 * O `server-only` e a segunda metade da protecao. Este modulo passa a ser
 * proibido em Client Component: importa-lo de um deixa de compilar, em vez de
 * embutir o valor no bundle que vai para o navegador. Hoje aqui so ha uma URL
 * interna; a chave de servico entra em seguida, e ai o erro seria vazamento de
 * segredo, nao configuracao errada.
 */
const schema = z.object({
  API_URL: z.url('precisa ser uma URL absoluta (ex.: http://localhost:8080)'),
});

const resultado = schema.safeParse(process.env);

if (!resultado.success) {
  const problemas = resultado.error.issues
    .map((issue) => `  ${issue.path.join('.')}: ${issue.message}`)
    .join('\n');

  throw new Error(`Ambiente invalido:\n${problemas}\n\nAs variaveis estao em .env.example.`);
}

export const env = resultado.data;
