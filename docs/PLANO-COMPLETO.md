# Plano de Projeto — Portfólio Backend

**Status:** `PROPOSTO` — aguardando aprovação
**Versão:** 2.0 (consolidada, organizada por MVPs)
**Data:** 05/08/2026
**Autor:** Staff Engineer / Software Architect / Tech Lead
**Stakeholder:** Crystofer Demetino

---

## Sumário executivo

Este documento descreve o planejamento completo de um portfólio profissional que **não apenas apresenta** competências de Desenvolvedor Backend — ele **é, ele próprio, a demonstração dessas competências**.

A decisão estrutural central: o portfólio é um **monorepo full-stack real**, com um site em Next.js consumindo uma **API própria em Java 21 + Spring Boot**, executando em produção com banco de dados, cache, resiliência, observabilidade, testes de integração com Testcontainers e pipeline de CI/CD.

Um tech lead que abrir o repositório encontra: arquitetura hexagonal, ADRs, testes com cobertura real, migrações versionadas, OpenAPI, Docker multi-stage, GitHub Actions e um histórico de commits que parece o de um time. Um recrutador não-técnico abre o site e vê algo bonito e rápido. **Os dois públicos são atendidos pelo mesmo artefato.**

### Como este documento está organizado

| Parte | Conteúdo | Quando usar |
|-------|----------|-------------|
| **Parte I — Planejamento** (seções 1–14) | Visão, arquitetura, stack, design system, padrões | Ler antes de começar. É a referência de "o quê" e "por quê". |
| **Parte II — Execução** (seções 15–17) | 7 MVPs, com os 54 commits distribuídos dentro deles | Consultar durante o desenvolvimento. É o "como" e o "quando". |

### Índice

**Parte I — Planejamento**

1. [Visão Geral do Projeto](#1-visão-geral-do-projeto)
2. [Objetivos Técnicos](#2-objetivos-técnicos-atributos-de-qualidade)
3. [Arquitetura Geral](#3-arquitetura-geral)
4. [Escolha da Stack](#4-escolha-da-stack)
5. [Estrutura de Pastas](#5-estrutura-de-pastas)
6. [Funcionalidades](#6-funcionalidades)
7. [Design System](#7-design-system)
8. [Componentização](#8-componentização)
9. [Integrações](#9-integrações)
10. [Estratégia de Performance](#10-estratégia-de-performance)
11. [Estratégia de SEO](#11-estratégia-de-seo)
12. [Estratégia de Acessibilidade](#12-estratégia-de-acessibilidade)
13. [Padrões de Código](#13-padrões-de-código)
14. [Decisões Arquiteturais (ADRs)](#14-decisões-arquiteturais-adrs)

**Parte II — Execução**

15. [Estratégia de MVPs](#15-estratégia-de-mvps)
16. [Os 7 MVPs e seus 54 commits](#16-os-7-mvps-e-seus-54-commits)
    - [MVP 0 — Fundação técnica](#mvp-0--fundação-técnica-commits-0109)
    - [MVP 1 — Portfólio publicado](#mvp-1--portfólio-publicado-commits-1023)
    - [MVP 2 — Perfil completo](#mvp-2--perfil-completo-commits-2432)
    - [MVP 3 — Prova de trabalho](#mvp-3--prova-de-trabalho-commits-3339)
    - [MVP 4 — Prova de engenharia backend](#mvp-4--prova-de-engenharia-backend-commits-4044)
    - [MVP 5 — Conversão](#mvp-5--conversão-commits-4549)
    - [MVP 6 — Alcance e qualidade](#mvp-6--alcance-e-qualidade-commits-5054)
17. [Riscos, checklists e aprovação](#17-riscos-checklists-e-aprovação)

---

## Decisões travadas nesta versão

| Decisão | Escolha | ADR |
|---------|---------|-----|
| Escopo | Monorepo full-stack com API real em produção | [ADR-0001](#adr-0001-monorepo-full-stack-com-api-real) |
| Backend | Java 21 + Spring Boot 4.1 | [ADR-0002](#adr-0002-java-21-e-spring-boot-34) → [ADR-0009](#adr-0009-spring-boot-35-substitui-o-adr-0002) → [ADR-0011](#adr-0011-spring-boot-41-substitui-o-adr-0009) |
| Estilo arquitetural | Hexagonal (Ports & Adapters) + monólito modular | [ADR-0003](#adr-0003-arquitetura-hexagonal-com-monólito-modular) |
| Fonte de verdade do conteúdo | PostgreSQL + Flyway | [ADR-0004](#adr-0004-postgresql-como-fonte-de-verdade-do-conteúdo) |
| Comunicação web ↔ api | BFF via Route Handlers do Next.js | [ADR-0005](#adr-0005-bff-em-route-handlers-do-nextjs) |
| Hospedagem | Vercel (web) + Render (api) + Neon (Postgres) | [ADR-0006](#adr-0006-hospedagem-em-vercel-e-render), emendado pelo [ADR-0010](#adr-0010-postgres-no-neon-emenda-o-adr-0006) |
| Renderização | SSG + ISR com revalidação por tag | [ADR-0007](#adr-0007-ssg-com-isr-e-revalidação-por-tag) |
| Resiliência externa | Resilience4j: circuit breaker + retry + fallback | [ADR-0008](#adr-0008-resiliência-na-integração-com-o-github) |
| Cabeçalhos de segurança | CSP estática, sem nonce, para preservar o ISR | [ADR-0012](#adr-0012-csp-estática-sem-nonce-emenda-a-seção-24) |

---
---

# PARTE I — PLANEJAMENTO

---

## 1. Visão Geral do Projeto

### 1.1 Objetivo

Construir um portfólio profissional que funcione como **evidência verificável** de competência em engenharia de software backend, e não como uma vitrine declarativa.

A distinção importa. A maioria dos portfólios de backend é um site estático que *afirma* "sei Spring Boot, sei Docker, sei testes". Este projeto adota a premissa oposta: **o portfólio é o próprio sistema backend**. As informações exibidas na tela vêm de uma API Java real, com banco de dados real, cache real, tratamento de falha real e pipeline de deploy real.

O objetivo secundário — igualmente importante — é que o **repositório seja legível**. Um tech lead avaliando um candidato passa entre 5 e 15 minutos no código. O projeto precisa comunicar qualidade nesse intervalo: README claro, ADRs, estrutura previsível, testes visíveis.

### 1.2 Público-alvo

| Persona | Contexto | O que procura | Como o projeto atende |
|---------|----------|---------------|----------------------|
| **Recrutador técnico** | Triagem inicial, 60–90 segundos no site | Stack, senioridade aparente, experiência, contato fácil | Hero com proposta de valor direta, skills escaneáveis, CTA de contato e download de CV sempre visíveis |
| **Tech Lead / Engineering Manager** | Avalia o repositório antes ou depois da entrevista | Qualidade de código, decisões arquiteturais, testes, histórico de commits | Monorepo organizado, ADRs, ArchUnit, cobertura, CI verde, commits atômicos |
| **Desenvolvedor par (entrevista técnica)** | Vai discutir o projeto ao vivo | Profundidade das escolhas e trade-offs conscientes | Cada decisão documentada em ADR, com alternativas descartadas e consequências |
| **Cliente / freelance** | Busca alguém para um serviço | Credibilidade, projetos entregues, facilidade de contato | Cases com problema → solução → resultado, formulário funcional |
| **O próprio autor** | Manutenção ao longo dos anos | Baixo custo de atualização | Conteúdo em banco com migrações versionadas: adicionar um projeto é uma migration, não um deploy de código |

### 1.3 Problema que o projeto resolve

**Problema de negócio.** Vagas de backend recebem centenas de candidaturas. O currículo em PDF é um formato de baixa largura de banda: não mostra como a pessoa pensa, apenas o que ela alega ter feito. O gargalo do candidato não é a falta de habilidade — é a **incapacidade de demonstrá-la antes da entrevista técnica**.

**Problema técnico.** Perfis do GitHub são ruidosos: forks, exercícios de curso, repositórios abandonados e projetos bons ficam misturados sem hierarquia. Não há narrativa. Um avaliador não consegue distinguir, em 5 minutos, o que é relevante.

**Solução proposta.** Um único ponto de entrada que:

1. apresenta uma narrativa curada (quem sou, o que sei, o que entreguei);
2. prova competência backend pela própria existência do sistema;
3. reduz o atrito do contato a um clique;
4. se mantém atualizado sozinho, puxando dados do GitHub via API.

### 1.4 Diferenciais

| # | Diferencial | Por que é diferencial |
|---|-------------|----------------------|
| 1 | **Backend real em produção**, não mockado | A maioria dos portfólios de "dev backend" não tem backend nenhum. Este tem API Java com Postgres, cache, rate limit e observabilidade. |
| 2 | **Arquitetura hexagonal com ArchUnit** | As regras de dependência não são uma promessa no README — são testes que quebram o build se violadas. |
| 3 | **Testes de integração com Testcontainers** | Prova domínio de testes que sobem infraestrutura real, e não apenas mocks de repositório. |
| 4 | **ADRs versionados** | Demonstra pensamento arquitetural explícito e maturidade de comunicação técnica — raro em portfólios júnior/pleno. |
| 5 | **Histórico de commits profissional** | 54 commits atômicos em Conventional Commits contam a história do projeto. Avaliadores olham o histórico. |
| 6 | **Resiliência visível na integração com o GitHub** | Circuit breaker, retry com backoff, timeout e fallback em cache. Trata a API do GitHub como dependência instável — porque é. |
| 7 | **Performance e acessibilidade como requisitos, não enfeite** | Orçamento de performance e testes de a11y no CI. Mostra rigor mesmo fora da especialidade declarada. |
| 8 | **Contrato de API tipado ponta a ponta** | O cliente TypeScript é gerado do OpenAPI. Se o backend muda, o frontend quebra no build — não em produção. |

### 1.5 Escopo

#### Dentro do escopo (v1.0)

**Site (`apps/web`)**

- Home com hero, destaque de skills, projetos em evidência e CTA
- Sobre, com bio e timeline profissional
- Skills agrupadas por categoria e nível de proficiência
- Listagem de projetos com filtro por tecnologia
- Detalhe de projeto (`/projetos/[slug]`) com narrativa problema → solução → resultado
- Estatísticas do GitHub (linguagens, contribuições, repositórios em destaque)
- Formulário de contato funcional com proteção antispam
- Download de currículo em PDF
- Tema claro/escuro respeitando a preferência do sistema
- Suporte a pt-BR e en-US (i18n)

**API (`apps/api`)**

- `GET /api/v1/profile` — bio, headline, links sociais
- `GET /api/v1/skills` — skills agrupadas por categoria
- `GET /api/v1/experiences` — timeline profissional
- `GET /api/v1/projects` e `GET /api/v1/projects/{slug}` — catálogo de projetos
- `GET /api/v1/github/stats` — estatísticas agregadas e cacheadas do GitHub
- `POST /api/v1/contact` — recebimento de mensagem com rate limit e envio de e-mail
- `/actuator/health`, `/actuator/info`, `/actuator/prometheus`
- `/swagger-ui` e `/v3/api-docs` — documentação OpenAPI

**Engenharia**

- Monorepo com Turborepo e pnpm
- Migrações de banco com Flyway
- Testes unitários, de fatia (slice) e de integração
- Docker multi-stage e Docker Compose para ambiente local
- CI/CD com GitHub Actions
- Análise de segurança (CodeQL, Trivy, Dependabot)

#### Fora do escopo (v1.0)

| Item | Motivo | Possível versão |
|------|--------|-----------------|
| Painel administrativo com login | Conteúdo muda raramente; uma migration resolve. Autenticação adicionaria superfície de ataque sem ganho proporcional. | v2.0 |
| Blog com editor | Escopo grande o suficiente para ser um produto próprio. | v2.0 |
| Microsserviços | Complexidade injustificada para o volume de tráfego. Modularização interna já demonstra o raciocínio. | Nunca — seria anti-pattern aqui |
| Kubernetes | Custo e complexidade operacional desproporcionais. Docker + PaaS é a escolha correta e defensável. | Nunca |
| Comentários / área de membros | Não serve ao objetivo do portfólio. | — |
| Teste de carga automatizado no CI | Um relatório k6 pontual, versionado, é suficiente. | v1.1 |

#### Restrições

- **Orçamento:** próximo de zero. Toda a stack deve caber em free tiers (Vercel Hobby, Render Free, Postgres gerenciado free, Resend Free).
- **Equipe:** uma pessoa. O roadmap assume ritmo de projeto paralelo, não dedicação integral.
- **Cold start:** o free tier do Render hiberna após inatividade. Mitigação no [ADR-0006](#adr-0006-hospedagem-em-vercel-e-render) — o site é estático/ISR, então o cold start **nunca afeta o visitante**, apenas a revalidação em background.

---

## 2. Objetivos Técnicos (Atributos de Qualidade)

Cada atributo abaixo é declarado como um **cenário de qualidade mensurável**, no formato usado em arquitetura de software (estímulo → resposta → medida). Objetivo vago não é objetivo — é intenção.

### 2.1 Escalabilidade

**Cenário:** o portfólio é compartilhado no LinkedIn e recebe 10.000 visitas em uma hora.
**Resposta esperada:** o site continua respondendo com TTFB < 1s, sem custo adicional relevante.

**Como:**

- Páginas geradas estaticamente (SSG) e servidas pela CDN global da Vercel. O pico de tráfego **não toca a API**.
- ISR com revalidação por tag: a página é regenerada em background, não sob demanda do usuário.
- A API é *stateless* — escala horizontalmente sem sessão pegajosa.
- Cache em duas camadas na API (Caffeine em memória; Redis opcional na v1.1) protege o Postgres e a API do GitHub.
- Pool de conexões HikariCP dimensionado explicitamente, não no default.

**Medição:** teste de carga com k6 (100 VUs por 2 min) contra produção, relatório versionado em `docs/reports/`.

### 2.2 Manutenibilidade

**Cenário:** seis meses depois, o autor precisa adicionar um novo projeto ao portfólio.
**Resposta esperada:** a alteração leva menos de 15 minutos e não exige tocar em código de aplicação.

**Como:**

- Conteúdo em banco com migração Flyway versionada. Adicionar projeto = escrever um `INSERT` em uma migration.
- Arquitetura hexagonal: trocar Postgres por outro banco afeta apenas `adapter/out/persistence`.
- Módulos com fronteiras explícitas, verificadas por ArchUnit.
- Tipagem forte nas duas pontas (TypeScript `strict`; no Java, tipos de domínio em vez de `String` para tudo).
- Componentes de UI isolados e documentados em `@portfolio/ui`.
- Nenhum arquivo com mais de ~200 linhas; nenhuma função com mais de ~20.

**Medição:** SonarCloud (complexidade cognitiva, duplicação < 3%) e o tempo real da tarefa, cronometrado.

### 2.3 Performance

**Cenário:** um recrutador abre o site em um celular de gama média, em rede 4G.
**Resposta esperada:** conteúdo principal visível em menos de 2 segundos.

**Orçamento de performance:**

| Métrica | Alvo | Limite rígido (falha o CI) |
|---------|------|---------------------------|
| Lighthouse Performance (mobile) | ≥ 95 | ≥ 90 |
| LCP | < 1.8s | < 2.5s |
| INP | < 150ms | < 200ms |
| CLS | < 0.05 | < 0.1 |
| JS inicial (comprimido) | < 100 KB | < 130 KB |
| CSS inicial | < 20 KB | < 30 KB |
| p95 da API (cache quente) | < 80ms | < 200ms |

Detalhamento na [seção 10](#10-estratégia-de-performance).

### 2.4 Segurança

**Cenário:** um agente automatizado varre o site em busca de vulnerabilidades comuns.
**Resposta esperada:** nenhuma superfície explorável; abuso do formulário de contato é contido.

**Como:**

- Nenhum segredo no cliente. Token do GitHub e chave de e-mail vivem apenas no servidor.
- BFF no Next.js: o navegador nunca fala direto com a API Java. Elimina CORS permissivo e esconde a origem.
- Autenticação entre BFF e API por chave compartilhada em header, validada por filtro do Spring Security.
- Rate limiting com Bucket4j: 5 req/h por IP em `POST /contact`; 60/min nos endpoints de leitura.
- Antispam em camadas: honeypot + Cloudflare Turnstile + validação de tamanho e conteúdo.
- Bean Validation em todos os DTOs; erros no formato RFC 9457 (Problem Details).
- Apenas JPA com queries parametrizadas; zero concatenação de SQL.
- Headers: CSP, HSTS, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`. A CSP é **estática, sem nonce** — o nonce exigiria renderização por requisição e desligaria o ISR que protege o visitante do cold start. Ver [ADR-0012](#adr-0012-csp-estática-sem-nonce-emenda-a-seção-24).
- Dependabot semanal, Trivy no build da imagem, OWASP Dependency-Check no CI, CodeQL nas duas linguagens.
- Logs sem PII: o e-mail do contato é registrado mascarado.
- LGPD: aviso claro no formulário sobre finalidade e retenção dos dados.

### 2.5 SEO

**Cenário:** alguém pesquisa "Crystofer Demetino desenvolvedor backend" no Google.
**Resposta esperada:** o portfólio aparece em primeiro lugar, com rich snippet de perfil.

**Como:** metadata dinâmica pela Metadata API do Next, JSON-LD (`Person`, `WebSite`, `BreadcrumbList`, `SoftwareSourceCode`), sitemap e robots gerados, OG images dinâmicas, URLs semânticas, `hreflang`. Detalhes na [seção 11](#11-estratégia-de-seo).

### 2.6 Responsividade

**Cenário:** o site é aberto em telas de 320px a 2560px.
**Resposta esperada:** layout íntegro, sem scroll horizontal, sem texto cortado, alvos de toque ≥ 44×44px.

**Como:** mobile-first com breakpoints do Tailwind, tipografia fluida com `clamp()`, layouts em CSS Grid com `auto-fit`/`minmax`, container queries onde o componente deve reagir ao contêiner e não à viewport, imagens com `sizes` corretos.

### 2.7 Acessibilidade

**Cenário:** uma pessoa usando leitor de tela navega pelo portfólio.
**Resposta esperada:** toda a informação e todas as ações estão disponíveis; a ordem de leitura faz sentido.

**Como:** conformidade WCAG 2.2 nível AA — HTML semântico, contraste mínimo 4.5:1 (texto) e 3:1 (UI), foco visível, skip link, `prefers-reduced-motion`, axe-core no CI e verificação manual com NVDA/VoiceOver. Detalhes na [seção 12](#12-estratégia-de-acessibilidade).

### 2.8 Modularização

**Cenário:** o módulo `contact` precisa ser extraído para um serviço separado.
**Resposta esperada:** a extração é possível sem refatorar os demais módulos.

**Como:** cada módulo do backend (`profile`, `projects`, `github`, `contact`) tem domínio, casos de uso e adaptadores próprios. Módulos **não** se referenciam diretamente — comunicam-se por eventos de aplicação do Spring. ArchUnit proíbe importações entre pacotes de módulos distintos.

### 2.9 Reutilização de componentes

**Cenário:** uma nova seção precisa de um card com o mesmo comportamento do card de projeto.
**Resposta esperada:** reutilizar o primitivo existente, sem duplicar estilo.

**Como:** `@portfolio/ui` expõe primitivos headless (Radix + CVA) desacoplados de domínio; componentes de domínio ficam em `apps/web` e compõem os primitivos. A regra: **um primitivo não sabe o que é um "projeto"**.

### 2.10 Observabilidade *(atributo adicionado ao pedido original)*

**Cenário:** o formulário de contato para de enviar e-mails às 3h da manhã.
**Resposta esperada:** é possível descobrir a causa sem reproduzir o bug.

**Como:** Spring Boot Actuator, métricas Micrometer em `/actuator/prometheus`, logs estruturados em JSON com `correlationId` propagado via MDC, health checks das dependências (Postgres, GitHub, provedor de e-mail) e Sentry no frontend.

Sem observabilidade, todos os atributos acima são inverificáveis em produção — por isso ele entra na lista mesmo não tendo sido pedido.

### 2.11 Matriz de trade-offs

Atributos de qualidade competem entre si. Estas são as trocas conscientes:

| Conflito | Decisão | Justificativa |
|----------|---------|---------------|
| Performance × riqueza visual | Animações apenas em `transform` e `opacity`; Framer Motion sob demanda | Manter o budget de JS abaixo de 100 KB é inegociável |
| Segurança × simplicidade | O BFF adiciona um salto de rede | Esconder segredos e evitar CORS aberto vale o salto |
| Escalabilidade × custo | Sem Redis na v1 | Caffeine atende o volume; Redis seria over-engineering documentado |
| Manutenibilidade × velocidade inicial | Hexagonal é mais verboso que MVC | O projeto existe para demonstrar arquitetura; a verbosidade **é** parte do entregável |
| Modularização × pragmatismo | Monólito modular, não microsserviços | Microsserviços aqui seriam erro de julgamento — e um avaliador sênior perceberia |
| Riqueza de conteúdo × prazo | Seeds realistas desde o MVP 1 | Conteúdo final não bloqueia a engenharia |

---

## 3. Arquitetura Geral

### 3.1 Contexto (C4 — Nível 1)

```
                       ┌──────────────────────────────┐
                       │          Visitante            │
                       │  (recrutador, tech lead, dev) │
                       └───────────────┬──────────────┘
                                       │ HTTPS
                                       ▼
        ┌──────────────────────────────────────────────────────┐
        │            PORTFÓLIO (Sistema)                        │
        │  ┌────────────────────┐      ┌────────────────────┐   │
        │  │   Web (Next.js)    │─────▶│   API (Spring)     │   │
        │  │   SSG + ISR + BFF  │ HTTP │   Java 21          │   │
        │  └────────────────────┘      └─────────┬──────────┘   │
        └────────────────────────────────────────┼──────────────┘
                                                 │
              ┌──────────────┬───────────────────┼──────────────┐
              ▼              ▼                   ▼              ▼
        ┌───────────┐  ┌───────────┐      ┌───────────┐  ┌───────────┐
        │PostgreSQL │  │GitHub API │      │  Resend   │  │ Turnstile │
        │ (dados)   │  │(externo)  │      │ (e-mail)  │  │(antispam) │
        └───────────┘  └───────────┘      └───────────┘  └───────────┘
```

### 3.2 Contêineres (C4 — Nível 2)

| Contêiner | Tecnologia | Responsabilidade | Onde roda |
|-----------|-----------|------------------|-----------|
| **web** | Next.js 15, React 19, TS | Renderização, SEO, UI, BFF | Vercel (edge + serverless) |
| **api** | Java 21, Spring Boot 4.1 | Regras de negócio, persistência, integrações | Render (container Docker) |
| **db** | PostgreSQL 16 | Fonte de verdade do conteúdo e das mensagens | Neon ([ADR-0010](#adr-0010-postgres-no-neon-emenda-o-adr-0006)) |
| **cache** | Caffeine (in-process) | Cache de leitura e de respostas do GitHub | Dentro da `api` |
| **ui** | Biblioteca React | Primitivos de design system | Pacote do monorepo |
| **api-client** | TS gerado do OpenAPI | Contrato tipado entre web e api | Pacote do monorepo |

### 3.3 Fluxo da aplicação

#### Fluxo A — Leitura de conteúdo (99% das requisições)

```
1. Visitante                → GET https://portfolio.dev/projetos
2. Vercel CDN               → HIT? devolve HTML estático em ~50ms. FIM.
3. (MISS ou revalidação)    → Next.js Server Component executa
4. Server Component         → fetch(API_URL/api/v1/projects,
                                   { next: { tags: ['projects'], revalidate: 3600 } })
5. Spring: Controller       → valida a chave de serviço no header
6. Controller               → chama a porta de entrada (use case)
7. Use case                 → consulta a porta de saída (repositório)
8. Adapter de persistência  → Caffeine HIT? devolve. MISS → Postgres
9. Resposta sobe            → Domain → DTO → JSON (ETag + Cache-Control)
10. Next renderiza HTML     → armazena na CDN → devolve ao visitante
```

**Ponto arquitetural:** o passo 2 termina a maior parte das requisições. A API é um detalhe de build-time, não um ponto único de falha em runtime. É isso que permite ao free tier do Render ser suficiente.

#### Fluxo B — Envio do formulário de contato (escrita)

```
1. Cliente         → resolve o desafio do Turnstile
2. Cliente         → POST /api/contact (Route Handler do Next — mesma origem)
3. Route Handler   → valida o schema com Zod
4. Route Handler   → verifica o token do Turnstile junto à Cloudflare
5. Route Handler   → repassa para a API com a chave de serviço e o IP real
6. Spring Filter   → autentica a chave de serviço
7. Bucket4j        → o IP excedeu 5/hora? → 429 com Problem Details. FIM.
8. Controller      → Bean Validation no DTO
9. Use case        → cria o agregado ContactMessage (validação de domínio)
10. Porta de saída → persiste a mensagem no Postgres
11. Use case       → publica ContactMessageReceivedEvent
12. Listener @Async (AFTER_COMMIT) → envia e-mail via Resend
13. Resposta       → 202 Accepted (o usuário não espera o e-mail sair)
```

**Ponto arquitetural:** o envio de e-mail é desacoplado por evento e ocorre **após o commit**. Se o Resend estiver fora do ar, a mensagem não se perde — ela está no banco. Um `@Scheduled` reprocessa mensagens com `emailStatus = FAILED`. É o padrão *transactional outbox* em sua forma mais simples e defensável.

#### Fluxo C — Estatísticas do GitHub (integração externa instável)

```
1. Next (ISR, a cada 6h)   → GET /api/v1/github/stats
2. Use case                → consulta a porta GitHubStatsProvider
3. Adapter GitHub          → Caffeine HIT (TTL 6h)? devolve. FIM.
4. (MISS) Resilience4j     → CircuitBreaker aberto? → fallback no cache expirado
5. Retry (3x, backoff exp.)→ RestClient → api.github.com (timeout de 3s)
6. Sucesso                 → mapeia para o modelo de domínio → grava no cache
7. Falha em tudo           → fallback: último valor conhecido, ou payload vazio
                             (o site nunca quebra por causa do GitHub)
8. @Scheduled (a cada 6h)  → reaquece o cache proativamente
```

### 3.4 Arquitetura interna do backend — Hexagonal (Ports & Adapters)

#### Por que hexagonal, e não MVC em camadas

MVC em camadas (`controller → service → repository`) funciona, mas cria uma dependência estrutural: o serviço depende da interface do repositório, que normalmente estende `JpaRepository` — ou seja, **o núcleo do negócio conhece o Spring Data**. A regra de negócio fica acoplada ao framework de persistência.

Na hexagonal, a dependência é invertida:

```
        ┌───────────────────────────────────────────────┐
        │                  ADAPTERS IN                   │
        │   REST Controller · Scheduler · CLI            │
        └────────────────────┬──────────────────────────┘
                             │ implementa/chama
                             ▼
        ┌───────────────────────────────────────────────┐
        │                   PORTS IN                     │
        │   ListProjectsUseCase · SubmitContactUseCase   │
        └────────────────────┬──────────────────────────┘
                             ▼
        ┌───────────────────────────────────────────────┐
        │        APPLICATION (orquestração)              │
        │   ProjectService · ContactService              │
        └────────────────────┬──────────────────────────┘
                             ▼
        ┌───────────────────────────────────────────────┐
        │             DOMAIN (puro, sem framework)       │
        │   Project · Skill · ContactMessage · Slug      │
        │   regras de negócio e invariantes              │
        └────────────────────┬──────────────────────────┘
                             ▼
        ┌───────────────────────────────────────────────┐
        │                  PORTS OUT                     │
        │   LoadProjectPort · SendEmailPort              │
        └────────────────────┬──────────────────────────┘
                             │ implementado por
                             ▼
        ┌───────────────────────────────────────────────┐
        │                 ADAPTERS OUT                   │
        │   JPA Repository · GitHub Client · Resend      │
        └───────────────────────────────────────────────┘
```

**A regra da dependência:** as setas apontam sempre para dentro. `domain` não importa nada de `application`, `adapter` ou `org.springframework`. Isso não é uma convenção documentada — é **um teste ArchUnit que quebra o build**:

```java
@ArchTest
static final ArchRule dominio_nao_depende_de_framework =
    noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "..adapter..");
```

#### Ganhos concretos

| Ganho | Como se manifesta neste projeto |
|-------|--------------------------------|
| Testes de domínio sem Spring | Regras de negócio testadas em milissegundos, sem subir contexto |
| Troca de infraestrutura barata | Migrar Postgres → outro banco toca apenas `adapter/out/persistence` |
| Entidade de domínio ≠ entidade JPA | Sem anotações de persistência vazando para o modelo de negócio |
| Fronteira explícita para o avaliador | O nome da pasta comunica a intenção arquitetural em 5 segundos |

#### Custo assumido

Mais classes e um mapper por adaptador. Para um CRUD de 4 tabelas, isso é objetivamente *over-engineering* — **e essa é exatamente a escolha correta aqui**, porque o produto entregue não é o CRUD: é a demonstração de arquitetura. Registrado no [ADR-0003](#adr-0003-arquitetura-hexagonal-com-monólito-modular). Ser capaz de nomear o próprio trade-off é parte do que se está demonstrando.

### 3.5 Organização por módulos (monólito modular)

O backend é dividido em **módulos por contexto de negócio**, não por camada técnica:

| Módulo | Responsabilidade | Portas de entrada | Portas de saída |
|--------|------------------|-------------------|-----------------|
| `profile` | Bio, headline, links sociais, skills, timeline | `GetProfileUseCase`, `ListSkillsUseCase`, `ListExperiencesUseCase` | `LoadProfilePort`, `LoadSkillPort` |
| `projects` | Catálogo de projetos, filtro, detalhe | `ListProjectsUseCase`, `GetProjectBySlugUseCase` | `LoadProjectPort` |
| `github` | Integração, agregação e cache de estatísticas | `GetGitHubStatsUseCase` | `GitHubStatsProviderPort` |
| `contact` | Recebimento, persistência e notificação de mensagens | `SubmitContactMessageUseCase` | `SaveContactMessagePort`, `SendEmailPort` |
| `shared` | Config, tratamento de erro, segurança, observabilidade | — | — |

**Comunicação entre módulos.** Regra: módulos não se importam entre si. Nenhuma classe de `contact` importa qualquer classe de `projects`. Quando um módulo precisa reagir a algo que aconteceu em outro, a comunicação é por **evento de aplicação do Spring**:

```java
// contact/application/ContactService.java
events.publishEvent(new ContactMessageReceivedEvent(id, senderEmailMasked, receivedAt));

// contact/adapter/out/email/ContactEmailListener.java
@TransactionalEventListener(phase = AFTER_COMMIT)
@Async
void on(ContactMessageReceivedEvent event) { ... }
```

Por que isso importa: é **exatamente a mesma fronteira** que existiria entre dois microsserviços. Se um dia o módulo precisar virar um serviço próprio, troca-se o `ApplicationEventPublisher` por um broker de mensagens, e o restante do código não muda. O monólito modular é a decisão certa hoje, sem fechar a porta do amanhã.

### 3.6 Organização do frontend

O `apps/web` segue uma variação de **Feature-Sliced Design**, adaptada ao App Router:

| Camada | Conteúdo | Pode importar de |
|--------|----------|------------------|
| `app/` | Rotas, layouts, metadata, Route Handlers (BFF) | features, components, lib |
| `features/` | Seções de domínio (hero, projects, contact…) | components, lib, types |
| `components/` | Componentes compartilhados da aplicação | `@portfolio/ui`, lib |
| `@portfolio/ui` | Primitivos sem domínio (Button, Card, Badge…) | nada do app |
| `lib/` | Clientes, utilitários, configuração | types |
| `types/` | Contratos e tipos | nada |

Dependências fluem **para baixo apenas**, garantido por `eslint-plugin-boundaries`. É o mesmo princípio da regra de dependência do backend, aplicado ao frontend.

**Server vs. Client Components.** A regra padrão: **tudo é Server Component até que se prove o contrário**. `"use client"` só aparece quando o componente precisa de estado, efeito, ou API de navegador. Consequência prática: os dados do portfólio nunca viram JSON no bundle do cliente, e o JS inicial fica no orçamento de 100 KB.

### 3.7 Modelo de dados

```
profile (1) ──< social_link
project (N) ──< project_tech >── technology
project (1) ──< project_metric
skill (N) ──> skill_category
experience (timeline)
contact_message
```

| Tabela | Campos principais |
|--------|-------------------|
| `profile` | id, full_name, headline, bio, location, available_for_work, resume_url |
| `social_link` | id, profile_id, platform, url, display_order |
| `project` | id, slug (único), title, summary, problem, solution, outcome, repo_url, live_url, cover_image, featured, display_order, published_at |
| `technology` | id, name, slug, category, icon_slug |
| `project_tech` | project_id, technology_id |
| `project_metric` | id, project_id, label, value (ex.: "p95", "80ms") |
| `skill_category` | id, name, display_order |
| `skill` | id, category_id, name, proficiency (ENUM), years_of_experience |
| `experience` | id, company, role, start_date, end_date (nullable), description, highlights (jsonb) |
| `contact_message` | id, name, email, subject, message, ip_hash, user_agent, email_status (ENUM), created_at |

**Decisões de modelagem:**

- `slug` único e indexado — a URL é parte do contrato público e não deve depender de ID sequencial.
- `ip_hash` em vez de IP puro — rate limit e auditoria sem armazenar PII desnecessária (LGPD).
- `email_status` como enum (`PENDING`, `SENT`, `FAILED`) — habilita o reprocessamento do Fluxo B.
- `highlights` em `jsonb` — lista de bullets sem tabela extra; leitura simples, escrita rara.
- Toda a estrutura em migrações Flyway, com seed de conteúdo em migration separada e idempotente.

### 3.8 Contrato de API

- **Estilo:** REST sobre HTTP, JSON, versionado em `/api/v1`.
- **Erros:** RFC 9457 (Problem Details) via `ProblemDetail` do Spring 6 — resposta uniforme para todos os erros.
- **Documentação:** springdoc-openapi gera o `openapi.json` no build; o pacote `@portfolio/api-client` é gerado a partir dele. Contrato divergente = build quebrado.
- **Cache HTTP:** `ETag` + `Cache-Control: public, max-age=300, stale-while-revalidate=3600`.
- **Nomenclatura:** substantivos no plural, kebab-case nas URLs, camelCase nos campos JSON.

```json
{
  "type": "https://portfolio.dev/errors/rate-limit-exceeded",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Limite de 5 mensagens por hora atingido.",
  "instance": "/api/v1/contact",
  "retryAfterSeconds": 2400,
  "correlationId": "0f3c1a9e-..."
}
```

### 3.9 Estratégia de cache (três níveis)

| Nível | Onde | O que guarda | TTL | Invalidação |
|-------|------|--------------|-----|-------------|
| L0 | CDN da Vercel | HTML e assets | Imutável (assets) / ISR (HTML) | `revalidateTag()` |
| L1 | `fetch` cache do Next | Respostas da API | 1h (conteúdo), 6h (GitHub) | Tag + tempo |
| L2 | Caffeine na API | Entidades e stats do GitHub | 10 min (conteúdo), 6h (GitHub) | TTL + `@Scheduled` |

Redis fica **fora da v1**, deliberadamente: com uma única instância da API, um cache distribuído não resolve nenhum problema existente. Decisão consciente, não omissão — e a interface de cache já é abstraída, então adicioná-lo depois é trocar uma implementação.

### 3.10 Resiliência

| Padrão | Onde | Configuração |
|--------|------|--------------|
| Timeout | Todas as chamadas HTTP externas | connect 2s / read 3s |
| Retry | GitHub, Resend | 3 tentativas, backoff exponencial, jitter |
| Circuit Breaker | GitHub | abre com 50% de falha em 10 chamadas; half-open após 60s |
| Fallback | GitHub | último valor cacheado; se não houver, payload vazio válido |
| Bulkhead | GitHub | máx. 5 chamadas concorrentes |
| Outbox simplificado | Contato | mensagem persistida antes do envio; retentativa agendada |
| Graceful shutdown | API | `server.shutdown=graceful`, 30s de espera |

Implementação com **Resilience4j** (não Hystrix, que está em manutenção). Todos os estados de circuito são expostos como métricas Micrometer.

### 3.11 Observabilidade

| Pilar | Ferramenta | Detalhe |
|-------|-----------|---------|
| Health | Actuator | `/actuator/health` com indicadores de DB, GitHub e e-mail |
| Métricas | Micrometer → Prometheus | Latência por endpoint, taxa de erro, estado do circuit breaker, hit ratio do cache |
| Logs | Logback + encoder JSON | Estruturados, com `correlationId`, `traceId` e nível por pacote |
| Correlação | Filtro + MDC | O BFF gera o `X-Correlation-Id`; a API propaga e devolve |
| Frontend | Sentry + Vercel Analytics | Erros de runtime e Web Vitals reais |

### 3.12 Ambientes

| Ambiente | Web | API | Banco | Objetivo |
|----------|-----|-----|-------|----------|
| **local** | `pnpm dev` | `mvn spring-boot:run` (perfil `local`) | Postgres no Docker Compose | Desenvolvimento |
| **test** | Vitest / Playwright | JUnit + Testcontainers | Postgres efêmero | CI |
| **preview** | Preview da Vercel por PR | Staging no Render | Banco de staging | Revisão de PR |
| **production** | Vercel (main) | Render (main) | Neon, branch `production` | Público |

Nenhuma configuração fica hardcoded: `application.yml` por perfil, variáveis de ambiente injetadas, nenhum segredo versionado. `.env.example` documenta todas as variáveis exigidas.

---

## 4. Escolha da Stack

> Regra: nenhuma tecnologia entra por ser popular. Cada uma responde a **um problema específico** deste projeto, e cada escolha lista a alternativa descartada e por quê. Saber justificar é metade da competência arquitetural; a outra metade é saber recusar.

### 4.1 Backend

| Tecnologia | Problema que resolve | Por quê | Alternativa descartada |
|-----------|---------------------|---------|------------------------|
| **Java 21 (LTS)** | Linguagem-alvo das vagas backend enterprise | LTS até 2031; Records eliminam boilerplate; sealed interfaces + pattern matching dão exaustividade verificada pelo compilador; **Virtual Threads** tornam chamadas bloqueantes baratas; text blocks | **Java 17** — sem Virtual Threads nem pattern matching completo |
| **Spring Boot 4.1** *([ADR-0011](#adr-0011-spring-boot-41-substitui-o-adr-0009))* | Infraestrutura de aplicação sem escrevê-la | Domínio absoluto no mercado enterprise; Actuator; `@TransactionalEventListener`; `RestClient` moderno; `ProblemDetail` nativo; Virtual Threads em uma linha de config | **Quarkus** — melhor startup, menor demanda de mercado |
| **Maven** | Build e ciclo de testes | Declarativo e previsível; plugins maduros (Flyway, JaCoCo, Spotless, OWASP); é o que times Spring usam | **Gradle** — Kotlin DSL adiciona uma linguagem a mais na leitura |
| **PostgreSQL 16** | Persistência confiável | ACID sólido; `jsonb`; full-text search nativo; free tier em todo PaaS; o relacional mais requisitado | **MongoDB** — os dados são fortemente relacionais |
| **Flyway** | Evolução versionada do schema | SQL puro versionado em Git, validado no CI. Substitui `ddl-auto` (aceitável em tutorial, inaceitável em produção). Bônus: adicionar projeto ao portfólio **é** escrever uma migration | `ddl-auto=update` |
| **Resilience4j** | A API do GitHub tem rate limit e cai | Funcional e leve; integração de primeira classe com Spring e Micrometer | **Hystrix** — em manutenção |
| **Bucket4j** | Formulário público convida spam | Token bucket maduro; funciona in-memory e troca para Redis sem mudar a lógica | Implementação própria |
| **Caffeine** | Evitar ida ao banco e ao GitHub | Cache local mais rápido do ecossistema Java (W-TinyLFU); integrado ao `@Cacheable`; estatísticas no Micrometer | **Redis** — sem problema distribuído a resolver na v1 |
| **springdoc-openapi** | Documentação e geração do cliente TS | Gera OpenAPI 3.1 dos controllers; produz o `openapi.json` que alimenta o `@portfolio/api-client` — contrato verificável em build-time | Documentação manual |
| **MapStruct** | Hexagonal exige mapear domínio ↔ JPA ↔ DTO | Gera mappers em tempo de compilação (sem reflection) e falha o build se um campo ficar sem mapeamento | Mapeamento manual |

### 4.2 Frontend

| Tecnologia | Problema que resolve | Por quê | Alternativa descartada |
|-----------|---------------------|---------|------------------------|
| **Next.js 15 (App Router)** | SEO, performance e BFF em um framework | Server Components mantêm dados fora do bundle; SSG+ISR com `revalidateTag`; Metadata API tipada; Route Handlers = BFF sem projeto separado; `next/image` e `next/font` | **Astro** (menor demanda, sem Route Handlers); **Vite + React** (sem SSR/SSG, inviabiliza SEO) |
| **React 19** | Camada de componentes | Base do Next; `useActionState` e Server Actions simplificam o formulário sem estado global | — |
| **TypeScript strict** | Erros de contrato em produção | Com `strict`, `noUncheckedIndexedAccess` e tipos gerados do OpenAPI, mudança no backend quebra o **build** do frontend | JavaScript |
| **Tailwind CSS 4** | Estilo consistente e CSS pequeno | Design system em tokens CSS (`@theme`); CSS final ~15 KB; zero CSS morto; v4 usa variáveis nativas, facilitando o tema | **CSS-in-JS** — runtime + incompatível com RSC |
| **shadcn/ui + Radix** | A11y de componentes interativos é difícil | Radix resolve foco, `aria`, teclado e portais; shadcn entrega o código **dentro do repositório**, não como dependência opaca | **MUI** — bundle grande, estilo difícil de sobrescrever |
| **Framer Motion** | Animação polida sem custo | Declarativa em `transform`/`opacity` (GPU); `prefers-reduced-motion` nativo; importável dinamicamente | CSS puro (menos controle de orquestração) |
| **Zod** | Validar entrada no BFF | Schema único = validação em runtime **e** fonte do tipo TS | Validação manual |
| **next-intl** | i18n com rotas localizadas e SEO | Integração nativa com App Router; `hreflang`; mensagens tipadas | `next-i18next` (App Router) |

### 4.3 Testes

| Camada | Ferramenta | Papel |
|--------|-----------|-------|
| Unitário (Java) | JUnit 5 + AssertJ | Regras de domínio, sem Spring |
| Mocks | Mockito | Dublês para portas de saída |
| Fatia web | `@WebMvcTest` | Controllers isolados |
| Fatia dados | `@DataJpaTest` | Repositórios contra Postgres real |
| Integração | **Testcontainers** | Postgres em Docker, descartável, idêntico ao de produção |
| HTTP externo | **WireMock** | Simula GitHub, inclusive falhas e timeouts — testa o circuit breaker |
| Arquitetura | **ArchUnit** | Faz as regras de dependência falharem o build |
| Cobertura | JaCoCo | Mínimo de 80% em `domain` e `application` |
| Unitário (front) | Vitest + Testing Library | Comportamento de componentes |
| E2E | Playwright | Jornadas críticas em 3 navegadores |
| A11y | `@axe-core/playwright` | Zero violações, verificado no CI |
| Performance | Lighthouse CI | Falha o PR se sair do orçamento |
| Carga | k6 | Relatório pontual versionado |

**Sobre Testcontainers:** é o divisor entre "sei escrever teste" e "sei testar sistemas". Testar repositório com H2 em memória valida um banco que não existe em produção — dialetos divergem e o bug aparece depois do deploy. Testcontainers sobe o Postgres 16 real.

### 4.4 Ferramentas

| Ferramenta | Papel | Justificativa |
|-----------|-------|---------------|
| **pnpm** | Pacotes | Links simbólicos: mais rápido, menos disco; workspaces nativos |
| **Turborepo** | Monorepo | Cache de tarefas e execução paralela; só reconstrói o que mudou |
| **ESLint 9 (flat)** | Lint TS/React | Inclui `eslint-plugin-boundaries` e `jsx-a11y` |
| **Prettier** | Formatação | Fim das discussões de estilo; roda no pre-commit |
| **Spotless + Google Java Format** | Formatação Java | Equivalente do Prettier no backend, verificado no CI |
| **Husky + lint-staged** | Git hooks | Impede commit de código quebrado |
| **Commitlint** | Validação de commits | Garante Conventional Commits |
| **GitHub Actions** | CI/CD | Nativo, free tier generoso, matriz por app |
| **Dependabot** | Dependências | PRs semanais de segurança |
| **CodeQL** | SAST | Análise estática para Java e TypeScript |
| **Trivy** | Scan de container | Vulnerabilidades na imagem Docker |
| **SonarCloud** | Qualidade contínua | Complexidade, duplicação, code smells — com badge |
| **Docker + Compose** | Ambiente e empacotamento | Multi-stage; `docker compose up` sobe tudo |

### 4.5 Serviços externos

| Serviço | Uso | Plano | Alternativa descartada |
|---------|-----|-------|------------------------|
| **Vercel** | Hospedagem do web | Hobby | Netlify — App Router menos maduro |
| **Render** | API + Postgres | Free | Railway (crédito limitado), Fly.io (mais complexo) |
| **GitHub API** | Estatísticas | Grátis (5.000/h com token) | — |
| **Resend** | E-mail transacional | Free (3.000/mês) | SMTP do Gmail — frágil, entregabilidade ruim |
| **Cloudflare Turnstile** | Antispam | Grátis | reCAPTCHA — pior UX e privacidade |
| **Vercel Analytics** | Web Vitals reais | Free | Google Analytics — mais pesado, exige consentimento |
| **Sentry** | Erros | Free (5k eventos/mês) | — |

---

## 5. Estrutura de Pastas

> Princípio: **a estrutura de pastas é documentação executável**. Um avaliador deve inferir a arquitetura pelos nomes das pastas, sem abrir um único arquivo.

### 5.1 Raiz do monorepo

```
portfolio/
├── apps/
│   ├── web/                    # Next.js 15 — site e BFF
│   └── api/                    # Java 21 + Spring Boot — API REST
├── packages/
│   ├── ui/                     # Design system (primitivos React)
│   ├── api-client/             # Cliente TS gerado do OpenAPI
│   ├── eslint-config/          # Config de lint compartilhada
│   └── typescript-config/      # tsconfig base compartilhado
├── infra/
│   ├── docker/                 # Dockerfiles e Compose
│   ├── k6/                     # Scripts de teste de carga
│   └── scripts/                # seed, backup, keep-alive
├── docs/
│   ├── PLANO-COMPLETO.md       # este documento
│   ├── diagrams/               # Diagramas C4 (.drawio / .svg)
│   └── reports/                # Lighthouse, k6, cobertura
├── .github/
│   ├── workflows/
│   │   ├── ci-web.yml
│   │   ├── ci-api.yml
│   │   ├── codeql.yml
│   │   ├── lighthouse.yml
│   │   └── deploy.yml
│   ├── ISSUE_TEMPLATE/
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── dependabot.yml
├── .husky/
├── .editorconfig
├── .gitignore
├── .nvmrc
├── commitlint.config.js
├── package.json
├── pnpm-workspace.yaml
├── turbo.json
├── LICENSE
└── README.md
```

### 5.2 `apps/web` — Next.js

```
apps/web/
├── public/
│   ├── fonts/
│   ├── images/
│   ├── resume/crystofer-cv-pt-br.pdf
│   ├── icons/tech-sprite.svg
│   ├── favicon.ico
│   ├── icon.svg
│   └── apple-icon.png
│
├── src/
│   ├── app/
│   │   ├── [locale]/
│   │   │   ├── layout.tsx              # Fontes, tema, providers
│   │   │   ├── page.tsx                # Home
│   │   │   ├── loading.tsx
│   │   │   ├── error.tsx
│   │   │   ├── not-found.tsx
│   │   │   ├── sobre/page.tsx
│   │   │   ├── projetos/
│   │   │   │   ├── page.tsx            # Listagem
│   │   │   │   └── [slug]/
│   │   │   │       ├── page.tsx        # Detalhe (generateStaticParams)
│   │   │   │       └── opengraph-image.tsx
│   │   │   └── contato/page.tsx
│   │   ├── api/                        # BFF — Route Handlers
│   │   │   ├── contact/route.ts
│   │   │   └── revalidate/route.ts
│   │   ├── sitemap.ts
│   │   ├── robots.ts
│   │   ├── manifest.ts
│   │   ├── opengraph-image.tsx
│   │   └── globals.css
│   │
│   ├── features/                       # Fatias verticais por domínio
│   │   ├── hero/components/HeroSection.tsx
│   │   ├── about/components/{AboutSection,Timeline,TimelineItem}.tsx
│   │   ├── skills/
│   │   │   ├── components/{SkillsSection,SkillCard,SkillCategory}.tsx
│   │   │   └── lib/group-by-category.ts
│   │   ├── projects/
│   │   │   ├── components/{ProjectsSection,ProjectCard,ProjectFilter,
│   │   │   │               ProjectDetail,ProjectMetrics,TechStack}.tsx
│   │   │   └── lib/filter-projects.ts
│   │   ├── github/
│   │   │   └── components/{GitHubStats,LanguageChart,
│   │   │                   ContributionGraph,RepositoryCard}.tsx
│   │   └── contact/
│   │       ├── components/{ContactForm,ContactInfo,ContactSection}.tsx
│   │       ├── actions/submit-contact.ts     # Server Action
│   │       └── schemas/contact-schema.ts     # Zod
│   │
│   ├── components/
│   │   ├── layout/{Navbar,MobileNav,Footer,Container,Section}.tsx
│   │   ├── common/{SectionHeading,SocialLinks,ThemeToggle,
│   │   │           LocaleSwitcher,ScrollToTop,SkipLink,ResumeDownload}.tsx
│   │   ├── motion/{FadeIn,StaggerContainer,MotionProvider}.tsx
│   │   └── seo/{JsonLd.tsx,structured-data.ts}
│   │
│   ├── hooks/
│   │   ├── use-media-query.ts
│   │   ├── use-scroll-position.ts
│   │   ├── use-active-section.ts
│   │   ├── use-reduced-motion.ts
│   │   └── use-copy-to-clipboard.ts
│   │
│   ├── lib/
│   │   ├── api/{client,projects,profile,github}.ts
│   │   ├── env.ts                     # Validação de env com Zod (falha no boot)
│   │   ├── metadata.ts
│   │   ├── fonts.ts
│   │   ├── utils.ts
│   │   └── constants.ts
│   │
│   ├── types/{project,skill,profile,github}.ts
│   ├── i18n/
│   │   ├── {routing,request}.ts
│   │   └── messages/{pt-BR,en-US}.json
│   └── styles/{theme,animations}.css
│
├── e2e/{home,projects,contact,accessibility}.spec.ts
├── .env.example
├── eslint.config.mjs
├── next.config.ts
├── playwright.config.ts
├── postcss.config.mjs
├── tsconfig.json
├── vitest.config.ts
└── package.json
```

**Regras de dependência** (aplicadas por `eslint-plugin-boundaries`):

```
app/  →  features/  →  components/  →  @portfolio/ui
  ↓         ↓              ↓
 lib/ ←────┴──────────────┘
  ↓
types/
```

- `features/` **não** importa de outra `features/`. Se duas precisam do mesmo código, ele sobe para `components/` ou `lib/`.
- `@portfolio/ui` **não** importa nada de `apps/web`.
- `types/` não importa de ninguém.

### 5.3 `apps/api` — Spring Boot

```
apps/api/
├── src/main/java/dev/crystofer/portfolio/
│   ├── PortfolioApplication.java
│   │
│   ├── shared/                                # Cross-cutting
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── CacheConfig.java
│   │   │   ├── OpenApiConfig.java
│   │   │   ├── RestClientConfig.java
│   │   │   ├── AsyncConfig.java
│   │   │   ├── JacksonConfig.java
│   │   │   └── properties/{GitHubProperties,MailProperties,
│   │   │                   RateLimitProperties}.java
│   │   ├── error/
│   │   │   ├── GlobalExceptionHandler.java    # @RestControllerAdvice
│   │   │   ├── DomainException.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   ├── RateLimitExceededException.java
│   │   │   └── ErrorType.java
│   │   ├── web/
│   │   │   ├── CorrelationIdFilter.java
│   │   │   ├── ServiceKeyAuthFilter.java
│   │   │   └── RateLimitInterceptor.java
│   │   └── domain/{Slug,EmailAddress,AggregateRoot}.java
│   │
│   ├── profile/
│   │   ├── domain/
│   │   │   ├── model/{Profile,SocialLink,Skill,SkillCategory,
│   │   │   │         Proficiency,Experience}.java
│   │   │   └── port/
│   │   │       ├── in/{GetProfileUseCase,ListSkillsUseCase,
│   │   │       │      ListExperiencesUseCase}.java
│   │   │       └── out/{LoadProfilePort,LoadSkillPort,
│   │   │               LoadExperiencePort}.java
│   │   ├── application/{ProfileService,SkillService,ExperienceService}.java
│   │   └── adapter/
│   │       ├── in/web/
│   │       │   ├── {Profile,Skill,Experience}Controller.java
│   │       │   ├── dto/{Profile,Skill,Experience}Response.java
│   │       │   └── mapper/ProfileWebMapper.java
│   │       └── out/persistence/
│   │           ├── entity/{Profile,Skill,Experience}Entity.java
│   │           ├── repository/{Profile,Skill}JpaRepository.java
│   │           ├── ProfilePersistenceAdapter.java
│   │           └── mapper/ProfilePersistenceMapper.java
│   │
│   ├── projects/
│   │   ├── domain/
│   │   │   ├── model/{Project,Technology,ProjectMetric,ProjectStatus}.java
│   │   │   └── port/
│   │   │       ├── in/{ListProjectsUseCase,GetProjectBySlugUseCase}.java
│   │   │       └── out/LoadProjectPort.java
│   │   ├── application/ProjectService.java
│   │   └── adapter/
│   │       ├── in/web/
│   │       │   ├── ProjectController.java
│   │       │   ├── dto/{ProjectSummary,ProjectDetail}Response.java
│   │       │   └── mapper/ProjectWebMapper.java
│   │       └── out/persistence/
│   │           ├── entity/{Project,Technology}Entity.java
│   │           ├── repository/ProjectJpaRepository.java
│   │           ├── ProjectPersistenceAdapter.java
│   │           └── mapper/ProjectPersistenceMapper.java
│   │
│   ├── github/
│   │   ├── domain/
│   │   │   ├── model/{GitHubStats,LanguageUsage,RepositorySummary}.java
│   │   │   └── port/
│   │   │       ├── in/GetGitHubStatsUseCase.java
│   │   │       └── out/GitHubStatsProviderPort.java
│   │   ├── application/GitHubStatsService.java
│   │   └── adapter/
│   │       ├── in/web/GitHubController.java
│   │       ├── in/scheduler/GitHubCacheWarmer.java
│   │       └── out/github/
│   │           ├── GitHubApiAdapter.java       # @CircuitBreaker @Retry
│   │           ├── dto/GitHubRepoDto.java
│   │           └── mapper/GitHubMapper.java
│   │
│   ├── contact/
│   │   ├── domain/
│   │   │   ├── model/{ContactMessage,EmailStatus}.java
│   │   │   ├── event/ContactMessageReceivedEvent.java
│   │   │   └── port/
│   │   │       ├── in/SubmitContactMessageUseCase.java
│   │   │       └── out/{SaveContactMessagePort,SendEmailPort}.java
│   │   ├── application/ContactService.java
│   │   └── adapter/
│   │       ├── in/web/{ContactController.java,dto/ContactRequest.java}
│   │       ├── in/scheduler/FailedEmailRetryJob.java
│   │       └── out/
│   │           ├── persistence/ContactPersistenceAdapter.java
│   │           └── email/{ResendEmailAdapter,ContactEmailListener}.java
│   │
│   └── observability/
│       ├── health/GitHubHealthIndicator.java
│       └── metrics/PortfolioMetrics.java
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-{local,test,prod}.yml
│   ├── logback-spring.xml
│   ├── db/migration/
│   │   ├── V1__create_profile_tables.sql
│   │   ├── V2__create_experience_table.sql
│   │   ├── V3__create_skill_tables.sql
│   │   ├── V4__create_project_tables.sql
│   │   ├── V5__create_contact_message_table.sql
│   │   └── V6__seed_initial_content.sql
│   └── templates/email/contact-notification.html
│
├── src/test/java/dev/crystofer/portfolio/
│   ├── architecture/{HexagonalArchitectureTest,ModuleBoundaryTest}.java
│   ├── profile/{domain,application,adapter}/...
│   ├── projects/...
│   ├── github/adapter/out/GitHubApiAdapterTest.java     # WireMock
│   ├── contact/{domain,application}/...
│   ├── integration/
│   │   ├── AbstractIntegrationTest.java                 # Testcontainers
│   │   ├── ProfileIntegrationTest.java
│   │   ├── ProjectIntegrationTest.java
│   │   └── ContactIntegrationTest.java
│   └── support/{PostgresTestContainer.java,fixtures/}
│
├── Dockerfile
├── pom.xml
└── README.md
```

**Por que módulo primeiro, camada depois.** A alternativa comum seria organizar por camada na raiz (`controller/`, `service/`, `repository/`). O problema: para entender a funcionalidade de contato, é preciso abrir três pastas distantes — e nada impede que qualquer serviço chame qualquer repositório. Organizando por módulo: tudo sobre "contato" está em um lugar só; a fronteira é fisicamente visível; extrair um módulo é mover uma pasta; e o ArchUnit consegue expressar a regra de forma simples.

### 5.4 `packages/ui` e `packages/api-client`

```
packages/ui/src/
├── components/
│   ├── button.tsx      badge.tsx     card.tsx      input.tsx
│   ├── textarea.tsx    label.tsx     separator.tsx avatar.tsx
│   ├── tooltip.tsx     dialog.tsx    sheet.tsx     tabs.tsx
│   ├── skeleton.tsx    progress.tsx  toast.tsx     form-field.tsx
│   └── visually-hidden.tsx
├── lib/{cn.ts,variants.ts}
├── styles/tokens.css
└── index.ts            # Barrel export

packages/api-client/
├── src/
│   ├── generated/      # gerado do openapi.json — NÃO editar, está no .gitignore
│   ├── client.ts       # Wrapper tipado com retry e timeout
│   └── index.ts
└── scripts/generate.ts
```

### 5.5 Convenções de nomenclatura

| Contexto | Convenção | Exemplo |
|----------|-----------|---------|
| Componentes React | PascalCase | `ProjectCard.tsx` |
| Hooks | kebab-case, prefixo `use-` | `use-media-query.ts` |
| Utilitários TS | kebab-case | `filter-projects.ts` |
| Tipos TS | PascalCase | `type ProjectSummary` |
| Classes Java | PascalCase | `ProjectService.java` |
| Pacotes Java | lowercase | `dev.crystofer.portfolio.projects` |
| Tabelas | snake_case, singular | `contact_message` |
| Migrations | `V{n}__descricao_snake_case.sql` | `V5__create_contact_message_table.sql` |
| Endpoints | kebab-case, plural | `/api/v1/projects` |
| Variáveis de ambiente | SCREAMING_SNAKE_CASE | `GITHUB_TOKEN` |
| Branches | `tipo/descricao-curta` | `feat/contact-form` |

---

## 6. Funcionalidades

> Cada funcionalidade traz descrição, valor, requisitos técnicos e critérios de aceite. Funcionalidade sem critério de aceite não é requisito — é desejo.

| # | Funcionalidade | Prioridade | MVP | Backend? |
|---|---------------|-----------|-----|----------|
| F01 | Página inicial (Hero) | P0 | 1 | Sim |
| F02 | Tema claro/escuro | P1 | 1 | Não |
| F03 | Sobre + Timeline profissional | P0 | 2 | Sim |
| F04 | Download de currículo | P1 | 2 | Não |
| F05 | Skills por categoria | P0 | 2 | Sim |
| F06 | Catálogo de projetos | P0 | 3 | Sim |
| F07 | Detalhe de projeto | P0 | 3 | Sim |
| F08 | Integração com o GitHub | P1 | 4 | Sim |
| F09 | Formulário de contato | P0 | 5 | Sim |
| F10 | SEO técnico | P0 | 6 | Não |
| F11 | Internacionalização | P2 | 6 | Não |
| F12 | Analytics e Web Vitals | P1 | 6 | Não |
| F13 | Animações e microinterações | P2 | 1–6 | Não |
| F14 | Documentação da API (OpenAPI) | P1 | 1 | Sim |
| F15 | Observabilidade e health checks | P1 | 1 | Sim |

`P0` = bloqueia o lançamento · `P1` = importante · `P2` = desejável

### F01 — Página inicial (Hero)

**Descrição.** Ponto de entrada. Estrutura em página única com âncoras: Hero → Sobre → Skills → Projetos → GitHub → Contato. Páginas dedicadas existem para conteúdo aprofundado e para SEO.

**Valor.** É onde 90% dos visitantes decidem, em 5 segundos, se continuam. O hero precisa responder "quem é" e "o que faz" sem rolagem.

**Requisitos.** SSG com ISR (1h); dados de `/profile`; o LCP é o título (não uma imagem), com fonte pré-carregada; espaço reservado para todo conteúdo assíncrono; navegação com destaque da seção ativa via `IntersectionObserver`.

**Critérios de aceite**

- [ ] Nome, título profissional e proposta de valor visíveis sem rolagem em 360×640
- [ ] CTAs "Ver projetos" e "Falar comigo" acima da dobra
- [ ] LCP < 1.8s em 4G simulado
- [ ] CLS = 0
- [ ] Conteúdo legível com JavaScript desabilitado

### F02 — Tema claro/escuro

**Descrição.** Alternância entre temas, com respeito à preferência do sistema e persistência da escolha.

**Requisitos.** Tokens CSS trocados por atributo no `<html>`; script inline **antes** da hidratação (evita FOUC); `next-themes` com `disableTransitionOnChange`; escolha em `localStorage`; `<meta name="theme-color">` sincronizado.

**Critérios de aceite**

- [ ] Zero flash de tema incorreto no primeiro carregamento
- [ ] Preferência do sistema respeitada na primeira visita
- [ ] Escolha manual persiste entre sessões
- [ ] Contraste WCAG AA atendido nos **dois** temas
- [ ] Botão com `aria-label` que descreve a ação, não o estado

### F03 — Sobre e Timeline profissional

**Descrição.** Bio em primeira pessoa e timeline vertical de experiências, com empresa, cargo, período e realizações.

**Valor.** Responde "essa pessoa tem senioridade compatível com a vaga?" — a pergunta central do recrutador. Substitui a leitura do currículo.

**Requisitos.** Dados de `/experiences` ordenados por `start_date DESC`; cargo atual com badge "Atual" e `end_date` nulo; realizações vindas de `highlights` (jsonb); timeline como `<ol>` (leitores de tela anunciam a posição); animação escalonada desativada sob `prefers-reduced-motion`.

**Critérios de aceite**

- [ ] Ordem cronológica decrescente
- [ ] Períodos formatados por locale (`MMM yyyy`)
- [ ] Experiência atual visualmente distinta
- [ ] Marcação semântica de lista ordenada
- [ ] Legível em 320px sem scroll horizontal

### F04 — Download de currículo

**Descrição.** Botão de download do CV em PDF, no hero e no rodapé.

**Valor.** Recrutadores ainda precisam do PDF para o ATS. Não oferecê-lo cria atrito real.

**Critérios de aceite**

- [ ] Download inicia sem abrir aba nova
- [ ] Nome do arquivo descritivo (`crystofer-demetino-backend-2026.pdf`)
- [ ] Link anuncia formato e tamanho ("PDF, 240 KB")
- [ ] Evento registrado no analytics

### F05 — Skills por categoria

**Descrição.** Competências agrupadas em Linguagens, Frameworks, Bancos de Dados, DevOps & Cloud, Testes e Ferramentas, com nível de proficiência e anos de experiência.

**Valor.** É a seção que o recrutador escaneia buscando as palavras-chave da vaga.

**Requisitos.** Dados de `/skills` **já agrupados pela API** (agrupamento é regra de negócio, não de apresentação); proficiência como enum; representação visual **acompanhada de texto** — cor sozinha não comunica nível (WCAG 1.4.1); ícones via sprite SVG local.

**Decisão de produto:** sem barras de percentual ("React 85%"). São arbitrárias, indefensáveis em entrevista e caíram em descrédito. Usa-se rótulo textual + anos de experiência.

**Critérios de aceite**

- [ ] Skills agrupadas com cabeçalho de categoria
- [ ] Nível comunicado por texto, não apenas por cor
- [ ] Grid responsivo: 1 → 2 → 3 colunas
- [ ] Contraste ≥ 4.5:1 em todos os rótulos

### F06 — Catálogo de projetos

**Descrição.** Grade de cards com capa, título, resumo, stack e links. Filtro por tecnologia.

**Requisitos.** Dados de `/projects` (SSG); filtro **no cliente** sobre dados pré-carregados (não gera requisição); estado do filtro na URL (`?tech=spring-boot`) para ser compartilhável e indexável; `next/image` com AVIF/WebP e `placeholder="blur"`; card inteiro clicável com **uma única** área de foco.

**Critérios de aceite**

- [ ] Filtro reflete na URL e sobrevive a recarregamento
- [ ] Estado vazio tratado ("Nenhum projeto com essa tecnologia")
- [ ] Cards com altura consistente, independentemente do texto
- [ ] Foco por teclado percorre os cards em ordem lógica
- [ ] Imagens com `width`/`height` — zero CLS

### F07 — Detalhe de projeto

**Descrição.** Página por projeto (`/projetos/[slug]`) com narrativa **Problema → Solução → Resultado**, stack, métricas, capturas e links.

**Valor.** É onde a senioridade aparece. "Fiz uma API REST" é júnior; "reduzi o p95 de 800ms para 80ms introduzindo cache em duas camadas, medido com k6" é pleno/sênior. A estrutura da página **força** a segunda forma.

**Requisitos.** `generateStaticParams` a partir dos slugs; `generateMetadata` por projeto; OG image dinâmica em `opengraph-image.tsx`; JSON-LD `SoftwareSourceCode`; métricas de `project_metric` em destaque.

**Critérios de aceite**

- [ ] Todas as páginas pré-renderizadas no build
- [ ] Metadata única por projeto
- [ ] Preview de link correto no WhatsApp, LinkedIn e X
- [ ] Slug inexistente → 404 personalizada
- [ ] Breadcrumb com JSON-LD

### F08 — Integração com o GitHub

**Descrição.** Estatísticas reais: repositórios públicos, distribuição de linguagens, contribuições no último ano, repositórios em destaque.

**Valor.** Duplo. Para o visitante, prova de atividade contínua. Para o avaliador técnico, **a implementação é a demonstração** — cache, circuit breaker, retry e fallback visíveis no código.

**Requisitos.** Chamada feita **pela API Java**, nunca pelo navegador; cache Caffeine 6h; Resilience4j completo; fallback em cadeia; `@Scheduled` de reaquecimento; estado do circuito em `/actuator/prometheus`; gráfico em SVG puro (economia de ~45 KB).

**Critérios de aceite**

- [ ] O site renderiza normalmente com a API do GitHub fora do ar
- [ ] Nenhum token exposto no cliente (verificável no bundle)
- [ ] Circuit breaker abre e fecha corretamente (teste com WireMock)
- [ ] Cache hit ratio observável nas métricas
- [ ] Gráfico com alternativa textual acessível

### F09 — Formulário de contato

**Descrição.** Nome, e-mail, assunto e mensagem. Persiste no banco e notifica por e-mail.

**Valor.** É a conversão. Todo o resto do site existe para levar até aqui. Mensagem perdida é oportunidade perdida.

**Requisitos.** Validação em três camadas (cliente/Zod, BFF/Zod, API/Bean Validation); antispam em camadas (honeypot + Turnstile + tamanho + tempo mínimo); rate limit 5/hora por IP com 429 e `Retry-After`; **persistência antes do envio**; e-mail por evento `AFTER_COMMIT` assíncrono; `@Scheduled` reprocessa `FAILED`; IP armazenado como hash (LGPD); estados de UI explícitos; aviso de privacidade.

**Critérios de aceite**

- [ ] Mensagem persiste mesmo se o provedor de e-mail falhar
- [ ] Erros associados aos campos via `aria-describedby`
- [ ] Botão desabilitado durante envio, com estado anunciado
- [ ] Sucesso anunciado em região `aria-live`
- [ ] Rate limit retorna 429 com mensagem clara e `Retry-After`
- [ ] Funciona sem JavaScript (Server Action com fallback nativo)
- [ ] Teste de integração cobrindo caminho feliz e rate limit

### F10 — SEO técnico

Detalhado na [seção 11](#11-estratégia-de-seo).

**Critérios de aceite**

- [ ] Lighthouse SEO = 100
- [ ] Rich Results Test sem erros
- [ ] Sitemap acessível e válido
- [ ] Toda página com `title` e `description` únicos

### F11 — Internacionalização

**Descrição.** Site em português e inglês, com rotas localizadas (`/pt-BR/projetos`, `/en-US/projects`).

**Valor.** Amplia o alcance para vagas internacionais e remotas — e demonstra domínio de i18n com SEO correto, que é um problema técnico real.

**Critérios de aceite**

- [ ] Troca de idioma preserva a página atual
- [ ] `hreflang` correto e recíproco
- [ ] Datas e números formatados por locale
- [ ] Nenhuma string hardcoded nos componentes

### F12 — Analytics e Web Vitals

**Descrição.** Vercel Analytics + Speed Insights + Sentry. Eventos: download de CV, envio de contato, clique em repositório, uso de filtro.

**Sem cookies de rastreamento** — consequência: nenhum banner de consentimento, o que é bom para UX, performance e LGPD.

**Critérios de aceite**

- [ ] Nenhum cookie de terceiro definido
- [ ] Analytics não impacta o Lighthouse (carregamento diferido)
- [ ] Eventos principais registrados corretamente

### F13 — Animações e microinterações

**Descrição.** Entradas em fade + slide, hover em cards, transições de estado, gradiente sutil no hero. Referência de polimento: Linear, Vercel, Raycast.

**Requisitos.** Apenas `transform` e `opacity`; Framer Motion via `dynamic()`; `prefers-reduced-motion` global; nada acima da dobra que atrase o LCP; máximo de 400ms.

**Critérios de aceite**

- [ ] Nenhuma animação em propriedades que causam layout
- [ ] Com `prefers-reduced-motion: reduce`, conteúdo aparece sem movimento
- [ ] Nenhum ganho de INP acima de 50ms atribuível a animação
- [ ] Framer Motion ausente do bundle inicial

### F14 — Documentação da API

**Critérios de aceite**

- [ ] Todos os endpoints documentados com exemplos
- [ ] Respostas de erro documentadas (RFC 9457)
- [ ] Cliente TS gerado no build; divergência quebra o build
- [ ] Swagger UI acessível e linkado no README

### F15 — Observabilidade e health checks

**Critérios de aceite**

- [ ] `/actuator/health` reflete o estado real das dependências
- [ ] Métricas de latência, erro e cache expostas
- [ ] Logs em JSON com `correlationId` rastreável do BFF até o banco
- [ ] Endpoints sensíveis do Actuator protegidos

---

## 7. Design System

> **Referências:** Linear, Vercel, Stripe, Raycast, Apple. O que essas interfaces têm em comum não é decoração — é **restrição**. Poucas cores, uma família tipográfica, escala de espaçamento rígida, movimento discreto, hierarquia por contraste e espaço.
>
> **Todos os valores de contraste deste documento foram calculados**, não estimados, com a fórmula de luminância relativa da WCAG.

### 7.1 Princípios de UX/UI

| # | Princípio | Consequência prática |
|---|-----------|----------------------|
| 1 | **Conteúdo acima de cromo** | Nenhum elemento decorativo compete com texto |
| 2 | **Hierarquia por espaço, não por linha** | Separadores são a última opção |
| 3 | **Uma cor de destaque, com parcimônia** | O roxo aparece em CTAs, links e foco. Se estivesse em tudo, não destacaria nada |
| 4 | **Movimento com propósito** | Animação comunica origem, destino ou mudança de estado |
| 5 | **Densidade calibrada** | Confortável no mobile, informativa no desktop |
| 6 | **Estados sempre visíveis** | Todo interativo tem hover, focus-visible, active e disabled |
| 7 | **Acessível por construção** | O contraste é validado no token, não corrigido depois |
| 8 | **Dark-first** | O tema escuro é o padrão; o claro é igualmente completo, não uma inversão automática |

### 7.2 Paleta de cores

**Escolha do destaque: violeta/índigo (`#5B4BD6` / `#8B7CF6`).** É a família cromática do território "ferramenta de desenvolvedor" (Linear, Raycast), evita o azul genérico de portfólio corporativo, e — o ponto decisivo — atinge contraste AA tanto como texto sobre fundo escuro quanto como fundo de botão com texto branco. Nem toda cor bonita passa nos dois testes.

#### Tema escuro (padrão)

| Token | Valor | Uso | Contraste medido |
|-------|-------|-----|------------------|
| `--bg` | `#08080A` | Fundo da página | — |
| `--surface` | `#0F0F12` | Cards, painéis | — |
| `--surface-2` | `#16161A` | Elementos elevados, inputs | — |
| `--border` | `#232329` | Divisórias decorativas | 1.28:1 (decorativo) |
| `--border-interactive` | `#6A6A74` | Bordas de input e botão | **3.74:1** ✅ (WCAG 1.4.11) |
| `--fg` | `#FAFAFA` | Texto principal | **19.17:1** ✅ AAA |
| `--fg-muted` | `#A1A1AA` | Texto secundário | **7.81:1** ✅ AAA |
| `--fg-subtle` | `#8A8A94` | Legendas, metadados | **5.85:1** ✅ AA |
| `--accent` | `#8B7CF6` | Links, ícones, foco | **6.01:1** ✅ AA |
| `--accent-solid` | `#5B4BD6` | Fundo de botão primário | branco sobre ele: **6.14:1** ✅ |
| `--success` | `#3DD68C` | Sucesso | **10.67:1** ✅ AAA |
| `--warning` | `#FFB224` | Avisos | **11.10:1** ✅ AAA |
| `--danger` | `#FF6369` | Erros | **6.90:1** ✅ AA |

#### Tema claro

| Token | Valor | Uso | Contraste medido |
|-------|-------|-----|------------------|
| `--bg` | `#FFFFFF` | Fundo da página | — |
| `--surface` | `#FAFAFA` | Cards, painéis | — |
| `--surface-2` | `#F4F4F5` | Elementos elevados, inputs | — |
| `--border` | `#E4E4E7` | Divisórias decorativas | 1.27:1 (decorativo) |
| `--border-interactive` | `#82828B` | Bordas de input e botão | **3.81:1** ✅ |
| `--fg` | `#09090B` | Texto principal | **19.90:1** ✅ AAA |
| `--fg-muted` | `#52525B` | Texto secundário | **7.73:1** ✅ AAA |
| `--fg-subtle` | `#71717A` | Legendas, metadados | **4.83:1** ✅ AA |
| `--accent` | `#5B4BD6` | Links, ícones, foco | **6.14:1** ✅ AA |
| `--accent-solid` | `#5B4BD6` | Fundo de botão primário | branco sobre ele: **6.14:1** ✅ |
| `--success` | `#0E7247` | Sucesso | **5.97:1** ✅ AA |
| `--warning` | `#8F5100` | Avisos | **6.28:1** ✅ AA |
| `--danger` | `#C42A2F` | Erros | **5.65:1** ✅ AA |

> Nota sobre o `--border` (1.27:1): bordas puramente decorativas **não** estão sujeitas ao critério 1.4.11 da WCAG. Bordas que comunicam a fronteira de um controle interativo estão — por isso existe o token separado `--border-interactive`, que passa em 3:1.

```css
@theme {
  --color-bg:                 #08080A;
  --color-surface:            #0F0F12;
  --color-surface-2:          #16161A;
  --color-border:             #232329;
  --color-border-interactive: #6A6A74;
  --color-fg:                 #FAFAFA;
  --color-fg-muted:           #A1A1AA;
  --color-fg-subtle:          #8A8A94;
  --color-accent:             #8B7CF6;
  --color-accent-solid:       #5B4BD6;
  --color-success:            #3DD68C;
  --color-warning:            #FFB224;
  --color-danger:             #FF6369;
}

[data-theme='light'] {
  --color-bg:                 #FFFFFF;
  --color-surface:            #FAFAFA;
  --color-surface-2:          #F4F4F5;
  --color-border:             #E4E4E7;
  --color-border-interactive: #82828B;
  --color-fg:                 #09090B;
  --color-fg-muted:           #52525B;
  --color-fg-subtle:          #71717A;
  --color-accent:             #5B4BD6;
  --color-accent-solid:       #5B4BD6;
  --color-success:            #0E7247;
  --color-warning:            #8F5100;
  --color-danger:             #C42A2F;
}
```

**Regra de uso:** nenhum componente escreve um valor hexadecimal. Cor só entra por token — reforçado por lint.

### 7.3 Tipografia

| Papel | Fonte | Justificativa |
|-------|-------|---------------|
| Interface e texto | **Geist Sans** (variável) | Desenhada para interfaces; ótima legibilidade em corpo pequeno; fonte variável = um arquivo para todos os pesos |
| Código e dados | **JetBrains Mono** | Monoespaçada legível; usada em snippets, versões, métricas e rótulos técnicos |

Carregamento com `next/font/local`, `display: swap`, subset latino, `preload` apenas do peso do LCP. Zero requisição a domínio de terceiro — bom para performance **e** privacidade.

**Escala** (razão 1.25, fluida com `clamp()`):

| Token | Mobile → Desktop | Peso | Line-height | Tracking | Uso |
|-------|------------------|------|-------------|----------|-----|
| `display` | `clamp(2.5rem, 6vw, 4.5rem)` | 600 | 1.05 | -0.03em | Título do hero |
| `h1` | `clamp(2rem, 4vw, 3rem)` | 600 | 1.15 | -0.02em | Título de página |
| `h2` | `clamp(1.5rem, 3vw, 2.25rem)` | 600 | 1.2 | -0.02em | Título de seção |
| `h3` | `1.5rem` | 600 | 1.3 | -0.01em | Título de card |
| `h4` | `1.25rem` | 500 | 1.4 | 0 | Subtítulo |
| `body-lg` | `1.125rem` | 400 | 1.65 | 0 | Texto de destaque |
| `body` | `1rem` | 400 | 1.65 | 0 | Texto padrão |
| `body-sm` | `0.875rem` | 400 | 1.6 | 0 | Texto secundário |
| `caption` | `0.75rem` | 500 | 1.5 | 0.02em | Metadados, badges |
| `mono` | `0.875rem` | 400 | 1.6 | 0 | Código, versões |

**Decisões:** tracking negativo em títulos (ajuste ótico que Linear e Stripe usam); line-height 1.65 no corpo (acima do padrão 1.5, atende a recomendação WCAG 1.4.12); máximo de 68 caracteres por linha; nunca abaixo de 0.75rem.

### 7.4 Espaçamento e layout

Base de **4px**, escala geométrica. Nenhum valor arbitrário entra no código.

| Token | px | rem | Uso típico |
|-------|-----|-----|------------|
| `space-1` | 4 | 0.25 | Gap ícone/texto |
| `space-2` | 8 | 0.5 | Padding interno pequeno |
| `space-3` | 12 | 0.75 | Gap entre relacionados |
| `space-4` | 16 | 1 | Padding padrão |
| `space-6` | 24 | 1.5 | Padding de card |
| `space-8` | 32 | 2 | Gap entre grupos |
| `space-12` | 48 | 3 | Separação de blocos |
| `space-16` | 64 | 4 | Padding de seção (mobile) |
| `space-24` | 96 | 6 | Padding de seção (desktop) |
| `space-32` | 128 | 8 | Respiro entre grandes seções |

**Ritmo vertical:** `py-16` mobile → `py-24` em `md` → `py-32` em `lg`. Consistência aqui é o que faz o site parecer profissionalmente desenhado — mais do que qualquer detalhe visual isolado.

| Item | Valor |
|------|-------|
| Container | `max-width: 1200px`, `padding-inline: 1rem / 1.5rem / 2rem` |
| Grid | 12 colunas, `gap: 1.5rem` |
| Breakpoints | `sm: 640` · `md: 768` · `lg: 1024` · `xl: 1280` · `2xl: 1536` |
| Largura de leitura | `68ch` |

### 7.5 Raio, elevação e foco

| Token | Valor | Uso |
|-------|-------|-----|
| `radius-sm` | 6px | Badges, inputs pequenos |
| `radius-md` | 8px | Botões, inputs |
| `radius-lg` | 12px | Cards |
| `radius-xl` | 16px | Painéis, modais |
| `radius-full` | 9999px | Avatares, pills |

**Elevação.** No tema escuro, profundidade vem de **luminosidade de superfície e borda** — sombra preta sobre fundo preto é invisível. No claro, sombras discretas:

```css
--shadow-sm: 0 1px 2px rgb(0 0 0 / 0.04);
--shadow-md: 0 4px 12px rgb(0 0 0 / 0.06);
--shadow-lg: 0 12px 32px rgb(0 0 0 / 0.08);
```

**Anel de foco (não negociável):**

```css
--focus-ring: 0 0 0 2px var(--color-bg), 0 0 0 4px var(--color-accent);
```

Anel duplo garante visibilidade sobre qualquer superfície. Aplicado via `:focus-visible`, nunca removido com `outline: none` sem substituto.

### 7.6 Movimento

| Token | Valor | Uso |
|-------|-------|-----|
| `duration-fast` | 120ms | Hover, mudança de cor |
| `duration-base` | 180ms | Transições padrão |
| `duration-slow` | 280ms | Entrada de elemento |
| `duration-slower` | 400ms | Entrada de seção |
| `ease-out` | `cubic-bezier(0.16, 1, 0.3, 1)` | Padrão — entrada rápida, chegada suave |
| `ease-in-out` | `cubic-bezier(0.65, 0, 0.35, 1)` | Transições bidirecionais |
| `stagger` | 60ms | Atraso entre itens de lista |

**Regras:** anima-se apenas `transform` e `opacity` (qualquer outra propriedade força layout/paint e ameaça o INP); nada acima de 400ms; `prefers-reduced-motion: reduce` desativa translação e escala.

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
```

### 7.7 Ícones e estilo visual

**Ícones:** `lucide-react` (traço 1.5px, tree-shakeable). **Logos de tecnologia:** Simple Icons extraídos em build para sprite SVG local — nada de CDN de terceiro. Tamanhos 16/20/24px; ícone decorativo com `aria-hidden`; ícone que **é** a ação recebe `aria-label` e alvo ≥ 44×44px.

**Direção visual:** minimalismo técnico. Fundo quase preto, tipografia densa, superfícies sutilmente elevadas, uma cor de destaque, movimento discreto.

| Elemento assinatura | Descrição | Custo |
|----------|-----------|-------|
| Grid de pontos no hero | `radial-gradient` com máscara de desvanecimento | 0 KB |
| Glow radial de destaque | `radial-gradient` de baixa opacidade | 0 KB |
| Borda com gradiente em cards | `border` transparente + `background-image` no hover | ~200 bytes |
| Cursor de terminal no headline | Animação CSS de opacidade | 0 KB |
| Números monoespaçados | `font-variant-numeric: tabular-nums` | 0 KB |

**Deliberadamente ausentes:** carrossel, parallax, vídeo de fundo, partículas, cursor customizado, splash screen. Todos custam performance e nenhum comunica competência técnica — vários comunicam o oposto.

### 7.8 Governança dos tokens

1. Novo token exige justificativa no PR.
2. Valor hexadecimal fora do arquivo de tokens é erro de lint.
3. Espaçamento fora da escala (`mt-[13px]`) é erro de lint.
4. Alteração de token é *breaking change* e exige revisão visual em todas as páginas.

Essa disciplina é o mesmo raciocínio de fronteira arquitetural, aplicado à camada visual.

---

## 8. Componentização

> Regra estruturante: **um primitivo não conhece o domínio**. `Card` não sabe o que é um projeto. Quando um componente do design system recebe uma prop chamada `project`, ele deixou de ser primitivo — e a fronteira foi violada.

### 8.1 Níveis

```
Nível 4 — Páginas          app/[locale]/**/page.tsx
                            ↑ compõem
Nível 3 — Seções           features/*/components/*Section.tsx
                            ↑ compõem
Nível 2 — Domínio          ProjectCard, SkillCard, Timeline, ContactForm
                            ↑ compõem
Nível 1 — Aplicação        Navbar, Footer, Container, Section, SectionHeading
                            ↑ compõem
Nível 0 — Primitivos       @portfolio/ui: Button, Card, Badge, Input...
```

### 8.2 Nível 0 — Primitivos (`@portfolio/ui`)

| Componente | Responsabilidade | API principal |
|-----------|------------------|---------------|
| `Button` | Ação clicável com variantes e loading | `variant` (primary, secondary, ghost, outline, link) · `size` · `loading` · `asChild` |
| `Badge` | Rótulo curto não interativo | `variant` · `size` |
| `Card` | Superfície contentora composta | `Card`, `CardHeader`, `CardTitle`, `CardDescription`, `CardContent`, `CardFooter` |
| `Input` | Campo de uma linha com estado de erro | `error` · `aria-invalid` · `aria-describedby` |
| `Textarea` | Campo multilinha | `error` · `maxLength` · `autoResize` |
| `Label` | Rótulo associado a controle | `htmlFor` · `required` |
| `FormField` | Composição rótulo + controle + erro + descrição | Encapsula toda a ligação `aria-*` |
| `Separator` | Divisória semântica ou decorativa | `orientation` · `decorative` |
| `Avatar` | Imagem com fallback de iniciais | `src` · `alt` · `fallback` |
| `Tooltip` | Dica em hover/focus, acessível por teclado | `content` · `side` · `delayDuration` |
| `Dialog` | Modal com focus trap e `Esc` | Radix Dialog estilizado |
| `Sheet` | Painel lateral (menu mobile) | `side` |
| `Tabs` | Painéis navegáveis por setas | Radix Tabs estilizado |
| `Skeleton` | Placeholder com dimensão fixa | `className` para as medidas |
| `Progress` | `role="progressbar"` | `value` · `max` · `aria-label` |
| `Toast` | Notificação em região `aria-live` | `title` · `description` · `variant` |
| `VisuallyHidden` | Texto só para leitores de tela | — |

**Contrato dos primitivos**

1. Encaminham `ref` (`forwardRef`) — sem isso, Radix e libs de formulário quebram.
2. Repassam props do elemento nativo (`...props`).
3. Aceitam `className`, mesclado com `cn()` (`clsx` + `tailwind-merge`).
4. **Não têm margem própria** — espaçamento é responsabilidade de quem compõe. Essa regra sozinha elimina a maior fonte de inconsistência de layout.
5. Não fazem `fetch`, não leem contexto de domínio.

### 8.3 Nível 1 — Componentes de aplicação

| Componente | Responsabilidade | Notas técnicas |
|-----------|------------------|----------------|
| `Navbar` | Navegação, seção ativa, logo, tema, idioma | Client; `IntersectionObserver`; blur ao rolar; `<nav aria-label="Principal">` |
| `MobileNav` | Menu em `Sheet` | Focus trap, fecha ao navegar, `Esc` fecha |
| `Footer` | Links secundários, sociais, copyright | Server Component |
| `Container` | Largura máxima e padding lateral | Sem estilo além do layout |
| `Section` | Wrapper semântico com ritmo vertical | `id` para âncora, `aria-labelledby` |
| `SectionHeading` | Eyebrow + título + descrição | Garante um único `<h2>` por seção |
| `SkipLink` | "Pular para o conteúdo" | Primeiro elemento focável do DOM |
| `ThemeToggle` | Alterna claro/escuro | `aria-label` descreve a ação; sem flash |
| `LocaleSwitcher` | Alterna idioma preservando a rota | `hreflang` nos links |
| `SocialLinks` | Perfis externos | `rel="me noopener"`; `aria-label` por link |
| `ScrollToTop` | Volta ao topo | Acima de 600px; respeita reduced-motion |
| `JsonLd` | Injeta structured data | Server; `<script type="application/ld+json">` |
| `FadeIn` | Animação de entrada por viewport | No-op sob reduced-motion |
| `StaggerContainer` | Entrada escalonada de filhos | Delay configurável |
| `ResumeDownload` | Download do CV | Anuncia formato e tamanho; evento de analytics |

### 8.4 Nível 2 — Componentes de domínio

| Componente | Props | Notas |
|-----------|-------|-------|
| `ProjectCard` | `project: ProjectSummary` · `priority?: boolean` | Card inteiro é um link; **uma** área de foco; `priority` na imagem LCP |
| `ProjectFilter` | `technologies` · `selected` · `onChange` | Estado sincronizado com query string; `role="group"` |
| `ProjectDetail` | `project: ProjectDetail` | Estrutura fixa Problema → Solução → Resultado |
| `ProjectMetrics` | `metrics: Metric[]` | `tabular-nums`; rótulo associado ao valor |
| `TechStack` | `technologies` · `size` | `<ul>` semântica; ícones `aria-hidden` |
| `SkillCard` | `skill: Skill` | Nível em **texto**, não só cor |
| `SkillCategory` | `category` · `skills` | Grid responsivo |
| `Timeline` | `experiences: Experience[]` | `<ol>`; linha e marcadores `aria-hidden` |
| `TimelineItem` | `experience` · `isCurrent` | Badge "Atual" quando `end_date` é nulo |
| `GitHubStats` | `stats: GitHubStats` | Trata indisponibilidade sem quebrar |
| `LanguageChart` | `languages: LanguageUsage[]` | SVG puro; tabela textual acessível |
| `ContributionGraph` | `contributions` | `role="img"` com `aria-label` resumindo o dado |
| `RepositoryCard` | `repo: RepositorySummary` | Stars e linguagem; link externo sinalizado |
| `ContactForm` | — | Server Action + `useActionState`; honeypot; Turnstile |
| `ContactInfo` | `profile` | `mailto:` e cópia para a área de transferência |
| `HeroSection` | `profile` | Contém o LCP; sem animação que o atrase |
| `AvailabilityBadge` | `available: boolean` | Ponto pulsante + texto (nunca só cor) |

### 8.5 Nível 3 — Seções

Cada seção é um Server Component que busca seus próprios dados e compõe componentes de domínio:

```tsx
// features/projects/components/ProjectsSection.tsx
export async function ProjectsSection() {
  const projects = await getFeaturedProjects();  // cache por tag

  return (
    <Section id="projetos" aria-labelledby="projetos-heading">
      <Container>
        <SectionHeading
          id="projetos-heading"
          eyebrow="Portfólio"
          title="Projetos em destaque"
          description="Sistemas que construí, com o problema que resolviam."
        />
        <StaggerContainer className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {projects.map((p, i) => (
            <ProjectCard key={p.slug} project={p} priority={i < 3} />
          ))}
        </StaggerContainer>
      </Container>
    </Section>
  );
}
```

| Seção | Dados | Renderização |
|-------|-------|--------------|
| `HeroSection` | `/profile` | SSG |
| `AboutSection` | `/profile` + `/experiences` | SSG |
| `SkillsSection` | `/skills` | SSG |
| `ProjectsSection` | `/projects?featured=true` | SSG + ISR 1h |
| `GitHubSection` | `/github/stats` | ISR 6h |
| `ContactSection` | `/profile` | SSG (formulário é Client) |

### 8.6 Estados obrigatórios

Componente sem estado de erro é componente incompleto. Todo componente com dados assíncronos define quatro:

| Estado | Tratamento |
|--------|-----------|
| **Loading** | `Skeleton` com as **mesmas dimensões** do conteúdo final — é o que garante CLS = 0 |
| **Empty** | Mensagem explicativa e, quando aplicável, uma ação ("Limpar filtro") |
| **Error** | Mensagem clara + retry, sem stack trace; erro de seção não derruba a página (`error.tsx` por rota) |
| **Success** | O conteúdo |

### 8.7 Padrões e regras de qualidade

| Padrão | Onde | Por quê |
|--------|------|---------|
| **Compound Components** | `Card`, `Tabs`, `FormField` | API flexível sem explosão de props |
| **`asChild` (Slot do Radix)** | `Button` | `<Button asChild><Link/></Button>` sem `<a>` dentro de `<button>` |
| **Render props** | `ProjectFilter` | Quem consome controla a renderização |
| **CVA (variants)** | Todos os primitivos | Variantes tipadas, sem `if` de className |
| **Server-first** | Todas as seções | `"use client"` só com estado, efeito ou API de browser |
| **Barrel export** | `@portfolio/ui/index.ts` | Import limpo, mantendo tree-shaking |

**Regras:**

1. Um componente, uma responsabilidade. Se o nome precisa de "e", são dois componentes.
2. Máximo de ~150 linhas.
3. Máximo de 5 props diretas.
4. Sem prop drilling além de 2 níveis.
5. Props tipadas explicitamente; nunca `any`.
6. Sem lógica de negócio no componente — filtro, ordenação e agrupamento vivem em `lib/` ou no backend.
7. Teste de comportamento, não de implementação: `getByRole`, nunca seletor de classe.

---

## 9. Integrações

### 9.1 Mapa

| Integração | Direção | Onde vive | Criticidade | Falha aceitável? |
|-----------|---------|-----------|-------------|------------------|
| GitHub API | Saída | API Java | Média | **Sim** — degrada com fallback |
| Resend (e-mail) | Saída | API Java | Alta | Parcial — mensagem persiste e é reprocessada |
| Cloudflare Turnstile | Saída | BFF (Next) | Média | Não — bloqueia o envio |
| Vercel Analytics | Saída | Cliente | Baixa | Sim — silenciosa |
| Sentry | Saída | Cliente + API | Baixa | Sim — silenciosa |
| Sitemap / robots / manifest | Interna | Next | Alta (SEO) | Não |
| Open Graph images | Interna | Next (Edge) | Média | Sim — fallback estático |

### 9.2 GitHub API

| Endpoint | Uso | Frequência |
|----------|-----|-----------|
| `GET /users/{user}` | Nome, avatar, bio, contagem de repos | 6h |
| `GET /users/{user}/repos?sort=updated&per_page=100` | Repositórios, linguagens, stars | 6h |
| `GET /repos/{owner}/{repo}/languages` | Bytes por linguagem | 6h |
| GraphQL `contributionsCollection` | Calendário de contribuições (só existe no GraphQL) | 6h |

**Onde a chamada acontece: sempre no backend Java.** Nunca no navegador, nunca no BFF. Três motivos: o token não pode ir para o cliente; sem token são 60 req/h por IP; e cache, circuit breaker e agregação pertencem ao domínio, não à apresentação.

**Autenticação:** fine-grained PAT com escopo **somente leitura de repositórios públicos**. Nada além disso. Rotação anual documentada.

```java
@Component
class GitHubApiAdapter implements GitHubStatsProviderPort {

  @Override
  @CircuitBreaker(name = "github", fallbackMethod = "fallbackStats")
  @Retry(name = "github")
  @Bulkhead(name = "github")
  @Cacheable(cacheNames = "github-stats", key = "#username")
  public GitHubStats fetchStats(String username) { ... }

  private GitHubStats fallbackStats(String username, Throwable t) {
    log.warn("GitHub indisponível, usando fallback. causa={}", t.toString());
    return staleCache.get(username).orElse(GitHubStats.empty());
  }
}
```

```yaml
resilience4j:
  circuitbreaker.instances.github:
    slidingWindowSize: 10
    failureRateThreshold: 50
    waitDurationInOpenState: 60s
    permittedNumberOfCallsInHalfOpenState: 3
  retry.instances.github:
    maxAttempts: 3
    waitDuration: 500ms
    exponentialBackoffMultiplier: 2
    retryExceptions: [java.io.IOException, org.springframework.web.client.HttpServerErrorException]
  bulkhead.instances.github:
    maxConcurrentCalls: 5
```

**Cadeia de fallback:** cache válido → cache expirado → `GitHubStats.empty()`. Nunca uma exceção chega ao usuário.

**Testes.** WireMock simula 200 normal, 403 de rate limit, 500, timeout e resposta malformada. O teste **verifica que o circuit breaker abre** após o limiar e que o fallback é acionado — não apenas o caminho feliz.

### 9.3 Formulário de contato

```
Cliente ──(1)──▶ Turnstile (Cloudflare) ──▶ token
Cliente ──(2)──▶ POST /api/contact (Next Route Handler, mesma origem)
                     │ (3) valida com Zod
                     │ (4) verifica o token na Cloudflare (siteverify)
                     │ (5) extrai o IP real (x-forwarded-for)
                     ▼
                 POST /api/v1/contact (API Java, header X-Service-Key)
                     │ (6) ServiceKeyAuthFilter
                     │ (7) Bucket4j — 5/hora por hash de IP
                     │ (8) Bean Validation
                     │ (9) ContactMessage (validação de domínio)
                     │ (10) INSERT (email_status = PENDING)
                     │ (11) publishEvent(ContactMessageReceivedEvent)
                     ▼ 202 Accepted
     ┌──────────────────────────────────────────────────┐
     │ @TransactionalEventListener AFTER_COMMIT @Async  │
     │  → Resend API → email_status = SENT | FAILED     │
     └──────────────────────────────────────────────────┘
     ┌──────────────────────────────────────────────────┐
     │ @Scheduled(15min) → reprocessa FAILED (máx. 5x)  │
     └──────────────────────────────────────────────────┘
```

| Escolha | Alternativa comum | Por que a alternativa é pior |
|---------|-------------------|------------------------------|
| Persistir antes de enviar | Enviar direto sem guardar | Provedor fora do ar = mensagem perdida. Inaceitável quando a mensagem pode ser uma oportunidade de trabalho |
| Envio assíncrono após commit | Envio síncrono na requisição | O usuário esperaria 1–3s; e uma falha no e-mail causaria rollback de uma mensagem válida |
| 202 Accepted | 200 OK | Semanticamente correto: aceito para processamento, ainda não concluído |
| Hash do IP | IP em claro | Rate limit funciona igual, sem armazenar PII (LGPD) |
| Turnstile | reCAPTCHA | Melhor UX, sem rastreamento entre sites |

**Camadas antispam:** (1) honeypot invisível via CSS — preenchido = descarte silencioso com 202 falso; (2) Turnstile; (3) tempo mínimo de 3s de preenchimento; (4) rate limit 5/hora por IP; (5) validação de conteúdo (tamanho, e-mail RFC 5322, rejeição de mensagens com mais de 3 URLs).

**E-mail:** HTML com Thymeleaf e fallback em texto puro. Contém remetente, assunto, mensagem, timestamp e `correlationId`. O `Reply-To` é o e-mail do remetente — responder é um clique.

### 9.4 Analytics

| Ferramenta | O que mede | Custo |
|-----------|-----------|-------|
| Vercel Analytics | Pageviews, referrers, países | ~1 KB, após o `load` |
| Vercel Speed Insights | Core Web Vitals reais | ~2 KB, diferido |
| Sentry | Erros de runtime (cliente e API) | Lazy, só em produção |

| Evento | Quando dispara | Por que importa |
|--------|---------------|-----------------|
| `resume_download` | Clique no CV | Indicador forte de interesse real |
| `contact_submit` | Envio bem-sucedido | Métrica de conversão |
| `project_view` | Abertura de página de projeto | Quais projetos atraem |
| `repo_click` | Clique em link do GitHub | Interesse técnico |
| `filter_used` | Uso do filtro | Quais stacks são buscadas |

### 9.5 Sitemap, robots e OG

```ts
// app/sitemap.ts
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const projects = await getProjects();
  const base = env.NEXT_PUBLIC_SITE_URL;

  const staticRoutes = ['', '/sobre', '/projetos', '/contato'].map((r) => ({
    url: `${base}${r}`,
    lastModified: new Date(),
    changeFrequency: 'monthly' as const,
    priority: r === '' ? 1 : 0.8,
    alternates: { languages: { 'pt-BR': `${base}/pt-BR${r}`, 'en-US': `${base}/en-US${r}` } },
  }));

  const projectRoutes = projects.map((p) => ({
    url: `${base}/projetos/${p.slug}`,
    lastModified: p.updatedAt,
    changeFrequency: 'monthly' as const,
    priority: 0.7,
  }));

  return [...staticRoutes, ...projectRoutes];
}
```

**Ponto:** adicionar um projeto no banco faz a página existir, entrar no sitemap e ser indexada — sem tocar em código. É o retorno concreto da decisão de manter conteúdo em banco.

```ts
// app/robots.ts
export default function robots(): MetadataRoute.Robots {
  return {
    rules: [{ userAgent: '*', allow: '/', disallow: ['/api/', '/_next/'] }],
    sitemap: `${env.NEXT_PUBLIC_SITE_URL}/sitemap.xml`,
    host: env.NEXT_PUBLIC_SITE_URL,
  };
}
```

Crawlers de IA (GPTBot, CCBot) são permitidos deliberadamente — visibilidade em respostas de assistentes é canal legítimo de descoberta.

**Open Graph.** Imagens dinâmicas com `ImageResponse` no Edge Runtime: home (nome + título + stack), projeto (título + resumo + tecnologias), demais (template com o título). 1200×630, tipografia e cores do design system, cacheadas na CDN, com fallback estático. **Validação obrigatória** no Facebook Sharing Debugger, LinkedIn Post Inspector e X Card Validator — é a etapa que a maioria pula, e o resultado é um link compartilhado sem imagem.

**Ícones:** `icon.svg` (vetorial, suporta `prefers-color-scheme`), `favicon.ico` (32×32), `apple-icon.png` (180×180), `icon-192/512.png` para o manifest. Convenção de arquivos do App Router, sem `<link>` manual.

**Manifest:** `standalone`, `theme_color: #08080A`, ícone maskable. **Sem service worker na v1** — um site já servido por CDN com HTML estático não ganha nada mensurável com cache offline, e ganha um vetor de bugs difícil de depurar. Decisão registrada, não esquecimento.

### 9.6 Segurança das integrações

| Integração | Segredo | Onde vive | Exposto ao cliente? |
|-----------|---------|-----------|---------------------|
| GitHub | `GITHUB_TOKEN` | Env da API (Render) | Não |
| Resend | `RESEND_API_KEY` | Env da API (Render) | Não |
| Turnstile | `TURNSTILE_SECRET_KEY` | Env do web (Vercel) | Não (a site key é pública por design) |
| API interna | `SERVICE_API_KEY` | Env do web e da API | Não |
| Sentry | `SENTRY_DSN` | Público | Sim (seguro por design) |

**Regras:** nenhum segredo com prefixo `NEXT_PUBLIC_`, exceto os públicos por especificação; `.env.example` documenta todas as variáveis sem valores; validação de ambiente com Zod no boot — variável ausente derruba a aplicação **na inicialização**, não em produção às 3h da manhã; rotação documentada; secret scanning do GitHub habilitado.

---

## 10. Estratégia de Performance

> Meta: **Lighthouse ≥ 95 nas quatro categorias, em mobile**. Método: não perseguir o número — definir um orçamento, medir a cada PR e tratar estouro como falha de build. Performance só sobrevive quando é enforçada; otimizar depois é sempre mais caro do que não regredir.

### 10.1 Orçamento

| Recurso | Orçamento | Falha o CI acima de |
|---------|-----------|---------------------|
| JS inicial (comprimido) | 100 KB | 130 KB |
| CSS inicial | 20 KB | 30 KB |
| Fontes | 60 KB (2 arquivos) | 80 KB |
| Imagem do LCP | 80 KB | 120 KB |
| Peso total da página | 400 KB | 600 KB |
| Requisições no carregamento inicial | 20 | 30 |
| LCP (4G, mobile) | 1.8s | 2.5s |
| INP | 150ms | 200ms |
| CLS | 0.05 | 0.1 |
| TBT | 150ms | 300ms |
| TTFB | 400ms | 800ms |

Configurado em `lighthouserc.json` e verificado pelo Lighthouse CI em todo PR.

### 10.2 Renderização

| Rota | Estratégia | Revalidação | Justificativa |
|------|-----------|-------------|---------------|
| `/` | SSG + ISR | 1h | HTML da CDN |
| `/sobre` | SSG + ISR | 24h | Muda raramente |
| `/projetos` | SSG + ISR | 1h | Novo projeto aparece em até 1h |
| `/projetos/[slug]` | SSG (`generateStaticParams`) | Sob demanda por tag | Geradas no build |
| `/contato` | SSG (shell) + Client (form) | — | Só o formulário é interativo |
| `/api/*` | Dinâmica | — | BFF, por natureza |

```ts
const res = await fetch(`${API_URL}/api/v1/projects`, {
  headers: { 'X-Service-Key': env.SERVICE_API_KEY },
  next: { tags: ['projects'], revalidate: 3600 },
});
```

**Consequência sistêmica:** como o HTML é estático, o pico de tráfego é absorvido pela CDN e **a API praticamente não é chamada em runtime**. A decisão de renderização é, na prática, uma decisão de custo de infraestrutura.

### 10.3 JavaScript

**Server Components como padrão.** A regra mais impactante do projeto: tudo é Server Component até que se prove o contrário.

| Componente | Tipo | Motivo |
|-----------|------|--------|
| Todas as seções | Server | Só renderizam dados |
| `ProjectCard`, `Timeline` | Server | Sem interação |
| `Navbar` | Client | Scroll e seção ativa |
| `ThemeToggle` | Client | `localStorage` |
| `ContactForm`, `ProjectFilter` | Client | Estado |
| Wrappers de motion | Client | Animação |

```ts
const MotionProvider = dynamic(() => import('@/components/motion/MotionProvider'), { ssr: false });
const ContributionGraph = dynamic(() => import('@/features/github/components/ContributionGraph'), {
  loading: () => <Skeleton className="h-32 w-full" />,
});
```

**Eliminações deliberadas:**

| O que não entra | Peso evitado | Substituto |
|-----------------|-------------|------------|
| Biblioteca de charts | ~45 KB | SVG escrito à mão |
| Biblioteca de datas | ~20 KB | `Intl.DateTimeFormat` nativo |
| Gerenciador de estado | ~10 KB | Server Components + `useState` |
| Cliente HTTP (axios) | ~14 KB | `fetch` nativo |
| Biblioteca de ícones completa | ~50 KB | `lucide-react` com tree-shaking |
| jQuery / lodash | ~70 KB | JavaScript moderno |

Total evitado: aproximadamente **200 KB** — o dobro do orçamento inteiro de JS. Cada linha desta tabela é uma decisão explícita, e todas juntas são o que torna o alvo alcançável.

`@next/bundle-analyzer` roda no CI e comenta o delta no PR. Aumento acima de 10 KB exige justificativa escrita.

### 10.4 Imagens e fontes

| Técnica | Implementação |
|---------|---------------|
| Formato moderno | `next/image` serve AVIF, fallback WebP |
| Responsivo | `sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"` |
| Lazy loading | Padrão; `priority` só na imagem do LCP |
| Placeholder | `blur` com `blurDataURL` gerado no build |
| Sem CLS | `width` e `height` obrigatórios; `aspect-ratio` no CSS |
| Compressão | `sharp` no build; qualidade 80 (~40% menor, imperceptível) |
| Ícones | Sprite SVG único, inline no HTML |

```ts
export const geist = localFont({
  src: './fonts/Geist-Variable.woff2',
  variable: '--font-sans',
  display: 'swap',
  preload: true,
  fallback: ['system-ui', 'sans-serif'],
  adjustFontFallback: 'Arial',
});
```

| Técnica | Ganho |
|---------|-------|
| Fonte variável | Um arquivo em vez de 4–6 pesos |
| `woff2` com subset latino | ~25 KB por família em vez de ~120 KB |
| `display: swap` | Texto visível imediatamente, sem FOIT |
| `adjustFontFallback` | Métricas do fallback ajustadas → **CLS zero** na troca |
| Self-hosted | Sem DNS lookup nem conexão externa |

O `adjustFontFallback` é o detalhe que separa CLS 0.15 de CLS 0. Sem ele, a troca da fonte de sistema para a final desloca o texto.

### 10.5 CSS e cache

- **Tailwind 4** gera apenas as classes usadas — ~15 KB comprimido.
- **CSS crítico inline** automaticamente pelo Next.
- **Zero CSS-in-JS** — nenhum custo de runtime, compatível com RSC.
- **`content-visibility: auto`** em seções abaixo da dobra.
- **Nenhum `@import`** em CSS (bloqueia renderização em cascata).

| Camada | Recurso | Política |
|--------|---------|----------|
| CDN | Assets estáticos | `public, max-age=31536000, immutable` |
| CDN | HTML (ISR) | `s-maxage=3600, stale-while-revalidate=86400` |
| Navegador | Fontes | `public, max-age=31536000, immutable` |
| Next | `fetch` à API | `next: { tags, revalidate }` |
| API | Endpoints de leitura | `ETag` + `public, max-age=300, stale-while-revalidate=3600` |
| API | Cache de aplicação | Caffeine, 10 min (conteúdo) / 6h (GitHub) |
| API | Conexões | HikariCP: pool máximo 10, mínimo ocioso 2 |

`stale-while-revalidate` é o ponto central: o usuário recebe a versão em cache instantaneamente enquanto a atualização acontece em background. Ninguém espera por revalidação.

### 10.6 Performance do backend

| Técnica | Implementação | Ganho esperado |
|---------|--------------|----------------|
| Virtual Threads | `spring.threads.virtual.enabled=true` | Chamadas bloqueantes deixam de reter threads de plataforma |
| Cache de segundo nível | Caffeine em `@Cacheable` | p95 de ~40ms → ~2ms em cache quente |
| Prevenção de N+1 | `@EntityGraph` em projeto↔tecnologia | 1 query em vez de N+1 |
| Projeções | Interfaces de projeção do Spring Data | Menos dados trafegados |
| Índices | `slug`, `featured`, `display_order`, `created_at` | Scans sequenciais eliminados |
| Pool de conexões | HikariCP dimensionado | Sem exaustão sob carga |
| Compressão | `server.compression.enabled=true`, mínimo 1 KB | ~70% menos bytes em JSON |
| Startup | `spring.jmx.enabled=false`, lazy init seletiva | Cold start menor no free tier |

**Alvos:** p95 < 80ms com cache quente; p95 < 250ms com cache frio.

### 10.7 Medição

| Ferramenta | Quando | O que mede |
|-----------|--------|-----------|
| Lighthouse CI | Todo PR | Laboratório, mobile e desktop |
| Vercel Speed Insights | Contínuo | Web Vitals reais (RUM) |
| `@next/bundle-analyzer` | Todo build | Composição e delta do bundle |
| k6 | Antes do lançamento e a cada release | Carga na API |
| Actuator + Prometheus | Contínuo | Latência, erro, cache hit ratio |
| WebPageTest | Marcos | Waterfall detalhado, dispositivo real |

**Laboratório × campo.** Lighthouse roda em condições sintéticas; RUM mostra a experiência real. Divergência é sinal — normalmente de que o dispositivo real é mais lento. A decisão é sempre tomada com base no dado de campo.

---

## 11. Estratégia de SEO

> Objetivo realista: rankear em **primeiro lugar para o nome próprio + termos profissionais**, e aparecer em buscas de cauda longa por tecnologias e projetos. Não se disputa "desenvolvedor backend" genérico — não é a intenção nem seria alcançável.

### 11.1 Palavras-chave alvo

| Tipo | Exemplos | Prioridade |
|------|----------|-----------|
| Marca pessoal | "Crystofer Demetino", "Crystofer desenvolvedor" | P0 — precisa ser #1 |
| Profissional | "Crystofer Demetino backend", "Crystofer Java Spring" | P0 |
| Cauda longa técnica | "portfólio Spring Boot arquitetura hexagonal", "exemplo Testcontainers Spring Boot" | P1 |
| Projeto específico | Nome de cada projeto | P1 |

Buscas de marca são as que importam: um recrutador que recebeu o currículo vai pesquisar o nome. O portfólio precisa ser o primeiro resultado — antes de perfis de rede social.

### 11.2 Metadata

```ts
export const metadata: Metadata = {
  metadataBase: new URL(env.NEXT_PUBLIC_SITE_URL),
  title: {
    default: 'Crystofer Demetino — Desenvolvedor Backend | Java & Spring Boot',
    template: '%s | Crystofer Demetino',
  },
  description:
    'Desenvolvedor Backend especializado em Java, Spring Boot e arquitetura de sistemas. '
    + 'Portfólio com projetos reais, arquitetura hexagonal e código aberto.',
  keywords: ['desenvolvedor backend', 'java', 'spring boot', 'arquitetura de software', 'api rest'],
  authors: [{ name: 'Crystofer Demetino', url: env.NEXT_PUBLIC_SITE_URL }],
  creator: 'Crystofer Demetino',
  openGraph: {
    type: 'website',
    locale: 'pt_BR',
    alternateLocale: ['en_US'],
    url: env.NEXT_PUBLIC_SITE_URL,
    siteName: 'Crystofer Demetino',
    images: [{ url: '/opengraph-image', width: 1200, height: 630 }],
  },
  twitter: { card: 'summary_large_image' },
  robots: {
    index: true,
    follow: true,
    googleBot: { index: true, follow: true, 'max-image-preview': 'large', 'max-snippet': -1 },
  },
  alternates: {
    canonical: '/',
    languages: { 'pt-BR': '/pt-BR', 'en-US': '/en-US' },
  },
};
```

```ts
export async function generateMetadata({ params }): Promise<Metadata> {
  const project = await getProjectBySlug(params.slug);
  if (!project) return {};

  return {
    title: project.title,
    description: project.summary,
    alternates: { canonical: `/projetos/${project.slug}` },
    openGraph: {
      type: 'article',
      title: project.title,
      description: project.summary,
      publishedTime: project.publishedAt,
      images: [{ url: `/projetos/${project.slug}/opengraph-image` }],
    },
  };
}
```

| Elemento | Limite | Regra |
|----------|--------|-------|
| `title` | 50–60 caracteres | Único por página; palavra-chave à esquerda |
| `description` | 140–160 caracteres | Descreve o conteúdo real e convida ao clique; sem keyword stuffing |
| `h1` | 1 por página | Coerente com o `title`, sem ser idêntico |

### 11.3 Structured Data (JSON-LD)

```json
{
  "@context": "https://schema.org",
  "@type": "Person",
  "name": "Crystofer Demetino",
  "jobTitle": "Desenvolvedor Backend",
  "description": "Desenvolvedor Backend especializado em Java e Spring Boot.",
  "url": "https://crystofer.dev",
  "image": "https://crystofer.dev/avatar.jpg",
  "email": "mailto:crystoferdemetino@gmail.com",
  "sameAs": ["https://github.com/crystofer", "https://linkedin.com/in/crystofer"],
  "knowsAbout": ["Java", "Spring Boot", "PostgreSQL", "Docker", "Arquitetura de Software"]
}
```

`sameAs` é o campo que consolida a identidade entre GitHub, LinkedIn e o site — é o que alimenta o painel de conhecimento e o que faz o Google entender que os três perfis são a mesma pessoa.

Também implementados: `WebSite` (habilita a caixa de busca do site), `BreadcrumbList` (páginas internas) e `SoftwareSourceCode` (cada projeto, com `programmingLanguage` e `codeRepository`).

**Validação:** Rich Results Test e Schema Markup Validator, com zero erros, no checklist de lançamento.

### 11.4 URLs e estrutura semântica

| Princípio | Aplicação |
|-----------|-----------|
| Legível por humanos | `/projetos/api-gestao-pedidos`, não `/p?id=42` |
| Curta e hierárquica | Máximo de 3 níveis |
| kebab-case | Padrão da web |
| Sem stop words | `/projetos/api-pedidos` |
| Estável | Slug imutável após publicação; mudança exige redirect 301 |
| Localizada | `/pt-BR/projetos` · `/en-US/projects` |
| Sem barra final | Redirect 308 |

O slug é tratado como **contrato público**. Link quebrado custa autoridade acumulada.

```html
<body>
  <a href="#main" class="skip-link">Pular para o conteúdo</a>
  <header><nav aria-label="Principal"> … </nav></header>
  <main id="main">
    <h1>Crystofer Demetino — Desenvolvedor Backend</h1>
    <section aria-labelledby="sobre-heading">
      <h2 id="sobre-heading">Sobre</h2>
    </section>
    <section aria-labelledby="projetos-heading">
      <h2 id="projetos-heading">Projetos</h2>
      <article><h3>Nome do Projeto</h3></article>
    </section>
  </main>
  <footer> … </footer>
</body>
```

Um `<h1>` por página; hierarquia sem saltos; landmarks corretos. Semântica correta serve simultaneamente ao SEO e à acessibilidade — não são duas tarefas, é uma.

### 11.5 i18n e SEO

```html
<link rel="alternate" hreflang="pt-BR" href="https://crystofer.dev/pt-BR/projetos" />
<link rel="alternate" hreflang="en-US" href="https://crystofer.dev/en-US/projects" />
<link rel="alternate" hreflang="x-default" href="https://crystofer.dev/pt-BR/projetos" />
```

`hreflang` recíproco (se A aponta para B, B aponta para A), `x-default` definido, cada idioma com URL própria (nunca conteúdo trocado via JavaScript), `canonical` de cada versão apontando para si mesma.

### 11.6 Indexação

**No lançamento:** verificar propriedade no Search Console → submeter sitemap → solicitar indexação das páginas principais → verificar no Bing Webmaster Tools → adicionar o link nos perfis do GitHub e LinkedIn (backlinks de alta autoridade e relevância) → conferir a renderização com o teste de URL ao vivo.

**Contínuo:** cobertura de indexação, consultas de entrada, CTR médio e Core Web Vitals, revisados mensalmente.

Core Web Vitals são sinal de ranqueamento confirmado — toda a [seção 10](#10-estratégia-de-performance) é, portanto, também estratégia de SEO.

---

## 12. Estratégia de Acessibilidade

> **Meta:** conformidade WCAG 2.2 nível AA.
> **Posição adotada:** acessibilidade não é um requisito adicionado ao final — é uma propriedade do HTML correto. A maior parte do trabalho consiste em *não* fazer coisas erradas: não usar `<div>` como botão, não remover o outline de foco, não comunicar informação apenas por cor.
>
> Um portfólio de backend que se preocupa com a11y comunica algo específico: **rigor que não se limita à especialidade declarada**.

### 12.1 Critérios WCAG 2.2 AA — cobertura

#### Perceptível

| Critério | Nível | Como é atendido |
|----------|-------|-----------------|
| 1.1.1 Conteúdo não textual | A | `alt` descritivo em imagens informativas; `alt=""` + `aria-hidden` em decorativas |
| 1.3.1 Informação e relações | A | HTML semântico; `<ol>` na timeline; `<fieldset>`/`<legend>` no formulário |
| 1.3.2 Sequência com significado | A | Ordem do DOM = ordem visual |
| 1.3.4 Orientação | AA | Retrato e paisagem, sem bloqueio |
| 1.3.5 Identificar propósito de entrada | AA | `autocomplete="name"`, `"email"` |
| 1.4.1 Uso de cor | A | Nível de skill e disponibilidade sempre com texto além da cor |
| 1.4.3 Contraste mínimo | AA | Todos os tokens medidos — ver [seção 7.2](#72-paleta-de-cores) |
| 1.4.4 Redimensionar texto | AA | Zoom até 200% sem perda |
| 1.4.10 Reflow | AA | Sem scroll horizontal em 320px |
| 1.4.11 Contraste de não texto | AA | `--border-interactive` com ≥3:1; anel de foco ≥3:1 |
| 1.4.12 Espaçamento de texto | AA | Layout íntegro com o bookmarklet de espaçamento |
| 1.4.13 Conteúdo em hover/focus | AA | Tooltips dispensáveis com `Esc`, persistentes ao passar o mouse |

#### Operável

| Critério | Nível | Como é atendido |
|----------|-------|-----------------|
| 2.1.1 Teclado | A | Toda funcionalidade acessível por teclado |
| 2.1.2 Sem armadilha de foco | A | Modal e sheet com focus trap correto e saída por `Esc` |
| 2.1.4 Atalhos de caractere único | A | Não existem |
| 2.4.1 Pular blocos | A | Skip link como primeiro elemento focável |
| 2.4.2 Página com título | A | `title` único por página |
| 2.4.3 Ordem de foco | A | Ordem lógica; nenhum `tabindex` positivo |
| 2.4.4 Finalidade do link | A | Texto descritivo; nunca "clique aqui" |
| 2.4.6 Cabeçalhos e rótulos | AA | Descritivos e hierárquicos |
| 2.4.7 Foco visível | AA | Anel duplo em `:focus-visible`, jamais removido |
| **2.4.11 Foco não obscurecido** | AA (novo em 2.2) | Navbar fixa com `scroll-margin-top` para não cobrir o elemento focado |
| 2.5.3 Rótulo no nome | A | Rótulo visível contido no nome acessível |
| 2.5.4 Atuação por movimento | A | Nada depende de movimento do dispositivo |
| **2.5.7 Movimentos de arrastar** | AA (novo em 2.2) | Nenhuma funcionalidade exige arrastar |
| **2.5.8 Tamanho do alvo** | AA (novo em 2.2) | ≥ 24×24 CSS px exigido; adotado 44×44 como padrão interno |

#### Compreensível e Robusto

| Critério | Nível | Como é atendido |
|----------|-------|-----------------|
| 3.1.1 Idioma da página | A | `<html lang="pt-BR">`, sincronizado com o locale |
| 3.1.2 Idioma de partes | AA | `lang="en"` em trechos em inglês |
| 3.2.1 Em foco | A | Foco não dispara mudança de contexto |
| 3.2.2 Em entrada | A | Sem submit automático |
| 3.2.3 Navegação consistente | AA | Navbar e footer idênticos em todas as páginas |
| 3.2.4 Identificação consistente | AA | Mesmo ícone = mesma ação |
| **3.2.6 Ajuda consistente** | A (novo em 2.2) | Link de contato na mesma posição em todas as páginas |
| 3.3.1 Identificação de erro | A | Erro em texto, associado ao campo |
| 3.3.2 Rótulos ou instruções | A | Todos os campos rotulados; obrigatoriedade explícita |
| 3.3.3 Sugestão de erro | AA | "Informe um e-mail válido, como nome@dominio.com" |
| **3.3.7 Entrada redundante** | A (novo em 2.2) | Nenhuma informação é pedida duas vezes |
| **3.3.8 Autenticação acessível** | AA (novo em 2.2) | Turnstile não exige charada cognitiva |
| 4.1.2 Nome, função, valor | A | Elementos nativos sempre que possível; ARIA só quando necessário |
| 4.1.3 Mensagens de status | AA | `aria-live="polite"` em sucesso de formulário e resultado de filtro |

### 12.2 Navegação por teclado

| Tecla | Comportamento esperado |
|-------|----------------------|
| `Tab` / `Shift+Tab` | Avança/retrocede em ordem lógica |
| `Enter` | Ativa links e botões |
| `Space` | Ativa botões; rola quando não há foco em controle |
| `Esc` | Fecha modal, sheet e tooltip |
| `←` `→` | Navega entre abas |
| `Home` / `End` | Primeiro/último item em listas navegáveis |

**Roteiro de teste manual (a cada MVP):**

1. Carregar a home e pressionar `Tab` — o primeiro foco deve ser o skip link.
2. Percorrer toda a página só com teclado. Cada elemento focado deve ser **visível** — inclusive sob a navbar fixa.
3. Abrir o menu mobile: o foco entra e não escapa; `Esc` fecha e devolve o foco ao botão.
4. Preencher e enviar o formulário sem tocar no mouse.
5. Verificar se nenhum elemento fica focável estando invisível (armadilha comum em menus off-screen).

### 12.3 Leitores de tela

| Leitor | Navegador | Plataforma | Frequência |
|--------|-----------|-----------|-----------|
| NVDA | Firefox | Windows | A cada MVP |
| VoiceOver | Safari | macOS/iOS | Antes do lançamento |
| TalkBack | Chrome | Android | Antes do lançamento |

| Elemento | Anúncio esperado |
|----------|-----------------|
| Botão de tema | "Ativar tema claro, botão" — anuncia a **ação**, não o estado |
| Card de projeto | "Nome do Projeto, link" — uma única parada de foco |
| Timeline | "Lista com 5 itens. Item 1 de 5…" |
| Erro de campo | "E-mail, edição, inválido. Informe um e-mail válido." |
| Envio bem-sucedido | Região live anuncia "Mensagem enviada com sucesso." |
| Gráfico de linguagens | "Distribuição de linguagens: Java 45%, TypeScript 30%…" |
| Ícone decorativo | Silêncio |

### 12.4 Formulário acessível

```tsx
<FormField>
  <Label htmlFor="email">
    E-mail <span aria-hidden="true">*</span>
    <VisuallyHidden>(obrigatório)</VisuallyHidden>
  </Label>
  <Input
    id="email"
    name="email"
    type="email"
    autoComplete="email"
    required
    aria-invalid={!!errors.email}
    aria-describedby={errors.email ? 'email-error' : 'email-hint'}
  />
  <p id="email-hint" className="text-sm text-fg-subtle">
    Usarei este endereço apenas para responder.
  </p>
  {errors.email && (
    <p id="email-error" role="alert" className="text-sm text-danger">
      {errors.email}
    </p>
  )}
</FormField>
```

Detalhes que importam: o asterisco é `aria-hidden` e a obrigatoriedade é anunciada por texto; `aria-describedby` alterna entre dica e erro (nunca acumula); `role="alert"` anuncia imediatamente; `autoComplete` ativa o preenchimento automático, que é recurso de acessibilidade motora.

### 12.5 Movimento

```tsx
export function FadeIn({ children }: PropsWithChildren) {
  const reduced = useReducedMotion();
  if (reduced) return <>{children}</>;
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: '-80px' }}
      transition={{ duration: 0.28, ease: [0.16, 1, 0.3, 1] }}
    >
      {children}
    </motion.div>
  );
}
```

`prefers-reduced-motion` tratado globalmente **e** por componente; nada pisca mais de 3× por segundo (WCAG 2.3.1); nenhum autoplay; `scroll-behavior: smooth` desativado sob reduced-motion.

### 12.6 Testes automatizados

```ts
// e2e/accessibility.spec.ts
import AxeBuilder from '@axe-core/playwright';

const routes = ['/', '/sobre', '/projetos', '/projetos/exemplo', '/contato'];

for (const route of routes) {
  test(`sem violações de acessibilidade em ${route}`, async ({ page }) => {
    await page.goto(route);
    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'])
      .analyze();
    expect(results.violations).toEqual([]);
  });
}
```

| Ferramenta | Onde roda | Cobertura estimada |
|-----------|-----------|-------------------|
| `eslint-plugin-jsx-a11y` | Editor + CI | Erros de marcação em tempo de escrita |
| `@axe-core/playwright` | CI, todas as rotas | ~57% dos problemas detectáveis |
| Lighthouse a11y | CI | Score ≥ 95 |
| `jest-axe` (Vitest) | Testes de componente | Componentes isolados |

**Advertência honesta:** ferramentas automatizadas detectam cerca de metade dos problemas reais. Elas não avaliam se o texto alternativo é *bom*, se a ordem de foco é *lógica* ou se o anúncio faz *sentido*. Por isso o teste manual das seções 12.2 e 12.3 é obrigatório a cada MVP, e não substituível pelo CI.

### 12.7 Checklist por componente

Todo PR de componente novo precisa marcar todos:

- [ ] Funciona apenas com teclado
- [ ] Foco visível e não obscurecido
- [ ] Nome acessível correto
- [ ] Função (`role`) correta — preferencialmente implícita, via elemento nativo
- [ ] Estado comunicado (`aria-expanded`, `aria-selected`, `aria-invalid`)
- [ ] Contraste ≥ 4.5:1 (texto) e ≥ 3:1 (UI)
- [ ] Alvo de toque ≥ 44×44px
- [ ] Nenhuma informação transmitida só por cor
- [ ] Respeita `prefers-reduced-motion`
- [ ] Testado com leitor de tela
- [ ] Sem violações do axe

### 12.8 Declaração de acessibilidade

O site terá uma página `/acessibilidade` declarando o nível de conformidade alcançado, as limitações conhecidas, os métodos de teste e um canal para relatar problemas. Publicar limitações conhecidas não é admitir fracasso — é a prática recomendada, e demonstra que a conformidade foi **verificada**, não presumida.

---

## 13. Padrões de Código

> Convenção que depende de disciplina humana é convenção que se perde. Neste projeto, **toda regra que pode ser automatizada é automatizada** — lint, formatação, testes de arquitetura, validação de mensagem de commit. O que sobra para o julgamento humano é o que realmente exige julgamento.

### 13.1 Clean Code

| Princípio | Regra concreta | Verificação |
|-----------|---------------|-------------|
| Nomes revelam intenção | `getActiveProjectsOrderedByDate()`, não `getData()` | Revisão de PR |
| Sem abreviação | `repository`, não `repo` | Revisão de PR |
| Funções pequenas | Máximo de ~20 linhas | SonarCloud (complexidade cognitiva ≤ 15) |
| Um nível de abstração por função | Não misturar orquestração com detalhe | Revisão de PR |
| Poucos parâmetros | Máximo de 3; acima disso, objeto | Revisão de PR |
| Sem flag booleana de parâmetro | `findAll()` e `findPublished()`, não `find(boolean)` | Revisão de PR |
| Comentário explica *por quê* | O código explica o quê | Revisão de PR |
| Sem número mágico | Constante nomeada ou `@ConfigurationProperties` | ESLint / SonarCloud |
| Retorno antecipado | Guard clauses em vez de `if` aninhado | Revisão de PR |
| Sem código morto | Nada comentado "para depois" — o Git é o histórico | SonarCloud |

```java
// BOM: a API do GitHub retorna 403 (não 429) quando o rate limit é excedido.
// Por isso 403 é tratado como erro retentável aqui.

// RUIM: incrementa o contador
counter++;
```

### 13.2 SOLID

**Single Responsibility.** `ContactService` orquestra o recebimento de mensagem; **não** formata e-mail, **não** valida captcha, **não** aplica rate limit.

**Open/Closed.** Novo provedor de e-mail = nova implementação de `SendEmailPort`. Nenhuma classe existente é modificada.

```java
public interface SendEmailPort { void send(EmailMessage message); }
// ResendEmailAdapter · SmtpEmailAdapter · NoopEmailAdapter (perfil de teste)
```

**Liskov.** Qualquer implementação de `LoadProjectPort` é substituível sem quebrar o caso de uso. Nenhuma lança exceção não prevista nem enfraquece a pós-condição — verificado por testes de contrato compartilhados.

**Interface Segregation.** Portas pequenas: `LoadProjectPort` e `SaveProjectPort`, não um `ProjectRepository` com 15 métodos.

**Dependency Inversion.** `ProjectService` depende de `LoadProjectPort` (interface do domínio), não de `ProjectJpaRepository`. A seta aponta para dentro — verificado por ArchUnit, não confiado à disciplina.

### 13.3 DRY, KISS e YAGNI

DRY se aplica a **conhecimento**, não a texto parecido:

| Situação | Ação correta |
|----------|-------------|
| Regra de negócio duplicada | Extrair — é duplicação real |
| Dois DTOs com campos parecidos por coincidência | **Não** unir — vão divergir, e a união acopla contextos |
| Mesmo cálculo repetido | Extrair para o domínio |
| Estrutura de teste repetida | Extrair fixtures/builders, mantendo cada teste legível |

Abstração prematura por "parecerem iguais" produz acoplamento acidental — mais caro que a duplicação que tentava evitar. Regra prática: espere a terceira ocorrência.

| Solução complexa evitada | Solução adotada | Justificativa |
|--------------------------|-----------------|---------------|
| Redis distribuído | Caffeine local | Uma instância; sem problema distribuído |
| Event sourcing | CRUD com auditoria simples | Sem requisito de reconstrução de histórico |
| CQRS com bancos separados | Um modelo, com projeções | Volume não justifica |
| GraphQL | REST | Um cliente, contrato estável |
| Microsserviços | Monólito modular | Um desenvolvedor, um domínio |
| Kubernetes | Docker em PaaS | Custo e complexidade operacional |
| Service worker / PWA offline | Nada | HTML já vem da CDN |

**Nota:** a arquitetura hexagonal parece contradizer KISS — e a tensão é real. A justificativa está no [ADR-0003](#adr-0003-arquitetura-hexagonal-com-monólito-modular). Identificar quando se está deliberadamente violando um princípio, e por quê, é diferente de violá-lo por descuido.

### 13.4 Separation of Concerns

| Camada | Responsabilidade | O que **nunca** faz |
|--------|------------------|---------------------|
| `adapter/in/web` | Traduzir HTTP ↔ caso de uso | Regra de negócio, acesso a banco |
| `application` | Orquestrar, transacionar | Detalhe de HTTP, SQL |
| `domain` | Regra e invariante de negócio | Conhecer framework, HTTP ou banco |
| `adapter/out/persistence` | Traduzir domínio ↔ tabela | Regra de negócio |
| Server Component | Buscar e renderizar | Estado de UI, `useEffect` |
| Client Component | Interação | Chamada direta à API interna |
| `lib/` | Utilitário puro | Conhecer componente |

### 13.5 Tipagem forte

```jsonc
// tsconfig.json
{
  "strict": true,
  "noUncheckedIndexedAccess": true,
  "exactOptionalPropertyTypes": true,
  "noImplicitOverride": true,
  "noFallthroughCasesInSwitch": true,
  "verbatimModuleSyntax": true
}
```

- `any` é proibido (erro de lint). Use `unknown` e estreite.
- Asserção de tipo (`as`) exige comentário justificando.
- Tipos da API são **gerados** do OpenAPI — nunca escritos à mão.
- Zod valida o dado em runtime na fronteira; o tipo é inferido do schema.
- Union discriminada para estados: `{ status: 'idle' } | { status: 'loading' } | { status: 'error'; error: string }`.

No Java: **records** para DTOs e value objects; **value objects** em vez de primitivos (`Slug`, `EmailAddress` — validação na construção, e o compilador impede trocar um pelo outro); **`Optional`** em retorno de porta, nunca como parâmetro ou campo; **sealed interfaces** com `switch` exaustivo; **`@NonNullApi`** no `package-info.java`; **enums** em vez de `String` para conjuntos fechados.

```java
public record ContactRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 150) String subject,
    @NotBlank @Size(min = 20, max = 2000) String message
) {}
```

### 13.6 Testes

```java
@Test
@DisplayName("deve rejeitar mensagem quando o limite por IP for excedido")
void shouldRejectMessage_whenRateLimitExceeded() {
    // given
    var request = ContactFixtures.valid();
    given(rateLimiter.tryConsume(anyString())).willReturn(false);

    // when
    var thrown = catchThrowable(() -> service.submit(request));

    // then
    assertThat(thrown)
        .isInstanceOf(RateLimitExceededException.class)
        .hasMessageContaining("limite");
    then(repository).shouldHaveNoInteractions();
}
```

```
        ╱ E2E ╲            ~10 testes   Playwright — jornadas críticas
      ╱─────────╲
    ╱ Integração ╲         ~25 testes   Testcontainers, WireMock
  ╱───────────────╲
╱    Unitários     ╲       ~120 testes  Domínio e casos de uso
```

**Regras:** um comportamento por teste (o nome descreve o comportamento, não o método); Given/When/Then explícito; sem lógica no teste (nada de `if`/`for` decidindo asserções); testes independentes e em qualquer ordem; builders para fixtures; teste do comportamento observável, não da implementação. **Cobertura é diagnóstico, não meta** — 80% em `domain` e `application` é o limiar; teste escrito para inflar cobertura é ruído.

### 13.7 Testes de arquitetura (ArchUnit)

```java
@AnalyzeClasses(packages = "dev.crystofer.portfolio")
class HexagonalArchitectureTest {

  @ArchTest
  static final ArchRule dominio_nao_conhece_framework =
      noClasses().that().resideInAPackage("..domain..")
          .should().dependOnClassesThat()
          .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "..adapter..");

  @ArchTest
  static final ArchRule application_nao_conhece_web =
      noClasses().that().resideInAPackage("..application..")
          .should().dependOnClassesThat().resideInAPackage("..adapter.in.web..");

  @ArchTest
  static final ArchRule modulos_sem_ciclo =
      slices().matching("dev.crystofer.portfolio.(*)..").should().beFreeOfCycles();

  @ArchTest
  static final ArchRule controllers_terminam_com_controller =
      classes().that().areAnnotatedWith(RestController.class)
          .should().haveSimpleNameEndingWith("Controller");

  @ArchTest
  static final ArchRule sem_field_injection =
      noFields().should().beAnnotatedWith(Autowired.class)
          .because("injeção por construtor torna a dependência explícita e testável");
}
```

É o diferencial mais barato de implementar e o mais eloquente para um avaliador: a arquitetura deixa de ser intenção documentada e passa a ser **propriedade verificada do build**. No frontend, o equivalente é `eslint-plugin-boundaries`.

### 13.8 Ferramentas de qualidade

```js
// eslint.config.mjs
export default [
  js.configs.recommended,
  ...tseslint.configs.strictTypeChecked,
  ...tseslint.configs.stylisticTypeChecked,
  jsxA11y.flatConfigs.strict,
  {
    plugins: { boundaries },
    rules: {
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/consistent-type-imports': 'error',
      'boundaries/element-types': ['error', { default: 'disallow', rules: [/* ... */] }],
      'no-console': ['error', { allow: ['warn', 'error'] }],
    },
  },
];
```

```json
// .lintstagedrc.json
{
  "*.{ts,tsx}": ["eslint --fix", "prettier --write"],
  "*.{json,md,css}": ["prettier --write"],
  "*.java": ["mvn -q spotless:apply -DspotlessFiles="]
}
```

Hooks: `pre-commit` (lint-staged), `commit-msg` (commitlint), `pre-push` (testes unitários). No Java, Spotless com Google Java Format, verificado no CI.

### 13.9 Conventional Commits

```
<tipo>(<escopo>): <descrição no imperativo>

[corpo opcional]

[rodapé opcional]
```

| Tipo | Uso | Versão |
|------|-----|--------|
| `feat` | Nova funcionalidade | minor |
| `fix` | Correção de bug | patch |
| `refactor` | Mudança sem alterar comportamento | — |
| `perf` | Melhoria de performance | patch |
| `test` | Testes | — |
| `docs` | Documentação | — |
| `style` | Formatação | — |
| `build` | Build ou dependências | — |
| `ci` | Pipeline | — |
| `chore` | Manutenção | — |

**Escopos:** `web`, `api`, `ui`, `db`, `infra`, `docs`, `deps`.

**Regras:** descrição no imperativo ("adiciona", não "adicionado"), até 72 caracteres, sem ponto final, minúscula inicial. O corpo explica **por quê**, não o quê — o diff já mostra o quê.

```
feat(api): adiciona circuit breaker na integração com o GitHub

A API do GitHub retorna 403 quando o rate limit é excedido e ocasionalmente
apresenta latência elevada. Sem proteção, uma falha lá degradava toda a
resposta de /github/stats.

Configuração: abre com 50% de falha em 10 chamadas, half-open após 60s,
com fallback para o último valor em cache.

Refs: #23
```

Validado por commitlint no hook `commit-msg`. Commit fora do padrão **não entra no repositório**.

### 13.10 Fluxo de trabalho no Git

| Item | Convenção |
|------|-----------|
| Branch principal | `main`, protegida |
| Branches de trabalho | `feat/nome-curto`, `fix/nome-curto`, `docs/nome-curto` |
| Merge | Squash merge, título no padrão Conventional Commits |
| Proteção da `main` | CI verde obrigatório; sem force push |
| PR | Template com objetivo, mudanças, como testar e checklist |
| Tamanho do PR | Máximo de ~400 linhas alteradas |

**Nota sobre este projeto:** por ser de um único desenvolvedor, commits diretos na `main` seguindo o cronograma da [Parte II](#16-os-7-mvps-e-seus-54-commits) são aceitáveis. PRs são usados nos MVPs maiores — o objetivo é que o histórico demonstre familiaridade com o fluxo, não que o processo vire cerimônia vazia.

### 13.11 Definition of Done (por tarefa)

- [ ] Código escrito e revisado (autorrevisão do diff completo, no mínimo)
- [ ] Testes escritos e passando
- [ ] Lint e formatação sem erro
- [ ] Sem regressão de performance (bundle e Lighthouse)
- [ ] Acessibilidade verificada (teclado + axe)
- [ ] Responsividade verificada em 320px, 768px e 1440px
- [ ] Documentação atualizada, se necessário
- [ ] Commit no padrão Conventional Commits
- [ ] CI verde
- [ ] Funciona no preview deploy

---

## 14. Decisões Arquiteturais (ADRs)

> **Por que existem.** Uma decisão sem contexto registrado vira, meses depois, "por que isso está assim?". Pior: vira uma decisão revertida por alguém que não conhecia a razão original.
>
> **Para um portfólio, há um segundo motivo:** em entrevista técnica, a pergunta não é "o que você usou?", é "por que você usou?". Estes registros são a resposta, escrita antes da pergunta.
>
> **Regra:** ADR é imutável. Mudou de ideia? Escreve-se um novo que substitui o anterior. O histórico da decisão é tão valioso quanto a decisão.

### ADR-0001: Monorepo full-stack com API real

**Status:** Aceito

**Contexto.** O objetivo é demonstrar competência como Desenvolvedor Backend. Existe uma contradição no formato tradicional de portfólio: ele é um site — artefato de frontend — que apenas *afirma* competência backend. Ao mesmo tempo, o portfólio precisa ser bonito, rápido e bem posicionado em buscadores.

**Decisão.** Monorepo full-stack: `apps/web` (Next.js — apresentação, SEO, BFF), `apps/api` (Java + Spring — domínio, persistência, integrações), `packages/*` (design system, cliente tipado, configs). A API roda **em produção**, e todo o conteúdo do site vem dela.

**Alternativas descartadas**

- *Site estático com repositórios separados* — exige que o avaliador saia do portfólio e navegue por outros repositórios; passo que a maioria não dará em uma triagem de 5 minutos.
- *Frontend com BFF, sem backend Java* — Route Handlers não demonstram Spring, JPA, testes de integração, migrações nem arquitetura hexagonal.
- *Dois repositórios separados* — fragmenta a avaliação; o monorepo entrega tudo em um link.
- *Backend em Node/NestJS* — a especialidade a demonstrar é Java/Spring, que é o que o mercado-alvo contrata.

**Consequências positivas.** Competência verificável, não declarada; um único link para compartilhar; contrato tipado validado em build-time; refatoração atômica de contrato; CI unificado com cache; demonstra capacidade poliglota.

**Consequências negativas (aceitas).** Setup mais complexo (duas toolchains) — mitigado por Docker Compose e jobs de CI separados; mais superfície de manutenção — mitigado por Dependabot agrupado; dois provedores de hospedagem — free tiers cobrem; build mais longo — cache do Turborepo; mais um ponto de falha em runtime — mitigado pela renderização estática (ADR-0007).

### ADR-0002: Java 21 e Spring Boot 3.4

**Status:** **Substituído pelo [ADR-0009](#adr-0009-spring-boot-35-substitui-o-adr-0002)** quanto à versão do Spring Boot. Todo o restante — Java 21, Maven e os recursos de linguagem — continua aceito e em vigor.

> O texto abaixo é o original e não foi alterado: a seção 14 determina que ADR é imutável, e mudar o registro apagaria justamente o histórico que ele existe para preservar. Só a linha de status muda, que é o mecanismo padrão de substituição.

**Contexto.** O backend precisa maximizar empregabilidade no mercado-alvo, demonstrar conhecimento atual do ecossistema e caber em um free tier com pouca memória e cold start.

**Decisão.** Java 21 (LTS) com Spring Boot 3.4 e Maven. Recursos explorados deliberadamente: Records, sealed interfaces + pattern matching, Virtual Threads, text blocks.

**Alternativas descartadas**

- *Java 17* — sem Virtual Threads estáveis nem pattern matching completo.
- *Quarkus* — melhor startup e footprint, mas demanda de mercado significativamente menor; para um projeto cujo objetivo é empregabilidade, essa é a variável decisiva.
- *Kotlin + Spring* — demonstraria competência adjacente à que se quer provar.
- *Gradle* — Kotlin DSL adiciona uma terceira linguagem à leitura do repositório.

**Consequências positivas.** Alinhamento direto com o mercado; ecossistema Spring maduro; Virtual Threads tornam desnecessária a programação reativa aqui; Records eliminam boilerplate; LTS até 2031.

**Consequências negativas (aceitas).** Consumo de memória de 180–350 MB — mitigado por `-XX:MaxRAMPercentage=75`, JMX desabilitado e lazy init; cold start de 20–50s no free tier — mitigado porque o site é estático e o usuário nunca espera a API; mais verboso que Kotlin — verbosidade explícita é legível, e legibilidade é o objetivo em um repositório que será avaliado por leitura.

### ADR-0003: Arquitetura hexagonal com monólito modular

**Status:** Aceito

**Contexto.** O domínio real é modesto: quatro contextos, leitura predominante, escrita quase inexistente. Um MVC em camadas resolveria o problema funcional em uma fração do esforço. **A tensão precisa ser nomeada honestamente:** a arquitetura escolhida não é justificada pela complexidade do domínio — é justificada pelo **objetivo do artefato**.

**Decisão.** Hexagonal (Ports & Adapters), organizada em monólito modular por contexto de negócio. Módulos não se importam entre si; comunicação por eventos. Regras verificadas por ArchUnit — violação quebra o build.

**Alternativas descartadas**

- *MVC em camadas* — a interface do repositório normalmente estende `JpaRepository`, fazendo o núcleo do negócio depender do Spring Data; e não comunica intenção arquitetural (é o que qualquer tutorial produz).
- *Clean Architecture completa* — mais cerimônia de nomenclatura, sem ganho neste tamanho de domínio.
- *Vertical Slice* — excelente para CRUD, mas menos reconhecível no mercado brasileiro; parte do valor da decisão é ser reconhecida imediatamente.
- *Microsserviços* — um desenvolvedor, um domínio, tráfego baixo. Seria erro de julgamento, e um avaliador sênior o identificaria como imaturidade, não sofisticação.

**Consequências positivas.** Domínio testável sem subir o Spring; troca de infraestrutura isolada; fronteiras prontas para extração futura; **arquitetura verificada, não prometida**; comunica intenção pela estrutura de pastas; prepara respostas concretas de entrevista.

**Consequências negativas (aceitas).** Um endpoint simples envolve ~6 arquivos onde MVC usaria 3; **é over-engineering para o domínio real** — registrado explicitamente, porque o entregável não é o CRUD, é a demonstração de arquitetura; custo de mapeamento — mitigado por MapStruct em tempo de compilação; curva de leitura maior — mitigada por README com diagrama. **Risco de dogmatismo** — onde a hexagonal não agrega (endpoints puramente de leitura de `github`), o adaptador é fino e sem mapeamento redundante. O padrão serve ao projeto, não o contrário.

### ADR-0004: PostgreSQL como fonte de verdade do conteúdo

**Status:** Aceito

**Contexto.** O conteúdo precisa de um lugar para morar. Requisitos: adicionar um projeto em poucos minutos; conteúdo sob controle de versão; a escolha deve **exercitar competência backend**; custo zero.

**Decisão.** PostgreSQL 16 como fonte de verdade, com todo o conteúdo inserido por migrações Flyway versionadas. Adicionar projeto = escrever `V7__add_project_x.sql` e fazer deploy.

**Alternativas descartadas**

- *Markdown no repositório (MDX)* — zero infraestrutura e ótimo DX, mas **elimina o backend**: sem banco, não há JPA, migração, repositório nem teste de integração. Some justamente a evidência que o portfólio existe para produzir.
- *CMS headless* — adiciona dependência externa com free tier volátil; o conteúdo sai do controle de versão; e transforma o portfólio em consumidor de API alheia, quando a intenção é ser o produtor.
- *JSON estático servido pela API* — não exercita persistência, que é o ponto.
- *MongoDB* — os dados são fortemente relacionais; seria decisão indefensável em entrevista.

**Consequências positivas.** Exercita JPA, Flyway, Testcontainers, índices e prevenção de N+1; conteúdo versionado com revisão por diff; migrações demonstram maturidade (é o oposto de `ddl-auto=update`); modelagem relacional permite consultas ricas; rollback de conteúdo é `git revert` + deploy.

**Consequências negativas (aceitas).** Editar conteúdo exige SQL e deploy — aceitável para um portfólio que muda poucas vezes por ano (painel administrativo no backlog v2.0); mais infraestrutura — Postgres gerenciado no free tier; um ponto adicional de falha — mitigado pelo SSG/ISR; risco de perda no free tier — **as migrações são o backup**: o conteúdo pode ser recriado do zero a partir do repositório. Essa propriedade é uma consequência elegante da decisão, não coincidência.

### ADR-0005: BFF em Route Handlers do Next.js

**Status:** Aceito

**Contexto.** O navegador precisa enviar o formulário de contato para a API Java. Restrições: domínios diferentes (CORS); a chave da API não pode ir para o cliente; a verificação do Turnstile precisa acontecer no servidor; o IP real é necessário para o rate limit.

**Decisão.** Todas as chamadas de **escrita** do navegador passam por Route Handlers do Next.js, que validam com Zod, verificam o Turnstile, extraem o IP real, injetam o `X-Service-Key`, encaminham para a API e normalizam o erro. Leituras em Server Components chamam a API **diretamente do servidor** — já estão no servidor, e um salto extra seria desperdício.

**Alternativas descartadas**

- *Chamada direta do navegador* — exigiria CORS permissivo, expõe a origem da API e colocaria a chave de serviço no cliente (ou seja, não haveria autenticação alguma).
- *Server Actions chamando a API diretamente* — elegante, mas não expõe endpoint HTTP nomeado, o que dificulta testes de integração e esconde o fluxo. **Decisão híbrida adotada:** a Server Action existe e chama o Route Handler — progressive enhancement no formulário + endpoint testável.
- *API Gateway dedicado* — over-engineering evidente: uma aplicação, um cliente.

**Consequências positivas.** Nenhum segredo no cliente; **CORS deixa de existir como problema**; a origem da API não é exposta; Turnstile verificado onde é confiável; validação em camadas; erros normalizados; ponto natural para rate limiting na edge.

**Consequências negativas (aceitas).** Um salto de rede a mais (~20–50ms) — irrelevante para uma operação que já envolve envio de e-mail; duplicação de schemas entre BFF (Zod) e API (Bean Validation) — **defesa em profundidade, não violação de DRY**: o BFF valida forma, a API valida regra de negócio; mais um lugar para depurar — mitigado pelo `correlationId` propagado do BFF ao banco.

### ADR-0006: Hospedagem em Vercel e Render

**Status:** Aceito, **emendado pelo [ADR-0010](#adr-0010-postgres-no-neon-emenda-o-adr-0006)** quanto ao banco de dados, que passou para o Neon. Todo o restante — web na Vercel, API no Render, deploy por push, preview por PR — continua aceito e em vigor.

**Contexto.** Três componentes para hospedar (web, api, banco), com orçamento próximo de zero, deploy automatizado a partir do Git e HTTPS com domínio customizado.

**Decisão.** Web na **Vercel** (Hobby); API e Postgres no **Render** (Free, container Docker). Deploy automático da `main`; preview deploys por PR.

**Alternativas descartadas**

- *VPS com Docker Compose e Nginx* — demonstraria mais competência de infraestrutura, mas tem custo recorrente, responsabilidade de manutenção de segurança e perda da CDN global. **Registrado como possível evolução** — migrar a API para VPS é uma troca de destino de deploy, não mudança de arquitetura.
- *AWS (ECS + RDS + CloudFront)* — free tier expira em 12 meses e o risco de custo inesperado é real. Um portfólio que gera fatura surpresa é um problema, não uma demonstração.
- *Railway* — free tier baseado em crédito mensal limitado, continuidade incerta.
- *Vercel para tudo, API em serverless* — Java em serverless tem cold start proibitivo.

**Consequências positivas.** Custo zero; CDN global; preview por PR; HTTPS automático; deploy por push; Postgres gerenciado com backup; zero manutenção de SO.

**Consequências negativas (aceitas).**

- **Hibernação após 15 min de inatividade** — primeira requisição leva 20–50s. **Mitigação em duas camadas:** (1) o site é SSG/ISR, então o visitante recebe HTML da CDN e **nunca espera pela API**; o cold start afeta apenas a revalidação em background; (2) cron gratuito faz ping em `/actuator/health` a cada 10 min.
- **512 MB de RAM** — mitigado por `-XX:MaxRAMPercentage=75`, JMX desabilitado, pool dimensionado.
- **Free tier do Postgres pode expirar** — as migrações Flyway são o backup; o banco pode ser recriado em minutos em qualquer provedor.
- **Menos demonstração de DevOps que uma VPS** — compensado por Dockerfile multi-stage, GitHub Actions, health checks e métricas Prometheus, todos no repositório e igualmente avaliáveis.

### ADR-0007: SSG com ISR e revalidação por tag

**Status:** Aceito

**Contexto.** O conteúdo muda raramente, mas precisa ser rápido (LCP < 1.8s), indexável, resiliente (não pode quebrar se a API estiver hibernando) e barato.

**Decisão.** SSG como padrão, com ISR para revalidação em background e invalidação por tag. Quando o conteúdo muda, o backend chama `POST /api/revalidate` e a página é regenerada imediatamente — sem esperar o TTL.

**Alternativas descartadas**

- *SSR em toda requisição* — **toda visita dependeria da API**. Com hibernação no free tier, o primeiro visitante do dia esperaria 30 segundos — em termos práticos, perder esse visitante.
- *CSR* — SEO ruim (crawler recebe casca vazia) e LCP alto. Para um portfólio, onde a busca por nome é o principal canal de descoberta, isso inviabiliza o objetivo.
- *SSG puro sem ISR* — exigiria rebuild manual a cada mudança, e congelaria as estatísticas do GitHub no momento do build.
- *SSG com rebuild por webhook* — rebuilda o site inteiro por uma mudança pontual; ISR com tag regenera apenas a página afetada.

**Consequências positivas.** TTFB de ~50ms direto da CDN; HTML completo para crawlers; **o site sobrevive à indisponibilidade da API**; pico de tráfego absorvido pela CDN; custo próximo de zero (é o que viabiliza o ADR-0006); atualização imediata via `revalidateTag`.

**Consequências negativas (aceitas).** Conteúdo até 1h desatualizado se a invalidação falhar — irrelevante aqui; build mais longo conforme cresce o número de projetos — com 10–30 projetos o custo é de segundos, e acima disso `dynamicParams: true` resolve; complexidade de invalidação em duas camadas de cache — mitigada por uma única rota `/api/revalidate`; preview de conteúdo exigiria draft mode — fora do escopo, já que o preview deploy do PR cumpre esse papel.

### ADR-0008: Resiliência na integração com o GitHub

**Status:** Aceito

**Contexto.** A API do GitHub tem rate limit (60/h sem token, 5.000/h com), **responde 403 e não 429** quando o limite é excedido (o que engana implementações ingênuas), tem latência variável e cai eventualmente. Sem tratamento, uma falha lá se propaga: a chamada trava, o Server Component não resolve, a página não gera, o build ou a revalidação falham. Uma dependência opcional derruba uma página inteira.

**Decisão.** Conjunto padrão de padrões de resiliência com Resilience4j: timeout (2s/3s), retry (3×, backoff exponencial com jitter), circuit breaker (50% de falha em 10 chamadas, half-open após 60s), bulkhead (5 concorrentes), cache Caffeine 6h, fallback em cadeia e cache warming agendado.

**Regra que resume a decisão:** nenhuma falha do GitHub jamais alcança o usuário. O pior cenário é a seção exibir dados de algumas horas atrás, ou um estado vazio elegante.

**Alternativas descartadas**

- *Chamar direto do navegador* — token vazaria; rate limit de 60/h por IP; e a agregação ficaria na apresentação.
- *Chamar no build e congelar* — os dados envelheceriam até o próximo deploy, e o valor da seção é mostrar atividade recente.
- *Try/catch simples* — sob falha persistente, cada requisição continuaria tentando e falhando após o timeout, acumulando latência. O circuit breaker existe precisamente para parar de tentar.
- *GraphQL para tudo* — **parcialmente adotado:** o calendário de contribuições só existe no GraphQL; para o resto, REST é mais simples e igualmente eficaz.

**Consequências positivas.** O site funciona com o GitHub completamente fora do ar; cache de 6h reduz as chamadas para ~4/dia; estados do circuito expostos como métricas; **é a parte do código que melhor demonstra competência backend**; testável de forma determinística com WireMock.

**Consequências negativas (aceitas).** Configuração muito maior que um `fetch` simples — justificado, é a demonstração central do MVP 4; dados até 6h desatualizados — irrelevante; mais uma dependência; complexidade de teste maior — contrapartida: são justamente esses testes que **provam** que a resiliência existe, em vez de apenas afirmá-la.

### ADR-0009: Spring Boot 3.5 (substitui o ADR-0002)

**Status:** **Substituído pelo [ADR-0011](#adr-0011-spring-boot-41-substitui-o-adr-0009)** quanto à versão, quando a linha 3.5 saiu do suporte OSS em junho de 2026. O raciocínio abaixo continua sendo o registro de por que a 3.5 foi escolhida no lugar da 3.4 — e é literalmente o mesmo que levou à 4.1 depois.

**Contexto.** O ADR-0002 travou o backend em Spring Boot 3.4. Quando o commit 03 foi escrito, essa linha já não recebia correções: o último patch da 3.4 foi o `3.4.13`, de 18/12/2025, e nada saiu depois. A 3.5, por sua vez, seguia ativa — o `3.5.16` é de 25/06/2026.

O conflito é com um critério do próprio plano, não com preferência. A [seção 17.2](#172-definition-of-done-global-do-projeto) exige **zero CVE HIGH ou CRITICAL** como condição de release. Uma linha sem manutenção não tem como cumprir isso: a primeira vulnerabilidade publicada no Spring Framework 6 ou em qualquer dependência transitiva ficaria sem correção oficial, e a única saída seria fixar versões de dependência à mão — exatamente o tipo de remendo que o plano evita.

Existe ainda uma segunda pressão, registrada na operação: o Dependabot propôs o salto para a 4.1.0 disfarçado de bump de rotina, dentro de um grupo que não filtrava `update-types`. A ausência de uma decisão explícita sobre versão é o que torna esse tipo de proposta perigosa.

**Decisão.** Spring Boot **3.5.x**, hoje na 3.5.16.

O que torna a troca barata é que ela **não muda a plataforma**:

| | 3.5.16 | 4.1.0 |
|---|---|---|
| Spring Framework | 6.2.19 | 7.0.8 |
| Jakarta Persistence | 3.1.0 (Jakarta EE 10) | 3.2.0 (Jakarta EE 11) |
| Hibernate | 6.6.53 | 7.4.1 |

A 3.5 permanece em Spring Framework 6 e Jakarta EE 10 — a mesma base que a 3.4. Nenhum trecho de código deste plano muda, e nenhuma das decisões dos ADRs 0003, 0004, 0005 e 0008 é afetada.

**Alternativas descartadas**

- *Permanecer na 3.4* — é a alternativa que motivou este ADR. Cumpriria a letra do ADR-0002 e violaria o critério de segurança da seção 17.2, que é o mais duro dos dois. Fidelidade a uma decisão vencida não é rigor, é inércia.
- *Saltar para o Spring Boot 4* — troca Spring Framework 6 por 7, Jakarta EE 10 por 11 e Hibernate 6 por 7. Migração real, com risco real, para ganhar uma linha que já está disponível na 3.5. Pior: consumiria o tempo do MVP 1, que é o que elimina o maior risco do projeto. **Fica registrado como reavaliação futura** — a decisão é adiar, não recusar.
- *Fixar dependências vulneráveis à mão sobre a 3.4* — resolveria CVEs pontuais e criaria uma matriz de versões que ninguém mais testou junto. Trocaria um risco conhecido por um desconhecido.

**Consequências positivas.** O critério de zero CVE HIGH/CRITICAL volta a ser alcançável por atualização normal; a linha recebe patch; nenhuma mudança de código; o Dependabot passa a ter um alvo definido — majors bloqueados nos dois ecossistemas, patches liberados.

**Consequências negativas (aceitas).** O projeto fica uma linha atrás da mais recente, e um avaliador pode perguntar por que não a 4 — a resposta é este documento, que é justamente o que a seção 14 existe para produzir; a 3.5 também terá um fim de vida, e a decisão precisará ser revisitada, o que exigirá um ADR-00xx no futuro; e fica a dívida explícita de reavaliar o Spring Boot 4 quando o MVP 1 estiver em produção.

### ADR-0010: Postgres no Neon (emenda o ADR-0006)

**Status:** Aceito. Emenda o [ADR-0006](#adr-0006-hospedagem-em-vercel-e-render) na parte do banco de dados. Tudo o mais que o ADR-0006 decidiu — web na Vercel, API no Render em container Docker, deploy automático da `main`, preview por PR — **continua valendo sem alteração**.

**Contexto.** O ADR-0006 colocou os três componentes em duas plataformas, com o Postgres no Render junto da API. Ao preparar o commit 23, a conferência da documentação dos planos gratuitos (11/08/2026) revelou o que decide este ADR: **o Postgres gratuito do Render expira 30 dias depois de criado** e é apagado após mais 14.

O ADR-0006 previu esse risco e o aceitou, na forma "*Free tier do Postgres pode expirar — as migrações Flyway são o backup; o banco pode ser recriado em minutos em qualquer provedor*". A mitigação está correta quanto ao dado e errada quanto ao efeito. O que se perde na expiração não é o conteúdo — é **o site**. Trinta dias após o deploy o portfólio sai do ar sozinho, sem aviso, e só volta quando alguém perceber e recriar o banco à mão.

Isso colide com dois pontos do próprio plano. A [seção 2.2](#22-manutenibilidade) quer que manter o portfólio custe minutos por mudança de conteúdo, não uma tarefa recorrente de infraestrutura para continuar existindo. E a [seção 1.5](#15-escopo) declara que o autor é o público de manutenção ao longo dos anos: um sistema que exige intervenção mensal para não morrer falha com quem mais importa. Pior no contexto: a falha acontece calada, e o momento provável de descobri-la é quando alguém abrir o link do currículo.

**Decisão.** Postgres no **Neon**, plano gratuito, região `us-east-2` (Ohio) — a mesma da API no Render, para que banco e aplicação não paguem a travessia do país em toda consulta.

**Alternativas descartadas**

- *Recriar o banco no Render a cada 30 dias* — transforma o portfólio em tarefa recorrente, e o custo do esquecimento é o site fora do ar. Automatizar seria escrever um robô para contornar um limite que outro provedor simplesmente não tem.
- *Postgres pago no Render* — resolve o problema e viola a restrição de orçamento próximo de zero da [seção 1.5](#15-escopo), que é premissa e não preferência.
- *Manter o Render e aceitar a expiração* — é a posição do ADR-0006, e é o que este ADR revisa. Aceitar um risco cujo custo é "o portfólio sai do ar sozinho" não é tolerância a risco, é não ter medido a consequência.
- *SQLite em volume no Render* — o disco do plano gratuito é efêmero, e trocaria o Postgres 16 idêntico em dev, teste e produção por outro banco, violando a [seção 4.3](#43-testes) e o motivo pelo qual o Testcontainers existe neste projeto.
- *Outros provedores gerenciados (Supabase, Railway, Aiven)* — **não foram avaliados em profundidade**, e o registro honesto é esse: o Neon atendia a todas as restrições na primeira verificação, e continuar comparando teria custado tempo do MVP 1 sem mudar o resultado. Se o Neon mudar de política, é aqui que a busca recomeça.

**Consequências positivas.** O site deixa de ter prazo de validade. Os 0,5 GB do plano são folgados para um conteúdo que hoje é uma linha de perfil e alguns links. A suspensão por inatividade acontece em ~5 min mas o retorno é de centenas de milissegundos — uma ordem de grandeza abaixo do minuto que o serviço do Render leva para acordar, de modo que o banco deixa de ser o gargalo da revalidação. E o `render.yaml` fica mais honesto: descreve exatamente o que o Render hospeda, sem um banco que ele não tem.

**Consequências negativas (aceitas).**

- **Mais um provedor na topologia** — mais uma conta, mais um painel e mais uma política que pode mudar sem aviso. A regra registrada é reconferir os limites antes de qualquer mudança de plano ou de provedor.
- **O scale-to-zero exige configuração que o Render não exigiria.** Conexão aberta conta como atividade, então `minimum-idle: 0` e `idle-timeout: 60000` são o que permite o banco dormir — sem eles a cota de compute se esgota com o site parado e ninguém visitando. O `max-lifetime: 300000` existe porque o Neon derruba conexão ociosa pelo lado dele, e um pool que não recicla antes disso entrega ao Hibernate uma conexão morta. E a sonda da plataforma passa a ser `/actuator/health/liveness`, porque o health completo inclui o indicador `db` e acordaria o banco a cada verificação.
- **O pooler do Neon não serve ao driver JDBC.** É PgBouncer em modo transação, e o driver usa prepared statements do servidor a partir da quinta execução. Usa-se o endpoint direto, sem o sufixo `-pooler`, com o HikariCP fazendo o papel de pool. É uma pegadinha a mais na string de conexão, e ela não é adivinhável a partir do que o painel do Neon mostra.
- **O plano gratuito não permite restringir acesso por IP.** A credencial do banco vale de qualquer lugar da internet, o que eleva o custo de vazá-la. Por isso todos os segredos entram como `sync: false` no `render.yaml` e nunca passam pelo repositório — o blueprint foi escrito assim de propósito.

### ADR-0011: Spring Boot 4.1 (substitui o ADR-0009)

**Status:** Aceito. Substitui o [ADR-0009](#adr-0009-spring-boot-35-substitui-o-adr-0002) quanto à versão do Spring Boot. O [ADR-0002](#adr-0002-java-21-e-spring-boot-34) segue valendo em tudo o mais — **Java 21**, Maven, Records, sealed interfaces, Virtual Threads e text blocks não são tocados.

**Contexto.** O ADR-0009 escolheu a linha 3.5 e explicou por quê: a 3.4 tinha parado de receber correções, e a [seção 17.2](#172-definition-of-done-global-do-projeto) exige **zero CVE HIGH ou CRITICAL** como condição de release — critério que uma linha sem manutenção não tem como cumprir.

Em 25/06/2026 a mesma coisa aconteceu com a 3.5. O anúncio do `3.5.16` declara que é **o último release OSS da geração 3.5**, e recomenda subir para a 4.0 ou 4.1 para continuar recebendo suporte aberto. O projeto usava exatamente o `3.5.16`.

Não há decisão nova a tomar aqui, e sim uma já tomada a executar. O ADR-0009 registrou o salto para a 4 como *"reavaliação futura — a decisão é adiar, não recusar"*, e fixou o gatilho: **quando o MVP 1 estiver em produção**. O MVP 1 foi publicado e marcado com a tag `v0.1.0`.

**Decisão.** Spring Boot **4.1.x**.

O que o ADR-0009 apontou como custo do salto continua verdadeiro — e agora foi pago:

| | 3.5.16 | 4.1.0 |
|---|---|---|
| Spring Framework | 6.2.19 | 7.x |
| Jakarta EE | 10 | 11 |
| Hibernate | 6.6.x | 7.x |
| Jackson | 2 (`com.fasterxml.jackson`) | **3** (`tools.jackson`) |
| Java mínimo | 17 | 17 |

A última linha é o ponto que mantém o ADR-0002 intacto: a 4.0 tem **Java 17** como base, então a escolha do Java 21 LTS não é afetada.

**Alternativas descartadas**

- *Permanecer na 3.5* — é a alternativa que motivou este ADR, exatamente como permanecer na 3.4 motivou o anterior. Cumpriria a letra do ADR-0009 e violaria o critério de segurança da seção 17.2, que é o mais duro dos dois. Repetir aqui a frase que já valeu uma vez: fidelidade a uma decisão vencida não é rigor, é inércia.
- *Suporte comercial da 3.5* — existe, e é a saída oferecida a quem não pode migrar. Custa dinheiro, e a [seção 1.5](#15-escopo) fixa orçamento próximo de zero.
- *Adiar de novo, até depois do MVP 2* — foi considerada com um argumento razoável: migração logo após um lançamento bem-sucedido é risco mal colocado. Descartada pelo custo composto — os 9 commits do MVP 2 seriam escritos numa plataforma que seria trocada em seguida, e cada MVP adiante encarece a mesma migração. Com um módulo, quatro tabelas e 73 testes, **este é o momento mais barato que ela teria**.
- *Migrar apenas para a 4.0* — a linha mais antiga das duas suportadas ganharia menos tempo até a próxima migração, pelo mesmo trabalho.

**Consequências positivas.** O critério de zero CVE HIGH/CRITICAL volta a ser alcançável por atualização normal. A dívida mais antiga do projeto sai da lista. E a migração vira um item verificável no repositório: os 73 testes seguem verdes, o documento OpenAPI publicado é byte a byte idêntico ao versionado — o springdoc 3.1 preserva `required`, os tipos nuláveis e o enum —, e a imagem de produção sobe em 19,5s contra 23,7s da anterior, usando 249 MB dos 512 do free tier.

**Consequências negativas (aceitas).**

- **A modularização da 4.0 espalhou o que era um starter em vários módulos**, e cada peça ausente só se manifesta ao tentar usá-la — `spring-boot-starter-webmvc` no lugar de `-web`, starter próprio para Flyway, `resttestclient` e `restclient` para o `TestRestTemplate`, `starter-webmvc-test` para o `@WebMvcTest`. Uma delas falha com `NoClassDefFoundError` dentro de um `@ConditionalOnMissingBean`, mensagem que não cita nenhuma das classes envolvidas. Está tudo registrado no corpo do commit da migração.
- **Jackson 3 alcança o código de produção**, movendo o databind de `com.fasterxml.jackson` para `tools.jackson`. A regra de ArchUnit que barra Jackson no domínio passou a listar os dois pacotes, porque as anotações ficaram no pacote antigo — e é a anotação o vazamento provável.
- **O Testcontainers deixou de ser gerenciado pelo Boot** e passou a exigir BOM próprio, com os artefatos renomeados na 2.0.
- **A 4.1 também terá um fim de vida**, e esta decisão será revisitada — como o ADR-0009 previu para si mesmo. A diferença é que agora existe um precedente de como fazê-lo: verificar o calendário de suporte, não a data do último release.

### ADR-0012: CSP estática sem nonce (emenda a seção 2.4)

**Status:** Aceito. Emenda a [seção 2.4](#24-segurança) quanto à **forma** da Content Security Policy. Todo o restante da seção 2.4 — HSTS, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy` e as demais medidas — continua valendo sem alteração, e é entregue junto com esta decisão.

**Contexto.** A seção 2.4 lista, entre os cabeçalhos exigidos, "CSP com nonce". Nenhum dos 54 commits agenda cabeçalho nenhum — é lacuna do plano, da mesma classe que a chave de serviço foi. A medição no site publicado, em 11 e 12/08/2026, confirmou o efeito: existe **apenas** o HSTS, que a Vercel envia por conta própria.

Ao implementar, os dois requisitos se revelaram incompatíveis entre si. Um nonce é único por requisição, então só pode existir onde há uma requisição no instante da renderização. A documentação do Next é literal a respeito: com nonce, *"Static optimization and Incremental Static Regeneration (ISR) are disabled"*.

E o ISR não é preferência de performance aqui. É a peça que o [ADR-0006](#adr-0006-hospedagem-em-vercel-e-render) usa para tornar aceitável o plano gratuito do Render: o serviço hiberna após 15 minutos sem tráfego e leva cerca de um minuto para voltar. Com a home pré-renderizada, o visitante recebe HTML da CDN e só a revalidação em segundo plano toca a API. Adotar o nonce devolveria esse minuto de espera a quem abrisse o site depois de um período parado — que é, justamente, o caso mais provável no portfólio de uma pessoa só.

A medição que fecha o quadro: o HTML pré-renderizado da home tem **11 scripts inline** — os dados de flight do App Router e o script anti-FOUC do next-themes — contra 7 externos. São eles que forçam a escolha entre nonce e `'unsafe-inline'`; nenhuma política sem um dos dois deixa a página funcionar.

**Decisão.** CSP **estática, sem nonce**, declarada em `next.config.ts` e aplicada a todas as rotas, com `'unsafe-inline'` em `script-src` e `style-src` e todas as demais diretivas restritivas — `object-src 'none'`, `base-uri 'self'`, `form-action 'self'`, `frame-ancestors 'none'`, `default-src 'self'`. Junto vão `X-Content-Type-Options`, `Referrer-Policy` e `Permissions-Policy`. O HSTS fica com a Vercel, para não haver duas fontes de verdade para o mesmo valor.

**Alternativas descartadas**

- *CSP com nonce via middleware* — é o que a seção 2.4 pede ao pé da letra, e é a razão deste ADR existir. Cumpriria o texto e desligaria o ISR, transferindo ao visitante o cold start que o ADR-0006 foi escrito para evitar. A troca é ruim nos dois sentidos: o ganho de segurança é pequeno num site sem entrada de usuário, e a perda de experiência é de um minuto de tela em branco. Fidelidade à letra de um requisito, quando ela custa o efeito de outro, não é rigor.
- *SRI experimental (`experimental.sri`)* — é a única saída que preservaria o ISR **e** dispensaria o `'unsafe-inline'`, e por isso foi considerada a sério. Descartada por duas razões independentes: o recurso é declaradamente experimental, e o que ele promete são hashes dos **arquivos** de script, enquanto o problema aqui são os 11 scripts **inline**. Uma guarda experimental que pode parar de valer num bump de versão, em silêncio, é pior do que uma diretiva honesta sobre a própria limitação — é o padrão da seção 4.1 outra vez, e este projeto já o pagou quatro vezes.
- *Hashes dos scripts inline* — descartada **com medida**, e a medida separa dois casos que pareciam um só. Bloqueando o inline de propósito, o navegador informa o hash de cada recurso recusado: os 11 scripts devolveram **11 hashes distintos**, porque carregam os dados de flight, que mudam com o conteúdo vindo do banco e com cada build. Fixá-los exigiria recalcular a política a cada publicação e a cada edição de perfil, e a falha apareceria como a página deixando de hidratar em produção.
- *Hash do estilo inline, mantendo o `'unsafe-inline'` só no `script-src`* — este é o caso separado, e é **tecnicamente viável**: o `style-src` recusou um hash **único e estável**, o do CSS que o next-themes injeta para desligar transições. Descartada assim mesmo, e vale registrar por quê, porque a razão não é a mesma das outras: esse hash é o de um detalhe interno de uma dependência, não de código deste repositório. Um bump de patch do next-themes muda o CSS, invalida o hash e quebra a troca de tema **sem que nada acuse** — e hoje não existe verificação automatizada que pegasse isso. A partir do commit 52, com Playwright e axe no CI, passa a existir; **é lá que esta alternativa deve ser reavaliada**, e não antes.
- *Adiar a CSP inteira para o MVP 5* — deixaria o site sem `X-Content-Type-Options`, `Referrer-Policy` e `Permissions-Policy`, que não têm trade-off nenhum, por causa da única diretiva que tem.

**Consequências positivas.** O site deixa de poder ser embutido em iframe de terceiro (`frame-ancestors 'none'`), fechando o vetor de clickjacking. `base-uri` e `form-action` fecham dois caminhos clássicos de exfiltração que sobrevivem mesmo a um XSS já ocorrido. `object-src 'none'` remove a superfície de plugins legados. O ISR permanece intacto — verificável no relatório do build, que segue marcando `○ /` com `Revalidate 1h` — e nenhum middleware entra no projeto, o que mantém o caminho da requisição com uma peça a menos.

**A política foi validada quebrando**, como toda guarda deste projeto. Removido o `'unsafe-inline'` do `script-src`, os 11 scripts inline são bloqueados, o React não hidrata (`Connection closed`), o anti-FOUC não roda — `data-theme` fica nulo — e a página perde título e `<h1>`. Removido o do `style-src`, a injeção do next-themes é bloqueada a cada troca de tema. Restaurada a política, as duas quebras desaparecem e o console volta a zero. As diretivas não são decorativas: cada uma foi vista fazendo efeito.

**Armadilha de método encontrada no caminho.** O `headers()` do Next é avaliado **no build** e gravado no `routes-manifest.json`; o `next start` lê o manifesto. Editar o `next.config.ts` e reiniciar o servidor serve a política **antiga**, sem aviso. Toda verificação de cabeçalho exige `build` novo — medir depois de um simples restart é medir o que já estava lá.

**Consequências negativas (aceitas).**

- **`'unsafe-inline'` em `script-src` é a metade fraca desta política, e não adianta chamá-la de outra coisa.** O que a torna aceitável hoje é a ausência de caminho de injeção: o site não tem formulário, não aceita entrada de usuário, não renderiza HTML de terceiro e não carrega script externo. Todo o conteúdo vem da própria API, através de Server Components. A diretiva autoriza um inline que ninguém tem como plantar.
- **Isso muda no MVP 5**, que traz o formulário de contato, o Turnstile da Cloudflare e, com ele, `script-src` e `frame-src` apontando para `challenges.cloudflare.com`. É quando a política precisa crescer e quando esta decisão deve ser reavaliada — com a diferença de que, lá, haverá entrada de usuário para justificar o custo do nonce, e o formulário não é a home pré-renderizada.
- **`'unsafe-inline'` em `style-src` é requisito de funcionamento, não conveniência.** O next-themes injeta um `<style>` em tempo de execução para desligar transições durante a troca de tema; bloqueá-lo faria as cores voltarem a interpolar no meio da troca, que é exatamente o mecanismo por trás dos dois falsos positivos de contraste investigados no MVP 1.
- **Uma política única para todas as rotas** não distingue a home pré-renderizada de uma rota dinâmica futura. Quando a distinção passar a importar — de novo, MVP 5 —, ela terá de ser introduzida.

#### Reavaliação no MVP 5 — feita no commit 49, e a decisão não muda

Este ADR marcou o próprio prazo: *"isso muda no MVP 5, que traz o formulário de contato, o Turnstile da Cloudflare e, com ele, `script-src` e `frame-src` apontando para `challenges.cloudflare.com`"*. O commit 49 chegou, e as duas coisas que faltavam para a pergunta ficar séria existem agora: **entrada de usuário** e **script de terceiro**.

**O que mudou na política.** Três diretivas, e cada uma cobre uma peça diferente do mesmo widget — o script que a página carrega, o `<iframe>` que ele monta, e as requisições que esse script faz para buscar o desafio:

```
script-src  'self' 'unsafe-inline' https://challenges.cloudflare.com
frame-src   https://challenges.cloudflare.com
connect-src 'self' https://challenges.cloudflare.com
```

⚠️ `connect-src` precisa ser declarada de propósito: sem ela quem responde é o `default-src 'self'`, que não conhece a Cloudflare. E `'self'` entra junto porque declarar a diretiva **substitui** o default por inteiro — listar só o terceiro barraria as requisições do próprio site. Sem qualquer uma das três o widget não aparece, e formulário sem widget passa a recusar toda mensagem enviada.

⚠️ **`frame-src` deixa de permitir a própria origem, e isso foi observado em funcionamento:** uma tentativa de embutir a home num `<iframe>` durante a verificação foi bloqueada pela política. O site não emoldura nada de si mesmo, então a restrição não custa nada — mas quem for embutir algo no futuro precisa mexer aqui.

**O que não mudou, e o motivo é o mesmo de antes.** O nonce continua desligando o ISR, e o formulário vive **na home** — exatamente a rota pré-renderizada que o [ADR-0006](#adr-0006-hospedagem-em-vercel-e-render) protege do cold start do Render. A saída de "uma política por rota", registrada acima como consequência negativa, também não ajuda aqui: ela existiria para poupar a home, e é a home que tem o formulário.

**A condição que tornava o `'unsafe-inline'` aceitável foi reexaminada, não repetida.** O texto original apoiava-se em "o site não tem formulário, não aceita entrada de usuário". A primeira metade caiu; a segunda merece a medida:

| Por onde a entrada de usuário passa | O que acontece com ela |
|---|---|
| Volta para a tela depois de uma recusa | `defaultValue` de `<input>` — atributo escapado pelo React, nunca HTML |
| Mensagens recebidas | Vão para o banco e para um e-mail. **Não são renderizadas em lugar nenhum do site** |
| Campos do formulário | Lidos como texto (`typeof valor === 'string'`), validados por Zod no servidor |

Não existe caminho por onde entrada de usuário vire script inline nesta página — que era a condição registrada. **O que muda é a margem:** antes não havia entrada nenhuma, e agora há uma que é segura por construção. A diferença entre "impossível" e "seguro enquanto ninguém renderizar isso como HTML" é real, e é ela que mantém o item na tabela de dívidas em vez de encerrá-lo.

**Novo prazo:** commit 52, junto do hash do `style-src`. É quando Playwright e axe entram no CI e passa a existir verificação automatizada capaz de pegar uma quebra de hidratação ou de política em silêncio — que é a única coisa que faltava para a alternativa dos hashes deixar de ser perigosa.

---
---

# PARTE II — EXECUÇÃO

---

## 15. Estratégia de MVPs

### 15.1 O que é um MVP neste projeto

Um MVP aqui é **um incremento independentemente publicável e valioso** — não uma sprint de calendário. O critério é único e severo:

> **Se o desenvolvimento parasse ao final deste MVP, o que existe já teria valor como portfólio?**

Se a resposta for "não", o recorte está errado.

### 15.2 A decisão que estrutura toda a Parte II

**O deploy sai do fim e entra no MVP 1.**

Esta é a diferença mais importante entre um cronograma de sprints tradicional e um cronograma por MVP. No modelo em que o deploy é a última etapa, o projeto fica 100% concluído ou 0% útil — e o maior risco identificado ([seção 17](#171-riscos)) é justamente "o escopo inflar e o projeto nunca ser publicado".

Colocando o deploy no MVP 1, a partir do **commit 23** existe um link compartilhável em produção. Todo MVP seguinte melhora um site que já está no ar. O risco de nunca lançar cai para praticamente zero.

### 15.3 Visão geral dos 7 MVPs

| MVP | Nome | Commits | Esforço | Entregável — o que existe ao final |
|-----|------|---------|---------|-----------------------------------|
| **0** | Fundação técnica | 01–09 | ~11h | Monorepo com CI verde. Sem valor externo; é pré-requisito. |
| **1** | **Portfólio publicado** | 10–23 | ~22h | **Link em produção** com hero real vindo do Postgres via API Java. Já serve como portfólio mínimo. |
| **2** | Perfil completo | 24–32 | ~15h | Sobre, timeline, skills e CV. Substitui o currículo. |
| **3** | Prova de trabalho | 33–39 | ~14h | Projetos com filtro e páginas de detalhe. É o que converte candidatura em entrevista. |
| **4** | Prova de engenharia backend | 40–44 | ~12h | GitHub com circuit breaker e cache. É o MVP que mais impressiona um avaliador técnico. |
| **5** | Conversão | 45–49 | ~12h | Formulário funcional ponta a ponta. Fecha o funil. |
| **6** | Alcance e qualidade | 50–54 | ~14h | SEO, i18n, testes, performance, docs. Torna o portfólio descobrível. |

**Total:** 54 commits · ~100h · ~10 semanas em ritmo paralelo (~10h/semana).

### 15.4 Regras de execução

1. **Um MVP por vez.** Não se inicia o próximo com o anterior incompleto.
2. **Todo commit deixa o build verde.** Nenhum "conserta o commit anterior".
3. **Ao final de cada MVP:** deploy, teste manual de teclado, verificação do Lighthouse, e tag no Git (`v0.1.0`, `v0.2.0`…).
4. **Se um MVP atrasar mais de 50%:** cortar escopo do MVP, não estender o prazo.
5. **MVPs 4 e 6 são adiáveis** sem bloquear o lançamento. MVPs 1, 2, 3 e 5 não são.

### 15.5 Como usar cada ficha de MVP

Cada MVP abaixo traz:

- **Objetivo** — a frase única que justifica o MVP
- **Por que agora** — a razão da posição na sequência
- **Entregável** — o que existe ao final
- **Commits** — cada um com objetivo, arquivos afetados e mensagem em Conventional Commits
- **Definition of Done** — critérios verificáveis
- **Riscos específicos**

---

## 16. Os 7 MVPs e seus 54 commits

---

### MVP 0 — Fundação técnica *(commits 01–09)*

**Objetivo.** Sair de repositório vazio para monorepo com as duas aplicações rodando, ferramentas de qualidade configuradas e CI verde. Nenhuma UI ainda.

**Por que agora.** Configurar ferramenta de qualidade depois de 30 arquivos escritos significa reformatar tudo e brigar com o histórico. Configuração vem antes de código, sempre.

**Entregável.** Nada visível ao usuário — e isso é esperado. É o único MVP sem valor externo; ele existe para que todos os outros sejam possíveis.

**Esforço estimado:** ~11h

#### Commit 01
```
chore: initialize monorepo with pnpm workspaces and turborepo
```
**Objetivo.** Criar a estrutura base do repositório.
**Arquivos.** `package.json`, `pnpm-workspace.yaml`, `turbo.json`, `.gitignore`, `.nvmrc`, `.editorconfig`, `LICENSE`, `README.md`

#### Commit 02
```
build(web): scaffold next.js 15 app with typescript strict mode
```
**Objetivo.** Aplicação web mínima, com TypeScript no modo mais rígido desde o primeiro arquivo.
**Arquivos.** `apps/web/package.json`, `next.config.ts`, `tsconfig.json`, `src/app/layout.tsx`, `src/app/page.tsx`

#### Commit 03
```
build(api): scaffold spring boot 3.4 application with java 21
```
**Objetivo.** Aplicação Spring mínima, com Actuator e perfis configurados.
**Arquivos.** `apps/api/pom.xml`, `PortfolioApplication.java`, `application.yml`, `application-local.yml`

#### Commit 04
```
build(web): configure tailwind css 4 and postcss
```
**Objetivo.** Pipeline de estilo pronto antes de qualquer componente existir.
**Arquivos.** `apps/web/postcss.config.mjs`, `src/app/globals.css`

#### Commit 05
```
chore: configure eslint, prettier and shared typescript config
```
**Objetivo.** Padrão de código automatizado no lado TypeScript.
**Arquivos.** `packages/eslint-config/*`, `packages/typescript-config/*`, `apps/web/eslint.config.mjs`, `.prettierrc`, `.prettierignore`

#### Commit 06
```
chore(api): configure spotless with google java format
```
**Objetivo.** Formatação automatizada no lado Java — o equivalente do Prettier.
**Arquivos.** `apps/api/pom.xml` (plugin Spotless)

#### Commit 07
```
chore: add husky, lint-staged and commitlint hooks
```
**Objetivo.** Impedir que código fora do padrão entre no repositório. A partir daqui, commit fora de Conventional Commits é rejeitado.
**Arquivos.** `.husky/pre-commit`, `.husky/commit-msg`, `.husky/pre-push`, `commitlint.config.js`, `.lintstagedrc.json`

#### Commit 08
```
build(infra): add docker compose with postgres 16
```
**Objetivo.** Ambiente local reproduzível com um comando.
**Arquivos.** `infra/docker/docker-compose.yml`, `.env.example`, `apps/api/src/main/resources/application-local.yml`

#### Commit 09
```
ci: add github actions workflows for web and api
```
**Objetivo.** CI rodando lint, testes e build nas duas aplicações, em jobs paralelos.
**Arquivos.** `.github/workflows/ci-web.yml`, `.github/workflows/ci-api.yml`, `.github/dependabot.yml`, `.github/PULL_REQUEST_TEMPLATE.md`

#### Definition of Done — MVP 0

- [ ] `pnpm dev` sobe o web em `localhost:3000`
- [ ] `./mvnw spring-boot:run` sobe a API em `localhost:8080`
- [ ] `/actuator/health` responde `UP`
- [ ] `docker compose up` sobe o Postgres
- [ ] Commit fora do padrão é rejeitado pelo hook (testar de propósito!)
- [ ] CI verde na `main`
- [ ] Tag `v0.0.1`

**Risco específico.** Configuração de monorepo com duas linguagens costuma consumir mais tempo que o previsto. Buffer de 2h reservado.

---

### MVP 1 — Portfólio publicado *(commits 10–23)*

**Objetivo.** Ter um **link em produção** que já funciona como portfólio mínimo, com o ciclo completo provado: Postgres → arquitetura hexagonal → API → Next → CDN → visitante.

**Por que agora.** É o MVP mais importante do projeto, por duas razões independentes:

1. **Prova a arquitetura.** Todo módulo dos MVPs seguintes replica o padrão estabelecido aqui. Se ele estiver errado, o erro se multiplica por quatro.
2. **Elimina o maior risco do projeto.** A partir do commit 23 existe algo publicável. Se tudo parasse aqui, ainda haveria um portfólio no ar, bonito, rápido e com backend real.

**Entregável.** Site em produção com HTTPS: hero com nome, headline e CTAs; tema claro/escuro; navbar e footer; dados vindos de uma API Java real com Postgres; Swagger UI público.

**Esforço estimado:** ~22h

#### Commit 10
```
feat(ui): add design tokens for color, typography and spacing
```
**Objetivo.** Fundação visual — todos os tokens da [seção 7](#7-design-system), com contraste já validado.
**Arquivos.** `packages/ui/package.json`, `src/styles/tokens.css`, `src/lib/cn.ts`

#### Commit 11
```
feat(web): add self-hosted variable fonts with zero layout shift
```
**Objetivo.** Geist Sans e JetBrains Mono locais, com `adjustFontFallback` — o detalhe que garante CLS zero.
**Arquivos.** `apps/web/src/lib/fonts.ts`, `public/fonts/*`, `src/app/layout.tsx`

#### Commit 12
```
feat(ui): add button, badge and card primitives with cva variants
```
**Objetivo.** Primeiros primitivos do design system, com `forwardRef` e `asChild`.
**Arquivos.** `packages/ui/src/components/{button,badge,card}.tsx`, `src/index.ts`

#### Commit 13
```
feat(web): add theme provider with system preference and no flash
```
**Objetivo.** Tema claro/escuro sem FOUC — script inline antes da hidratação.
**Arquivos.** `src/app/layout.tsx`, `src/components/common/ThemeToggle.tsx`, `src/components/providers/ThemeProvider.tsx`

#### Commit 14
```
feat(web): add layout primitives container, section and heading
```
**Objetivo.** Ritmo vertical e largura consistentes em todo o site.
**Arquivos.** `src/components/layout/{Container,Section}.tsx`, `src/components/common/SectionHeading.tsx`

#### Commit 15
```
feat(web): add navbar with active section tracking and mobile menu
```
**Objetivo.** Navegação principal acessível, com skip link, menu mobile em `Sheet` e rodapé.
**Arquivos.** `src/components/layout/{Navbar,MobileNav,Footer}.tsx`, `src/components/common/{SkipLink,SocialLinks}.tsx`, `src/hooks/use-active-section.ts`

#### Commit 16
```
feat(db): add flyway migration for profile and social link tables
```
**Objetivo.** Schema versionado, substituindo `ddl-auto`. Inclui seed com conteúdo realista.
**Arquivos.** `db/migration/V1__create_profile_tables.sql`, `apps/api/pom.xml` (Flyway)

#### Commit 17
```
feat(api): add profile domain model and hexagonal ports
```
**Objetivo.** Núcleo do domínio, sem uma única importação de framework.
**Arquivos.** `profile/domain/model/{Profile,SocialLink}.java`, `domain/port/in/GetProfileUseCase.java`, `domain/port/out/LoadProfilePort.java`, `shared/domain/EmailAddress.java`

#### Commit 18
```
feat(api): add profile persistence adapter with jpa and mapstruct
```
**Objetivo.** Adaptador de saída — a entidade JPA não vaza para o domínio.
**Arquivos.** `adapter/out/persistence/entity/ProfileEntity.java`, `repository/ProfileJpaRepository.java`, `ProfilePersistenceAdapter.java`, `mapper/ProfilePersistenceMapper.java`

#### Commit 19
```
feat(api): expose GET /api/v1/profile endpoint with problem details
```
**Objetivo.** Primeiro endpoint público, com tratamento uniforme de erro (RFC 9457) e OpenAPI publicado.
**Arquivos.** `adapter/in/web/ProfileController.java`, `dto/ProfileResponse.java`, `application/ProfileService.java`, `shared/error/GlobalExceptionHandler.java`, `shared/config/OpenApiConfig.java`

#### Commit 20
```
test(api): add archunit rules enforcing hexagonal boundaries
```
**Objetivo.** Tornar a arquitetura verificável pelo build. **Testar de propósito uma violação** para confirmar que a regra realmente falha.
**Arquivos.** `test/architecture/{HexagonalArchitectureTest,ModuleBoundaryTest}.java`

#### Commit 21
```
test(api): add profile integration test with testcontainers
```
**Objetivo.** Testar contra Postgres 16 real, não H2 — o divisor entre "sei escrever teste" e "sei testar sistemas".
**Arquivos.** `test/integration/{AbstractIntegrationTest,ProfileIntegrationTest}.java`, `test/support/fixtures/ProfileFixtures.java`, `application-test.yml`

#### Commit 22
```
feat(web): add hero section consuming profile api
```
**Objetivo.** Fechar o ciclo ponta a ponta. Inclui o cliente TS gerado do OpenAPI e a validação de env com Zod.
**Arquivos.** `packages/api-client/*`, `src/lib/api/{client,profile}.ts`, `src/lib/env.ts`, `src/features/hero/components/HeroSection.tsx`, `src/app/[locale]/page.tsx`

#### Commit 23
```
build(infra): add multi-stage dockerfile and deploy to production
```
**Objetivo.** **O site vai ao ar.** Imagem enxuta (JRE 21 slim, usuário não-root), deploy automático da `main`, keep-alive contra hibernação.
**Arquivos.** `infra/docker/Dockerfile.api`, `.github/workflows/deploy.yml`, `render.yaml`, `infra/scripts/keep-alive.sh`, `application-prod.yml`

> **🚀 MARCO — a partir daqui existe um link compartilhável em produção.**

#### Definition of Done — MVP 1

- [ ] Site acessível no domínio de produção, com HTTPS
- [ ] Hero exibe dados vindos do banco de produção
- [ ] Tema alterna sem flash e persiste entre sessões
- [ ] Contraste AA validado nos dois temas
- [ ] Navbar funcional em 320px e 1440px
- [ ] Skip link é o primeiro elemento focável
- [ ] ArchUnit passa **e falha** quando uma regra é violada de propósito
- [ ] Testcontainers roda no CI
- [ ] Swagger UI acessível publicamente
- [ ] Cliente TS gerado no build
- [ ] `/actuator/health` verde em produção
- [ ] Zero violações do axe
- [ ] Tag `v0.1.0`

**Riscos específicos.** (1) O primeiro deploy sempre revela problemas de configuração de ambiente — reservar 3h só para isso. (2) A tentação de "deixar bonito antes de publicar" — o design system está fechado; qualquer refinamento visual vai para o backlog.

---

### MVP 2 — Perfil completo *(commits 24–32)*

**Objetivo.** O site passa a substituir o currículo: quem é, o que fez e o que sabe.

**Por que agora.** É o conteúdo que responde à pergunta central do recrutador ("essa pessoa tem senioridade compatível?") e o que exige menos engenharia nova — replica o padrão do MVP 1 duas vezes. Consolida a arquitetura antes de partir para as partes difíceis.

**Entregável.** Site com Sobre, timeline profissional, skills por categoria e download de CV. Já é suficiente para enviar em uma candidatura.

**Esforço estimado:** ~15h

#### Commit 24
```
feat(db): add flyway migration for experience table
```
**Arquivos.** `db/migration/V2__create_experience_table.sql`

#### Commit 25
```
feat(api): add experience module with timeline ordering
```
**Objetivo.** Ordenação cronológica como regra de domínio, não de apresentação.
**Arquivos.** `profile/domain/model/Experience.java`, `port/in/ListExperiencesUseCase.java`, `port/out/LoadExperiencePort.java`, `application/ExperienceService.java`, adaptadores

#### Commit 26
```
feat(api): expose GET /api/v1/experiences endpoint
```
**Arquivos.** `adapter/in/web/ExperienceController.java`, `dto/ExperienceResponse.java`, `mapper/ExperienceWebMapper.java`

#### Commit 27
```
feat(web): add timeline component with semantic ordered list
```
**Objetivo.** Timeline acessível (`<ol>`), com destaque do cargo atual.
**Arquivos.** `src/features/about/components/{Timeline,TimelineItem}.tsx`

#### Commit 28
```
feat(web): add about section with resume download
```
**Arquivos.** `src/features/about/components/AboutSection.tsx`, `src/components/common/ResumeDownload.tsx`, `public/resume/*`

#### Commit 29
```
feat(db): add flyway migration for skill and category tables
```
**Arquivos.** `db/migration/V3__create_skill_tables.sql`

#### Commit 30
```
feat(api): add skill module with proficiency value object
```
**Objetivo.** Proficiência como enum de domínio, nunca `String`.
**Arquivos.** `profile/domain/model/{Skill,SkillCategory,Proficiency}.java`, portas, `application/SkillService.java`, adaptadores

#### Commit 31
```
feat(api): expose GET /api/v1/skills grouped by category
```
**Objetivo.** Agrupamento no backend — é regra de negócio, não formatação.
**Arquivos.** `adapter/in/web/SkillController.java`, `dto/SkillResponse.java`

#### Commit 32
```
feat(web): add skills section with accessible proficiency labels
```
**Objetivo.** Nível comunicado por texto, não apenas por cor (WCAG 1.4.1). Sem barras de percentual.
**Arquivos.** `src/features/skills/components/{SkillsSection,SkillCard,SkillCategory}.tsx`, `public/icons/tech-sprite.svg`

#### Definition of Done — MVP 2

- [ ] Timeline em ordem cronológica decrescente
- [ ] Períodos formatados por locale (`MMM yyyy`)
- [ ] Experiência atual visualmente distinta e com badge "Atual"
- [ ] Marcação `<ol>` correta, anunciada com posição pelo leitor de tela
- [ ] Download do CV funcionando, com formato e tamanho anunciados
- [ ] Skills agrupadas, com nível em texto além da cor
- [ ] Grid responsivo em 3 breakpoints
- [ ] Ícones sem requisição externa
- [ ] Testes de domínio e integração passando
- [ ] Deploy em produção · Tag `v0.2.0`

**Risco específico.** Falta de conteúdo real (bio, experiências). Mitigação: seeds com conteúdo placeholder realista; substituir conteúdo é uma migration, não um bloqueio.

---

### MVP 3 — Prova de trabalho *(commits 33–39)*

**Objetivo.** Mostrar o que foi construído, com a narrativa que separa júnior de pleno.

**Por que agora.** É a seção que converte candidatura em entrevista. Depende da fundação dos MVPs anteriores e é pré-requisito emocional para o resto — sem projetos, o portfólio é só um currículo bonito.

**Entregável.** Listagem com filtro por tecnologia e uma página estática por projeto, com OG image própria e narrativa Problema → Solução → Resultado.

**Esforço estimado:** ~14h

#### Commit 33
```
feat(db): add flyway migration for project and technology tables
```
**Objetivo.** Inclui `project_tech` (N:N), `project_metric` e índices em `slug` e `featured`.
**Arquivos.** `db/migration/V4__create_project_tables.sql`

#### Commit 34
```
feat(api): add project domain with slug value object
```
**Objetivo.** `Slug` como value object com validação na construção — o compilador impede trocá-lo por uma `String` qualquer.
**Arquivos.** `projects/domain/model/{Project,Technology,ProjectMetric}.java`, `shared/domain/Slug.java`, portas

#### Commit 35
```
feat(api): add project persistence adapter preventing n+1 queries
```
**Objetivo.** `@EntityGraph` no relacionamento projeto ↔ tecnologia. Verificar com log de SQL ativo.
**Arquivos.** `adapter/out/persistence/entity/{Project,Technology}Entity.java`, `ProjectJpaRepository.java`, `ProjectPersistenceAdapter.java`

#### Commit 36
```
feat(api): expose project list and detail endpoints with etag
```
**Arquivos.** `adapter/in/web/ProjectController.java`, `dto/{ProjectSummary,ProjectDetail}Response.java`, `application/ProjectService.java`

#### Commit 37
```
feat(web): add project card and technology stack components
```
**Objetivo.** Card com **uma única** área de foco e imagem otimizada.
**Arquivos.** `src/features/projects/components/{ProjectCard,TechStack}.tsx`

#### Commit 38
```
feat(web): add projects listing with url-synced technology filter
```
**Objetivo.** Filtro compartilhável e indexável via query string, sem requisição de rede.
**Arquivos.** `src/features/projects/components/{ProjectsSection,ProjectFilter}.tsx`, `lib/filter-projects.ts`, `src/app/[locale]/projetos/page.tsx`

#### Commit 39
```
feat(web): add static project detail pages with dynamic og images
```
**Objetivo.** `generateStaticParams` + `generateMetadata` + OG image por projeto + 404 personalizada.
**Arquivos.** `src/app/[locale]/projetos/[slug]/{page.tsx,opengraph-image.tsx,not-found.tsx}`, `src/features/projects/components/{ProjectDetail,ProjectMetrics}.tsx`

#### Definition of Done — MVP 3

- [ ] Todas as páginas de projeto pré-renderizadas (confirmar no output do `next build`)
- [ ] Filtro reflete na URL e sobrevive a recarregamento
- [ ] Estado vazio tratado ("Nenhum projeto com essa tecnologia")
- [ ] Cards com altura consistente, independentemente do texto
- [ ] Sem N+1 (verificado com `spring.jpa.show-sql=true`)
- [ ] OG image correta em cada projeto, validada no LinkedIn e WhatsApp
- [ ] Slug inexistente → 404 personalizada
- [ ] Foco por teclado percorre os cards em ordem lógica
- [ ] Deploy em produção · Tag `v0.3.0`

**Risco específico.** A tentação de escrever cases genéricos ("fiz uma API REST"). Mitigação: a estrutura da página **força** o formato Problema → Solução → Resultado, e `project_metric` exige números. Se não houver número, o case ainda não está pronto.

---

### MVP 4 — Prova de engenharia backend *(commits 40–44)*

**Objetivo.** Implementar a parte do sistema que um avaliador técnico reconhece imediatamente como competência sênior.

**Por que agora.** É o MVP de maior retorno por hora investida em termos de sinalização técnica — e é o único que pode ser adiado sem quebrar o funil (o site funciona sem ele). Por isso vem depois dos MVPs de conteúdo e antes do de conversão apenas se o cronograma estiver saudável; caso contrário, troca de posição com o MVP 5.

**Entregável.** Seção de estatísticas do GitHub que **continua funcionando com a API do GitHub fora do ar** — e o código que prova isso.

**Esforço estimado:** ~12h

#### Commit 40
```
feat(api): add github domain model and provider port
```
**Arquivos.** `github/domain/model/{GitHubStats,LanguageUsage,RepositorySummary}.java`, `port/in/GetGitHubStatsUseCase.java`, `port/out/GitHubStatsProviderPort.java`

#### Commit 41
```
feat(api): add github api adapter with restclient and caffeine cache
```
**Arquivos.** `adapter/out/github/{GitHubApiAdapter,dto,mapper}`, `shared/config/{RestClientConfig,CacheConfig}.java`, `properties/GitHubProperties.java`

#### Commit 42
```
feat(api): add circuit breaker, retry and fallback to github integration
```
**Objetivo.** Tratar o GitHub como a dependência instável que ele é. Inclui `@Scheduled` de reaquecimento e health indicator.
**Arquivos.** `GitHubApiAdapter.java`, `application/GitHubStatsService.java`, `adapter/in/scheduler/GitHubCacheWarmer.java`, `application.yml` (Resilience4j), `observability/health/GitHubHealthIndicator.java`

#### Commit 43
```
test(api): add github adapter tests with wiremock failure scenarios
```
**Objetivo.** **Provar** que o circuit breaker abre, entra em half-open e fecha; e que o fallback é acionado. Cenários: 403 de rate limit, 500, timeout, resposta malformada.
**Arquivos.** `test/github/adapter/out/GitHubApiAdapterTest.java`, `test/support/WireMockConfig.java`

#### Commit 44
```
feat(web): add github stats section with dependency-free svg chart
```
**Objetivo.** Gráfico em SVG puro (economia de ~45 KB) com alternativa textual acessível.
**Arquivos.** `src/features/github/components/{GitHubStats,LanguageChart,ContributionGraph,RepositoryCard}.tsx`, `src/lib/api/github.ts`

#### Definition of Done — MVP 4

- [ ] O site renderiza normalmente com o GitHub simulado como indisponível (testar de verdade!)
- [ ] Teste comprova que o circuit breaker abre e depois fecha
- [ ] Nenhum token no bundle do cliente (verificável com busca no `.next/static`)
- [ ] Métricas de cache e estado do circuito visíveis em `/actuator/prometheus`
- [ ] Gráfico com alternativa textual para leitor de tela
- [ ] Cache hit ratio > 95% em operação normal
- [ ] Deploy em produção · Tag `v0.4.0`

**Risco específico.** Rate limit do GitHub durante o desenvolvimento. Mitigação: usar WireMock localmente na maior parte do tempo; token configurado desde o início.

---

### MVP 5 — Conversão *(commits 45–49)*

**Objetivo.** Fechar o funil: o visitante interessado consegue entrar em contato, e a mensagem **não se perde**.

**Por que agora.** Todo o resto do site existe para levar até aqui. É P0 e não adiável.

**Entregável.** Formulário funcional ponta a ponta, com antispam em camadas, rate limit e garantia de que nenhuma mensagem se perde mesmo se o provedor de e-mail cair.

**Esforço estimado:** ~12h

#### Commit 45
```
feat(db): add flyway migration for contact message table
```
**Objetivo.** Inclui `email_status` (enum) e `ip_hash` — rate limit e auditoria sem armazenar PII (LGPD).
**Arquivos.** `db/migration/V5__create_contact_message_table.sql`

#### Commit 46
```
feat(api): add contact module with domain validation and event
```
**Arquivos.** `contact/domain/model/{ContactMessage,EmailStatus}.java`, `domain/event/ContactMessageReceivedEvent.java`, portas, `application/ContactService.java`

#### Commit 47
```
feat(api): add rate limiting and async email delivery with retry
```
**Objetivo.** Bucket4j + envio `AFTER_COMMIT` assíncrono + job de reprocessamento — o *transactional outbox* em sua forma mais simples.
**Arquivos.** `shared/web/RateLimitInterceptor.java`, `adapter/out/email/{ResendEmailAdapter,ContactEmailListener}.java`, `adapter/in/scheduler/FailedEmailRetryJob.java`, `templates/email/contact-notification.html`

#### Commit 48
```
feat(api): expose POST /api/v1/contact with bean validation
```
**Arquivos.** `adapter/in/web/ContactController.java`, `dto/ContactRequest.java`, `shared/error/RateLimitExceededException.java`

#### Commit 49
```
feat(web): add accessible contact form with turnstile and honeypot
```
**Objetivo.** Formulário com três camadas de validação, estados anunciados a leitores de tela e BFF escondendo os segredos.
**Arquivos.** `src/app/api/contact/route.ts`, `src/features/contact/components/{ContactForm,ContactInfo,ContactSection}.tsx`, `actions/submit-contact.ts`, `schemas/contact-schema.ts`

#### Definition of Done — MVP 5

- [ ] Mensagem persiste mesmo com o provedor de e-mail fora do ar (testar desligando o mock)
- [ ] Reprocessamento de `FAILED` funciona
- [ ] 429 com `Retry-After` correto ao exceder o rate limit
- [ ] Erros associados aos campos via `aria-describedby`
- [ ] Sucesso anunciado em região `aria-live`
- [ ] Honeypot e Turnstile ativos
- [ ] Funciona sem JavaScript (Server Action com fallback nativo)
- [ ] E-mail chega de verdade na caixa de entrada, com `Reply-To` correto
- [ ] Teste de integração cobrindo caminho feliz e rate limit
- [ ] Deploy em produção · Tag `v0.5.0`

**Risco específico.** Entregabilidade de e-mail (cair em spam). Mitigação: Resend com domínio verificado (SPF/DKIM); testar com Gmail, Outlook e um domínio corporativo antes de considerar pronto.

---

### MVP 6 — Alcance e qualidade *(commits 50–54)*

**Objetivo.** Tornar o portfólio descobrível e fechar os números do orçamento de qualidade.

**Por que agora, e não antes.** Otimizar antes de o site estar completo é otimizar o que ainda vai mudar. SEO e performance são medidos sobre o produto final.

**Entregável.** Site indexável, bilíngue, com Lighthouse ≥ 95, zero violações de acessibilidade e documentação completa.

**Esforço estimado:** ~14h

#### Commit 50
```
feat(web): add metadata, structured data, sitemap and robots
```
**Objetivo.** Metadata por rota, JSON-LD (`Person`, `WebSite`, `BreadcrumbList`, `SoftwareSourceCode`), sitemap dinâmico e OG global.
**Arquivos.** `src/lib/metadata.ts`, `src/components/seo/{JsonLd.tsx,structured-data.ts}`, `src/app/{sitemap,robots,manifest}.ts`, `src/app/opengraph-image.tsx`

#### Commit 51
```
feat(web): add i18n support for pt-br and en-us with hreflang
```
**Arquivos.** `src/i18n/{routing,request}.ts`, `messages/{pt-BR,en-US}.json`, `middleware.ts`, `src/components/common/LocaleSwitcher.tsx`

#### Commit 52
```
test(web): add end-to-end and accessibility test suites
```
**Objetivo.** Playwright + axe-core, com zero violações em todas as rotas, em 3 navegadores.
**Arquivos.** `playwright.config.ts`, `e2e/{home,projects,contact,accessibility}.spec.ts`, `vitest.config.ts`

#### Commit 53
```
perf(web): optimize bundle, images and add lighthouse ci budget
```
**Objetivo.** Fechar o orçamento de performance e **travá-lo no CI** — a partir daqui, regressão quebra o PR.
**Arquivos.** `next.config.ts`, `lighthouserc.json`, `.github/workflows/lighthouse.yml`, imports dinâmicos nos componentes de motion

#### Commit 54
```
docs: add architecture diagrams, adrs and complete readme
```
**Objetivo.** É o primeiro contato do avaliador com o repositório — e por isso é o último commit, quando tudo já é verdade.
**Arquivos.** `README.md`, `docs/PLANO-COMPLETO.md`, `docs/diagrams/*.svg`, `docs/reports/*` (Lighthouse, k6, cobertura), `CONTRIBUTING.md`

#### Definition of Done — MVP 6

- [ ] Lighthouse ≥ 95 nas 4 categorias, mobile, **medido em produção**
- [ ] Core Web Vitals no verde (LCP < 2.0s, INP < 200ms, CLS < 0.1)
- [ ] Zero violações do axe em todas as rotas
- [ ] Bundle inicial < 100 KB comprimido
- [ ] Rich Results Test sem erros
- [ ] Sitemap submetido no Search Console
- [ ] `hreflang` recíproco e com `x-default`
- [ ] Auditoria manual com NVDA documentada
- [ ] k6: p95 < 200ms com 100 VUs, relatório versionado
- [ ] README com diagrama, badges e link do ambiente
- [ ] Tag `v1.0.0` 🎉

---

## 17. Riscos, checklists e aprovação

### 17.1 Riscos

| Risco | Prob. | Impacto | Mitigação |
|-------|-------|---------|-----------|
| **Escopo inflar e o projeto nunca ser publicado** | Alta | **Crítico** | **Resolvido pela estrutura de MVPs:** o deploy está no MVP 1 (commit 23). A partir dali existe um link em produção. |
| Cold start do free tier faz o avaliador achar que o site está fora do ar | Alta | Alto | Site é SSG/ISR — o visitante nunca espera a API. Cron de ping a cada 10 min. |
| Rate limit da API do GitHub | Média | Médio | Token + cache de 6h + `@Scheduled` de refresh + fallback em cache |
| Perfeccionismo de design travar o desenvolvimento | Média | Alto | Design system congelado no MVP 1 e tratado como contrato imutável na v1 |
| Conteúdo (bio, cases) não estar pronto | Média | Médio | Seeds realistas desde o MVP 1; substituição é uma migration |
| E-mail do contato cair em spam | Média | Alto | Domínio verificado com SPF/DKIM; teste em Gmail, Outlook e domínio corporativo |
| Lighthouse não atingir 95 no MVP 6 | Baixa | Médio | Bundle analyzer desde o MVP 1; cortar animações antes de cortar conteúdo |
| Custo inesperado em algum serviço | Baixa | Médio | Apenas free tiers; alertas de billing; sem cartão onde não é exigido |

### 17.2 Definition of Done global do projeto

O projeto está pronto (v1.0.0) quando **todos** forem verdadeiros:

- [ ] Lighthouse ≥ 95 nas 4 categorias, em mobile, medido no domínio de produção
- [ ] Core Web Vitals no verde: LCP < 2.0s, INP < 200ms, CLS < 0.1
- [ ] Zero violações do axe-core em todas as páginas
- [ ] Navegação completa por teclado, sem armadilhas de foco
- [ ] Cobertura de testes do backend ≥ 80% de linhas em `domain` e `application`
- [ ] Suíte de integração com Testcontainers verde no CI
- [ ] ArchUnit garantindo as regras de dependência
- [ ] Pipeline de CI verde em todo commit da `main`
- [ ] OpenAPI publicado e acessível
- [ ] Zero vulnerabilidades `HIGH`/`CRITICAL` no Trivy e no OWASP Dependency-Check
- [ ] README com diagrama de arquitetura, instruções de execução local e link do ambiente
- [ ] Histórico em Conventional Commits, sem commits do tipo "wip" ou "fix"

### 17.3 Regras do histórico de commits

1. **Todo commit deixa o build verde.** Nenhum "conserta o commit anterior".
2. **Sem commits de manutenção** ("wip", "fix", "ajustes"). Se precisar corrigir antes de publicar, use `git commit --amend` ou rebase interativo.
3. **Corpo do commit explica o porquê** quando a decisão não é óbvia pelo diff.
4. **Um commit por conceito.** Refatoração e feature nunca no mesmo commit.
5. **Ordem importa.** O histórico é lido de cima para baixo por quem avalia.
6. **Sem force push na `main`** após a publicação.

**Verificação antes do lançamento:**

```bash
# Todos os commits seguem o padrão?
npx commitlint --from=$(git rev-list --max-parents=0 HEAD) --to=HEAD

# O histórico conta uma história legível?
git log --oneline --graph

# Nenhum commit gigante?
git log --shortstat --oneline | grep -E "[0-9]{4,} insertions"

# Nenhum segredo vazado no histórico?
gitleaks detect --source . --verbose
```

### 17.4 Backlog pós-v1.0

| Item | Versão | Valor |
|------|--------|-------|
| Blog técnico com MDX | v2.0 | SEO de cauda longa e autoridade |
| Painel administrativo | v2.0 | Editar conteúdo sem migration |
| Redis em cache distribuído | v1.1 | Só se houver múltiplas instâncias |
| Rastreamento distribuído (OpenTelemetry) | v1.1 | Observabilidade avançada |
| Testes de contrato (Pact) | v1.1 | Garantia entre web e api |
| Modo de apresentação para entrevista | v1.2 | Tour guiado das decisões técnicas |
| Página `/uses` | v1.2 | Conteúdo leve, bom para SEO |
| RSS | v2.0 | Depende do blog |

### 17.5 Aprovação — o que preciso de você

Este plano precisa de aprovação explícita antes do início da FASE 2 (desenvolvimento).

1. **Aprovado integralmente**, ou com ressalvas em quais seções?
2. **Nome de domínio** pretendido — afeta canonical, OG e `hreflang`
3. **Usuário do GitHub** e projetos que devem aparecer em destaque
4. **Conteúdo real** (bio, timeline profissional, skills) — você fornece ou proponho um rascunho com seeds realistas?

Ao aprovar, começo pelo **Commit 01** e paro após cada etapa, informando objetivo, arquivos criados, arquivos alterados, justificativa técnica, próximo passo e a mensagem de commit — seguida de:

```bash
git add .
git commit -m "mensagem-do-commit"
git push origin main
```

---

**Fim do documento.** Versão 2.0 · 54 commits · 7 MVPs · ~100h estimadas.







