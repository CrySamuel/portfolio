# Carga na API

**Data:** 18/10/2026 · **Ferramenta:** k6 2.3.0, no workflow **Carga · api** (disparo manual) ·
**Alvo:** a API de produção no Render, plano gratuito, região Ohio, com o Postgres no Neon na mesma
região

## O teste

[`infra/k6/carga.js`](../../infra/k6/carga.js): **100 usuários virtuais por 2 minutos**, como pede a
seção 2.1 do plano. Cada usuário pede, ao acaso, uma das seis leituras que o site faz — perfil,
experiências, habilidades, lista de projetos, um projeto e a atividade do GitHub —, num dos dois
idiomas, com a chave de serviço e um segundo de pausa entre os pedidos. O formulário de contato
fica de fora: tem limite por IP e manda e-mail de verdade.

Limites: p95 abaixo de **200 ms** e menos de 1% de falhas, no geral e em cada rota.

## Resultado

Duas execuções: a primeira com a API como estava, a segunda depois do cache de conteúdo que a
primeira motivou.

|                  | Sem cache (`35616b8`) | Com cache (`e707c9d`) | Mudança    |
| ---------------- | --------------------- | --------------------- | ---------- |
| p95              | 23.568 ms             | **1.400 ms**          | 17× menor  |
| Mediana          | 7.750 ms              | **103 ms**            | 75× menor  |
| p99              | 32.102 ms             | 8.892 ms              | 3,6× menor |
| Vazão            | 10,6 pedidos/s        | **67,2 pedidos/s**    | 6,3× maior |
| Pedidos em 2 min | 1.322                 | 8.139                 |            |
| Falhas           | 0,61%                 | **0,01%**             |            |
| Acerto do cache  | —                     | **98,6%**             |            |

**Os dois reprovam no limite de 200 ms.** A segunda execução passa no de falhas.

Por rota, o p95 e a mediana:

| Rota                     | Sem cache: p95 · mediana | Com cache: p95 · mediana |
| ------------------------ | ------------------------ | ------------------------ |
| `/api/v1/profile`        | 24.695 · 8.004 ms        | 1.390 · 103 ms           |
| `/api/v1/experiences`    | 24.362 · 8.791 ms        | 1.399 · 103 ms           |
| `/api/v1/skills`         | 24.323 · 9.006 ms        | 1.401 · 103 ms           |
| `/api/v1/projects`       | 24.781 · 8.904 ms        | 1.391 · 102 ms           |
| `/api/v1/projects/finai` | 25.671 · 9.203 ms        | 1.593 · 103 ms           |
| `/api/v1/github/stats`   | 14.142 · 395 ms          | 1.302 · 103 ms           |

Os resumos completos do k6 estão em [`dados/`](dados).

## Leitura

**Na primeira execução, o gargalo era o caminho do banco.** A única rota que já tinha cache — a do
GitHub — tinha mediana de 395 ms; as cinco que iam ao banco, de 8 a 9 segundos. Não era N+1: medido
localmente, cada rota faz de 2 a 4 consultas. Era cada leitura disputando um pool de **cinco
conexões** ao Neon (dimensionado assim para o banco poder dormir e para caber na cota de conexões
do plano gratuito) numa instância de CPU mínima. Os pedidos faziam fila pela conexão, e os 0,61% de
falhas são respostas de erro da própria API depois de pouco mais de 30 segundos — o tempo que o
pool espera por uma conexão antes de desistir.

**O cache tirou o banco do caminho.** A seção 10.5 do plano previa cache de 10 minutos no conteúdo,
e a primeira versão da API o deixou de fora de propósito: o visitante já recebe o HTML da CDN. A
medição mostrou o custo, e o cache entrou — uma região Caffeine por leitura, com o idioma na chave.
Invalidar não é problema aqui: o conteúdo só muda por migração, na subida da aplicação, que começa
com o cache vazio. As métricas do `/actuator/prometheus` depois do teste mostram 98,6% de acerto:
os 18 a 22 erros por região são da partida, quando os 100 usuários chegam juntos ao cache vazio.

**O que sobra é a CPU.** Com o cache, a rota do GitHub — que estava em cache também na primeira
execução — continua com p95 de 1,3 s, igual às outras: o tempo agora não depende do que a rota faz,
e sim de quantos pedidos disputam a mesma fração de processador. A mediana de 103 ms é o custo de um
pedido sem fila; o p95 é a fila. Passar dos 200 ms pede uma instância com mais CPU, que é plano
pago, e o portfólio não precisa disso, como explica a seção seguinte.

## Por que o visitante não sente nada disso

O teste mede a API sozinha, com 100 clientes simultâneos. Em produção, a API tem **um** cliente: o
servidor do Next, que a chama no build e na revalidação das páginas — algumas vezes por hora. O
visitante recebe o HTML pré-renderizado da CDN da Vercel e nunca espera pela API, como mostra o
[relatório do Lighthouse](lighthouse.md). O cenário que motiva o teste no plano — 10.000 visitas em
uma hora depois de um post no LinkedIn — termina inteiro na CDN.

O teste continua valendo pelo que mostrou: um caminho sem cache que só um teste de carga revelaria,
e o número de quanto a instância gratuita aguenta antes de enfileirar.

## Como reproduzir

Pelo GitHub: **Actions → Carga · api → Run workflow**. Precisa da variável `API_BASE_URL` e do
segredo `SERVICE_API_KEY` cadastrados no repositório.

Fora dele, com o k6 instalado:

```bash
API_URL=https://... SERVICE_API_KEY=... k6 run infra/k6/carga.js
```
