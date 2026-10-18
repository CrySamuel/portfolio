# Como contribuir

Este repositório é também o entregável: quem avalia o projeto lê o `git log`, os testes e os ADRs
antes de abrir o site. Por isso quase toda regra abaixo é verificada por máquina — o que depende de
lembrar acaba esquecido.

Para subir o ambiente, veja [Como executar](README.md#como-executar).

## Commits

[Conventional Commits](https://www.conventionalcommits.org/), validados pelo `commitlint` no hook
`commit-msg`:

```
<tipo>(<escopo>): <assunto em inglês, no imperativo>

<corpo em português: por que a mudança existe, o que foi medido,
o que foi descartado e por quê>
```

- **Tipos:** `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `style`, `build`, `ci`, `chore`,
  `revert`.
- **Escopos:** `web`, `api`, `ui`, `db`, `infra`, `docs`, `deps` — ou nenhum, para o que é da raiz
  do monorepo.
- **Título** com até 72 caracteres, sem ponto final, sem maiúscula inicial.
- **Corpo** com linhas de até 100 caracteres. O diff já mostra _o quê_; o corpo conta _por quê_.
- ⚠️ **Nenhuma linha do corpo pode começar com `palavra:`** — o parser a lê como rodapé e o hook
  recusa o commit. Quebre a linha em outro ponto.

Um commit deixa o build verde e conta uma mudança inteira: sem "wip", sem "ajustes", sem commit que
só conserta o anterior (seção 17.3 do [plano](docs/PLANO-COMPLETO.md)).

## Hooks

Instalados pelo `pnpm install` (Husky). **Não pule nenhum com `--no-verify`**: eles são a mesma
barreira que o CI, só que antes de o código sair da máquina.

| Hook         | O que roda                                                                                      |
| ------------ | ----------------------------------------------------------------------------------------------- |
| `pre-commit` | ESLint e Prettier nos arquivos alterados; Spotless se algum `.java` mudou                       |
| `commit-msg` | commitlint                                                                                      |
| `pre-push`   | os testes do web e a suíte inteira da API — **com Docker rodando**, por causa do Testcontainers |

## O que o CI cobra

A tabela [O que reprova uma mudança](README.md#o-que-reprova-uma-mudança) lista cada guarda. As que
mais pegam quem chega:

- **Arquitetura:** o domínio não importa Spring nem JPA, a aplicação não conhece adaptador, e um
  módulo não conhece outro. O ArchUnit reprova a violação com o nome da regra.
- **Cobertura:** cada pacote de `domain` e de `application` com pelo menos 80% das linhas. Código
  novo de regra de negócio chega com teste.
- **Performance:** no máximo 130 KB de JavaScript comprimido por página, e os limites do Lighthouse
  em [`apps/web/lighthouserc.json`](apps/web/lighthouserc.json). Uma dependência nova no cliente
  precisa caber no orçamento.
- **Acessibilidade:** zero violações do axe em todas as rotas, nos quatro navegadores do E2E.

## Guardas novas

Uma regra que nunca falhou não prova que funciona. Antes de confiar numa guarda nova — um limite,
uma regra de lint, um teste de arquitetura —, quebre-a de propósito, **escreva antes o que deve
acontecer**, confira que aconteceu exatamente isso e só então restaure. Se o resultado divergir da
previsão, a guarda ainda não está entendida.

## Decisões de arquitetura

Escolha que não é óbvia pelo código vira ADR na [seção 14 do plano](docs/PLANO-COMPLETO.md#14-decisões-arquiteturais-adrs):
contexto, alternativas, decisão e consequências — inclusive as negativas. Decisão revista não é
apagada: o ADR novo diz qual substitui ou emenda.

## Conteúdo do site

O conteúdo mora no banco, não no código. Adicionar ou mudar um projeto é editar a migração
repetível `apps/api/src/main/resources/db/migration/R__seed_projects.sql`, nos dois idiomas; o
Flyway a reaplica no próximo deploy da API, e o site se atualiza na revalidação seguinte.

## Segredos

Nenhum segredo entra no repositório. Os nomes estão em [`.env.example`](.env.example); os valores
moram nos painéis da Vercel, do Render e do GitHub. Uma chave que passou por um commit, um log ou
uma conversa é considerada vazada e deve ser trocada.
