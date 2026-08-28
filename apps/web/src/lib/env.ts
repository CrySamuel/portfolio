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
/**
 * O mesmo piso que a API exige no boot (`SecurityConfig`).
 *
 * Repetido nas duas pontas de propósito: se só a API validasse, uma chave curta
 * subiria o web normalmente e o erro apareceria como 401 em produção, longe da
 * causa. Validando aqui também, os dois lados recusam pelo mesmo motivo e na
 * mesma hora.
 */
const TAMANHO_MINIMO_DA_CHAVE = 32;

const schema = z.object({
  API_URL: z.url('precisa ser uma URL absoluta (ex.: http://localhost:8080)'),

  // Nunca com prefixo NEXT_PUBLIC_: esta chave é o que separa a API pública do
  // resto da internet (ADR-0005). Ela sai daqui para o cabeçalho X-Service-Key,
  // sempre do servidor - o navegador jamais a vê.
  SERVICE_API_KEY: z
    // A mensagem de tipo precisa ser explícita: variável ausente chega aqui como
    // `undefined` e falha antes do `.min()`, então o texto padrão do Zod
    // ("Invalid input") seria o que apareceria no log do deploy - justamente no
    // caso mais provável, que é alguém esquecer de cadastrá-la no painel.
    .string({ error: 'é obrigatória — gere com: openssl rand -base64 32' })
    .min(
      TAMANHO_MINIMO_DA_CHAVE,
      `precisa ter ao menos ${String(TAMANHO_MINIMO_DA_CHAVE)} caracteres (openssl rand -base64 32)`,
    ),

  /**
   * A Site Key do Turnstile — **pública por construção**.
   *
   * O prefixo `NEXT_PUBLIC_` está certo aqui e é o inverso exato do caso acima:
   * a chave precisa chegar ao navegador, porque é ela que o widget imprime no
   * HTML. Marcá-la como segredo não a protegeria de nada e apenas impediria o
   * widget de existir.
   *
   * Ela é lida **aqui**, num módulo `server-only`, e desce até o formulário como
   * prop. A alternativa — o Client Component ler `process.env.NEXT_PUBLIC_…`
   * direto — funcionaria, e é justamente por funcionar sem validação nenhuma que
   * não foi escolhida: uma variável esquecida no painel viraria `undefined`, o
   * widget não renderizaria e o formulário passaria a recusar toda mensagem
   * enviada, em produção, sem nada no log dizendo por quê.
   */
  NEXT_PUBLIC_TURNSTILE_SITE_KEY: z
    .string({ error: 'é obrigatória — sai do painel do Turnstile, junto da Secret Key' })
    .min(1, 'não pode ser vazia'),

  /**
   * A Secret Key do Turnstile — **nunca** com prefixo `NEXT_PUBLIC_`.
   *
   * É com ela que o servidor pergunta à Cloudflare se um token vale. Publicá-la
   * no bundle permitiria a qualquer um forjar a resposta da verificação, o que
   * é o mesmo que não ter Turnstile — com o custo de parecer que tem.
   *
   * Não há mínimo de tamanho como na `SERVICE_API_KEY`: aquele valor é gerado
   * por nós e o piso é uma escolha nossa; este é emitido pela Cloudflare, e
   * inventar um formato esperado aqui reprovaria o dia em que eles mudassem o
   * deles.
   */
  TURNSTILE_SECRET_KEY: z
    .string({ error: 'é obrigatória — sai do painel do Turnstile e não pode passar por conversa' })
    .min(1, 'não pode ser vazia'),
});

const resultado = schema.safeParse(process.env);

if (!resultado.success) {
  const problemas = resultado.error.issues
    .map((issue) => `  ${issue.path.join('.')}: ${issue.message}`)
    .join('\n');

  throw new Error(`Ambiente invalido:\n${problemas}\n\nAs variaveis estao em .env.example.`);
}

export const env = resultado.data;
