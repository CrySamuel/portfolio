# Portfólio — Crystofer Demetino

Portfólio profissional de Desenvolvedor Backend que **não apenas apresenta** competência técnica —
ele **é** a demonstração dela.

A maioria dos portfólios de backend é um site estático que _afirma_ domínio de Spring Boot, Docker e
testes. Este projeto adota a premissa oposta: **o portfólio é o próprio sistema backend**. O conteúdo
exibido na tela vem de uma API Java real, com banco de dados real, cache real, tratamento de falha
real e pipeline de deploy real.

> **Status:** MVP 0 — Fundação técnica. O projeto está em construção; este README evolui junto com ele.

---

## Arquitetura

```
Visitante ──HTTPS──▶ Web (Next.js 15)  ──HTTP──▶  API (Java 21 + Spring Boot 3.5)
                     SSG + ISR + BFF                        │
                     Vercel / CDN                           ├──▶ PostgreSQL 16
                                                            ├──▶ GitHub API
                                                            └──▶ Resend (e-mail)
```

- **Web** — Next.js 15 (App Router), React 19, TypeScript `strict`, Tailwind CSS 4. Renderização
  estática com ISR: o pico de tráfego é absorvido pela CDN e praticamente não toca a API.
- **API** — Java 21, Spring Boot 3.5, **arquitetura hexagonal** (Ports & Adapters) organizada em
  monólito modular por contexto de negócio. As regras de dependência são verificadas por ArchUnit —
  violá-las quebra o build.
- **Dados** — PostgreSQL 16 com migrações Flyway versionadas. Adicionar um projeto ao portfólio é
  escrever uma migration, não fazer um deploy de código.

## Stack

| Camada   | Tecnologias                                                                      |
| -------- | -------------------------------------------------------------------------------- |
| Web      | Next.js 15 · React 19 · TypeScript · Tailwind CSS 4 · Radix · Zod                |
| API      | Java 21 · Spring Boot 3.5 · Spring Data JPA · Resilience4j · Bucket4j · Caffeine |
| Dados    | PostgreSQL 16 · Flyway                                                           |
| Testes   | JUnit 5 · Testcontainers · WireMock · ArchUnit · Vitest · Playwright · axe-core  |
| Infra    | Docker · GitHub Actions · Vercel · Render                                        |
| Monorepo | pnpm workspaces · Turborepo                                                      |

## Estrutura

```
portfolio/
├── apps/
│   ├── web/          # Next.js 15 — site e BFF
│   └── api/          # Java 21 + Spring Boot — API REST
├── packages/
│   ├── ui/           # Design system (primitivos React)
│   ├── api-client/   # Cliente TypeScript gerado do OpenAPI
│   ├── eslint-config/
│   └── typescript-config/
├── infra/            # Docker, k6, scripts
├── docs/             # Plano, ADRs, diagramas e relatórios
└── .github/          # Workflows de CI/CD
```

A estrutura de pastas é documentação executável: a arquitetura deve ser inferível pelos nomes, sem
abrir um único arquivo.

## Pré-requisitos

| Ferramenta | Versão                                                               |
| ---------- | -------------------------------------------------------------------- |
| Node.js    | `24.16.0` (ver `.nvmrc`)                                             |
| pnpm       | `11.21.0`                                                            |
| JDK        | `21` (LTS)                                                           |
| Docker     | necessário para o Postgres local e para os testes com Testcontainers |

## Como executar

```bash
pnpm install
```

Sobe o Postgres local (os valores padrão já funcionam sem `.env`):

```bash
pnpm db:up
```

Web em `localhost:3000`:

```bash
pnpm --filter @portfolio/web dev
```

API em `localhost:8080`, com o perfil `local` ativado automaticamente:

```bash
cd apps/api && ./mvnw spring-boot:run
```

| Comando                 | O que faz                                    |
| ----------------------- | -------------------------------------------- |
| `pnpm db:up`            | Sobe o Postgres e espera ficar saudável      |
| `pnpm db:down`          | Para o container, preservando os dados       |
| `pnpm db:reset`         | Para o container e **apaga o volume**        |
| `pnpm lint`             | ESLint em todos os pacotes                   |
| `pnpm typecheck`        | `tsc --noEmit`                               |
| `pnpm format`           | Prettier                                     |
| `./mvnw spotless:apply` | Formata o código Java (dentro de `apps/api`) |

Para variáveis de ambiente, copie [.env.example](.env.example) para `.env`.

## Documentação

- [Plano completo do projeto](docs/PLANO-COMPLETO.md) — visão, arquitetura, stack, design system,
  ADRs e o roteiro de execução em 7 MVPs.

## Roadmap

| MVP | Entregável                                                            | Status          |
| --- | --------------------------------------------------------------------- | --------------- |
| 0   | Fundação técnica — monorepo com CI verde                              | 🚧 em andamento |
| 1   | **Portfólio publicado** — link em produção com hero vindo do Postgres | ⏳              |
| 2   | Perfil completo — sobre, timeline, skills e CV                        | ⏳              |
| 3   | Prova de trabalho — projetos com filtro e páginas de detalhe          | ⏳              |
| 4   | Prova de engenharia backend — GitHub com circuit breaker e cache      | ⏳              |
| 5   | Conversão — formulário de contato ponta a ponta                       | ⏳              |
| 6   | Alcance e qualidade — SEO, i18n, testes e performance                 | ⏳              |

## Licença

[MIT](LICENSE) © 2026 Crystofer Demetino
