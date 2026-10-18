# Lighthouse

**Data:** 18/10/2026 · **Versão do site:** `911ce51` em produção, `7288d04` no CI ·
**Ferramenta:** Lighthouse 13.5, emulação de celular e limitação de rede simulada (os padrões)

São duas medições com papéis diferentes. A de **produção** responde ao critério do plano
("≥ 95 nas 4 categorias, celular, medido em produção"). A do **CI** roda a cada mudança em
`apps/web` e reprova o workflow quando passa de um limite da seção 10.1 do plano.

## Produção

`npx lighthouse@13.5.0 <url> --chrome-flags="--headless=new"`, a partir de um computador no Brasil,
contra a Vercel. As homes rodaram duas vezes, porque são as que mais variam.

| Página            | Performance | LCP         | TBT         | CLS | Peso transferido | Pedidos | Terceiros |
| ----------------- | ----------- | ----------- | ----------- | --- | ---------------- | ------- | --------- |
| `/`               | 97 · 99     | 2,0 · 1,8 s | 110 · 60 ms | 0   | 180 KB           | 15      | 0         |
| `/en-US`          | 100 · 100   | 1,7 · 1,7 s | 30 · 50 ms  | 0   | 180 KB           | 15      | 0         |
| `/projetos`       | 100         | 1,7 s       | 30 ms       | 0   | 197 KB           | 20      | 0         |
| `/projetos/finai` | 99          | 2,0 s       | 80 ms       | 0   | 231 KB           | 20      | 0         |

**Acessibilidade, boas práticas e SEO: 100 nas quatro páginas.**

### Antes e depois do Turnstile sob demanda

A home era a única página abaixo de 99. O widget do Turnstile (o antispam do formulário de
contato, que fica no fim da página) carregava na abertura e trazia **740 a 830 KB** da
Cloudflare, conforme a execução, em 13 pedidos — o maior deles, um desafio de ~590 KB. Agora ele só carrega quando
o formulário chega a uma tela de distância ou recebe foco.

| Home (`/`)                   | Antes                           | Depois |
| ---------------------------- | ------------------------------- | ------ |
| Performance                  | 95–97 (88 numa execução a frio) | 97–99  |
| Peso transferido             | mais de 900 KB                  | 180 KB |
| Pedidos a terceiros na carga | 13, da Cloudflare               | 0      |

## CI

`lhci autorun` no workflow **E2E · web**, contra o `next start` do build, com a API de verdade
atrás. Três execuções por página; vale a mediana. Os limites estão em
[`apps/web/lighthouserc.json`](../../apps/web/lighthouserc.json).

| Página            | Performance | LCP               | TBT    | CLS | TTFB   | Peso   | Pedidos | Fontes          |
| ----------------- | ----------- | ----------------- | ------ | --- | ------ | ------ | ------- | --------------- |
| `/`               | 99          | 1,96 s            | 14 ms  | 0   | 6 ms   | 191 KB | 15      | 30 KB           |
| `/projetos`       | 99          | 2,04 s            | 27 ms  | 0   | 4 ms   | 208 KB | 20      | 30 KB           |
| `/projetos/finai` | 98          | **2,34 s** de 2,5 | 30 ms  | 0   | 3 ms   | 241 KB | 20      | **70 KB** de 80 |
| `/en-US`          | 99          | 2,19 s            | 31 ms  | 0   | 4 ms   | 190 KB | 15      | 30 KB           |
| **Limite**        | aviso < 95  | 2,5 s             | 300 ms | 0,1 | 800 ms | 600 KB | 30      | 80 KB           |

**Onde a folga é curta:** a página de projeto carrega duas fontes (a do texto e a monoespaçada dos
trechos de código) e está a 10 KB do limite de fontes; o LCP dela é o mais alto. Uma terceira fonte
ou um peso a mais estoura o CI — que é o ponto do limite.

O TTFB do CI é de milissegundos porque o servidor roda na mesma máquina; em produção ele vem da CDN.

## O que ficou sem ação, e por quê

| Achado do Lighthouse                       | Motivo                                                                                                                                                                                                                                                        |
| ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| "JavaScript legado", ~12 KB                | Polyfills que o próprio Next 15.5 embute, sem configuração para desligar                                                                                                                                                                                      |
| "JavaScript não usado", ~20 KB             | Um chunk do framework                                                                                                                                                                                                                                         |
| CSS que bloqueia a renderização, 40–240 ms | Só sai com o CSS inline, que colide com a CSP travada por hash ([ADR-0012](../PLANO-COMPLETO.md#adr-0012-csp-estática-sem-nonce-emenda-a-seção-24))                                                                                                           |
| Cache de voltar/avançar recusado nas homes | O iframe do Turnstile segura um `MediaStreamTrack`. Só acontece depois de o widget carregar, e o widget não está nos pedidos da carga — no teste ele entra depois, provavelmente na captura da página inteira, que estica a janela. É do iframe da Cloudflare |

## Metas do plano que estes números não cumprem

- **LCP abaixo de 2,0 s** (Definition of Done do MVP 6): em produção ele fica entre 1,7 e 2,0 s, no
  limite. São números de laboratório; o site não tem tráfego suficiente para o relatório de campo
  do Chrome (CrUX), que é o que o critério pede de verdade.
- **JavaScript inicial abaixo de 100 KB:** fora de alcance. Uma página vazia, só com React 19 e
  Next 15, já mede cerca de 100 KB comprimidos. O CI trava em 130 KB, e as páginas estão entre 118
  e 123 KB — medidas por [`scripts/orcamento-js.mjs`](../../apps/web/scripts/orcamento-js.mjs),
  que soma o gzip dos scripts de cada página no build.
