# Relatórios

Medições pontuais, versionadas com data, commit e método — para que cada número citado no
[README](../../README.md) possa ser conferido e refeito.

| Relatório                   | O que mede                                       | Resultado principal                                              |
| --------------------------- | ------------------------------------------------ | ---------------------------------------------------------------- |
| [Lighthouse](lighthouse.md) | Performance, acessibilidade, boas práticas e SEO | 97–100 de performance e 100 no resto, em produção, no celular    |
| [Cobertura](cobertura.md)   | Linhas e ramos cobertos pela suíte da API        | 92,8% das linhas; 97,5% no núcleo                                |
| [Carga](carga.md)           | Latência da API de produção sob 100 usuários     | p95 de 1,4 s com cache (era 23,6 s); alvo de 200 ms não cumprido |

As medições contínuas — orçamento de JS, limites do Lighthouse e piso de cobertura — rodam no CI a
cada mudança e reprovam o workflow; estes relatórios registram o estado num ponto do tempo.
