# Cobertura de testes da API

**Data:** 18/10/2026 · **Commit:** `e707c9d` · **Ferramenta:** JaCoCo 0.8.15, no `./mvnw verify` do
workflow **CI · api** (o relatório HTML completo sai como artefato `cobertura`)

A suíte tem **656 testes**: de unidade, de integração com PostgreSQL 16 real por Testcontainers, de
contrato HTTP com WireMock e de arquitetura com ArchUnit.

## Resultado

**92,8% das linhas** (1.631 de 1.758) e **81,8% dos ramos** (409 de 500).

O núcleo — os pacotes `domain` e `application`, onde mora a regra de negócio — separado da borda,
os adaptadores:

| Módulo     | Núcleo: linhas      | Núcleo: ramos       | Borda: linhas           | Borda: ramos        |
| ---------- | ------------------- | ------------------- | ----------------------- | ------------------- |
| `contact`  | 95,2% (80/84)       | 84,0% (42/50)       | 95,5% (150/157)         | 66,7% (12/18)       |
| `github`   | 96,7% (118/122)     | 90,5% (67/74)       | 83,0% (122/147)         | 46,2% (12/26)       |
| `profile`  | 97,9% (189/193)     | 92,4% (85/92)       | 89,1% (352/395)         | 67,6% (46/68)       |
| `projects` | 98,4% (120/122)     | 98,2% (55/56)       | 89,8% (274/305)         | 64,6% (31/48)       |
| `shared`   | 100% (40/40)        | 94,4% (17/18)       | 97,4% (185/190)         | 84,0% (42/50)       |
| **Total**  | **97,5%** (547/561) | **91,7%** (266/290) | **90,6%** (1.084/1.197) | **68,1%** (143/210) |

## O piso

O critério do plano é 80% das linhas em `domain` e `application`. O `verify` reprova se **qualquer
pacote** do núcleo ficar abaixo disso — por pacote, e não pela soma, porque um pacote pequeno e
descoberto some na média dos grandes.

Foi exatamente o que aconteceu na primeira execução: o pacote `contact.domain.event`, com 6 linhas,
estava em **66%**. As duas invariantes do evento de mensagem recebida — mensagem obrigatória e
identificador positivo — nunca tinham sido testadas, e somadas ao resto do núcleo passariam
despercebidas. Ganharam teste antes de o piso entrar.

A borda fica fora do piso por desenho: quem a cobre são os testes de integração, que sobem a
aplicação inteira. Os números mais baixos dela são records de desserialização da API do GitHub
(15%) e os dois agendadores (40% e 60%), cujo corpo é chamar um caso de uso já testado.

## Como reproduzir

```bash
cd apps/api && ./mvnw verify
```

Exige Docker rodando. O relatório sai em `apps/api/target/site/jacoco/index.html`.
