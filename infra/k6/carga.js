// Teste de carga da API (secao 2.1 do plano): 100 usuarios virtuais por 2 minutos, contra producao.
//
// Cada usuario pede o que o BFF pede para montar as paginas - perfil, experiencias, habilidades,
// projetos, um projeto e a atividade do GitHub - nos dois idiomas, com um segundo de pausa entre
// um pedido e outro. O formulario de contato fica de fora: tem limite por IP e manda e-mail de
// verdade.
//
// O alvo e a chave vem do ambiente, e a chave nunca entra no repositorio. No CI, o workflow
// "Carga · api" (disparo manual) passa as duas. Para rodar fora dele:
//
//   API_URL=https://... SERVICE_API_KEY=... k6 run infra/k6/carga.js
//
// Os limites sao os da secao 2.1: p95 abaixo de 200 ms e menos de 1% de falhas. Estourar um deles
// faz o k6 sair com erro, mas o resumo e gravado do mesmo jeito - o numero ruim tambem e resultado.
import { check, sleep } from 'k6';
import http from 'k6/http';

const API = __ENV.API_URL;
const CHAVE = __ENV.SERVICE_API_KEY;
if (!API || !CHAVE) {
  throw new Error('defina API_URL e SERVICE_API_KEY');
}

const ROTAS = [
  '/api/v1/profile',
  '/api/v1/experiences',
  '/api/v1/skills',
  '/api/v1/projects',
  '/api/v1/projects/finai',
  '/api/v1/github/stats',
];
const IDIOMAS = ['pt-BR', 'en-US'];

// Um limite por rota, alem do geral, para o resumo trazer o p95 de cada uma: uma rota lenta
// sumiria na media das outras cinco.
const porRota = Object.fromEntries(
  ROTAS.map((rota) => [`http_req_duration{name:${rota}}`, ['p(95)<200']]),
);

export const options = {
  scenarios: {
    carga: { executor: 'constant-vus', vus: 100, duration: '2m' },
  },
  thresholds: {
    http_req_duration: ['p(95)<200'],
    http_req_failed: ['rate<0.01'],
    ...porRota,
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const rota = ROTAS[Math.floor(Math.random() * ROTAS.length)];
  const idioma = IDIOMAS[Math.floor(Math.random() * IDIOMAS.length)];
  const resposta = http.get(`${API}${rota}`, {
    headers: { 'X-Service-Key': CHAVE, 'Accept-Language': idioma },
    tags: { name: rota },
  });
  check(resposta, { 'status 200': (r) => r.status === 200 });
  sleep(1);
}

// O resumo em JSON vai para o artefato, e dele sai o relatorio versionado em docs/reports. Nao
// carrega cabecalho nenhum, entao a chave nao vaza por aqui.
export function handleSummary(dados) {
  const ms = (valor) => `${valor.toFixed(0).padStart(5)} ms`;
  const linhas = [];
  for (const [nome, metrica] of Object.entries(dados.metrics)) {
    if (!nome.startsWith('http_req_duration')) continue;
    const v = metrica.values;
    // Metrica sem limite nao foi julgada, entao nao leva "ok" - na primeira execucao, a
    // {expected_response:true} saiu marcada como aprovada ao lado de seis reprovadas.
    const limites = Object.values(metrica.thresholds ?? {});
    const veredito =
      limites.length === 0 ? '     ' : limites.every((t) => t.ok) ? 'ok   ' : 'ACIMA';
    linhas.push(
      `${veredito} ${nome.padEnd(52)} p95 ${ms(v['p(95)'])}  med ${ms(v.med)}  max ${ms(v.max)}`,
    );
  }
  const pedidos = dados.metrics.http_reqs.values;
  const falhas = dados.metrics.http_req_failed.values.rate;
  linhas.push(
    `pedidos: ${pedidos.count} (${pedidos.rate.toFixed(1)}/s)  falhas: ${(falhas * 100).toFixed(2)}%`,
  );
  return {
    stdout: `${linhas.join('\n')}\n`,
    'k6-resumo.json': JSON.stringify(dados, null, 2),
  };
}
