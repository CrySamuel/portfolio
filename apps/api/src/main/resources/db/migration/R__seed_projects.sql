-- Conteudo do catalogo de projetos.
--
-- Migracao REPETIVEL, como os outros seeds: editar este arquivo e fazer deploy e
-- o jeito de atualizar o portfolio, e rodar duas vezes tem de deixar o banco no
-- mesmo estado.
--
-- Sao dois projetos, e a escolha do segundo foi do dono. O Music Style API
-- estava previsto e saiu depois de o repositorio ser conferido: o curriculo
-- afirmava Java 17, Bean Validation e PostgreSQL em producao, e o codigo tem
-- Java 23, nenhuma anotacao de validacao e H2 em memoria como unico ambiente.
-- Publicar o link mandaria o avaliador para um repositorio que desmente o PDF.
-- No lugar dele entrou este proprio portfolio, que e o artefato Java mais forte
-- que existe hoje e cuja cada afirmacao e verificavel.
--
-- A ordem dos comandos e imposta pela integridade, e nao por gosto:
--   1. technology entra (project_tech vai referencia-la)
--   2. project entra, e os projetos fora da lista saem com seus vinculos junto
--   3. project_tech e reescrito
--   4. so entao technology fora da lista pode sair - o ON DELETE RESTRICT do
--      vinculo recusaria antes do passo 3
--   5. project_metric e reescrito

-- As tecnologias. O slug alimenta a query string do filtro, entao ele nao pode
-- conter espaco nem acentuacao; o nome carrega a capitalizacao da marca, porque
-- marca escrita errada e ruido para quem avalia.
--
-- icon_slug fica nulo em todas. A divida esta registrada desde o MVP 2: logos
-- de linguagens e frameworks sao marcas registradas, cada projeto com politica
-- propria de uso, e desenhar aproximacoes ficaria pior do que nao ter icone.
INSERT INTO technology (name, slug, category) VALUES
    ('Python',         'python',         'language'),
    ('TypeScript',     'typescript',     'language'),
    ('Java',           'java',           'language'),
    ('SQLAlchemy',     'sqlalchemy',     'framework'),
    ('Spring Boot',    'spring-boot',    'framework'),
    ('Next.js',        'nextjs',         'framework'),
    ('PostgreSQL',     'postgresql',     'database'),
    ('Oracle Cloud',   'oracle-cloud',   'infrastructure'),
    ('Linux',          'linux',          'infrastructure'),
    ('Docker',         'docker',         'infrastructure'),
    ('GitHub Actions', 'github-actions', 'infrastructure'),
    ('Llama 3',        'llama-3',        'tool'),
    ('Testcontainers', 'testcontainers', 'tool')
ON CONFLICT (slug) DO UPDATE SET
    name       = EXCLUDED.name,
    category   = EXCLUDED.category,
    updated_at = now();

-- Os projetos.
--
-- O texto do FinAI foi escrito pelo dono e revisado com ele. Uma troca ficou
-- registrada: o original dizia "no ultimo mes", e pagina de portfolio e
-- permanente - referencia movel fica errada no mes seguinte. Ficou "em um mes".
--
-- **live_url do FinAI e NULL por decisao do dono, e nao por falta de endereco.**
-- O bot existe e responde. Publicar o link mudaria o perfil de quem chega ate
-- ele, de "quem o dono indicou" para "qualquer um que abra o site" - e cada
-- conversa consome cota da API da Groq com a chave dele e grava dado financeiro
-- de terceiro no VPS pessoal dele. A narrativa explica o que o bot e; quem
-- quiser ver o codigo tem o repositorio.
WITH desejados (slug, title, summary, problem, solution, outcome,
                repo_url, live_url, featured, display_order, published_at) AS (
    VALUES
        (
            'finai',
            'FinAI',
            'Assistente financeiro no Telegram que lê "gastei 50 no iFood no crédito" e devolve a transação categorizada, com aviso antes de o limite estourar.',
            'Controle financeiro em planilha não sobrevive à rotina: gasto não anotado na hora não é anotado nunca. O cartão saía do controle e o desafio financeiro do ano não passava do papel — não por falta de disciplina, mas porque registrar custava mais do que o registro valia.',
            'Um bot de Telegram que aceita a frase como ela sai da cabeça. O texto livre vai para o Llama 3 pela API da Groq e volta como JSON estruturado — valor, categoria, meio de pagamento. O SQLAlchemy cuida de cartões, limites e metas, e o sistema avisa antes do erro: "você já atingiu 80% do seu limite de lazer". Roda 24/7 numa instância Linux na Oracle Cloud.',
            'Mais de R$ 800 economizados em um mês, e um aporte mensal recorrente que antes não existia. O ganho não veio de anotar melhor — veio do aviso preventivo, o backend mudando comportamento antes do gasto em vez de relatar depois.',
            'https://github.com/CrySamuel/FinAI-Bot',
            NULL,
            TRUE,
            0::smallint,
            DATE '2026-03-24'
        ),
        (
            'portfolio',
            'Este portfólio',
            'O site que você está lendo: monorepo full-stack com API Java em produção, arquitetura hexagonal verificada por teste e o histórico de commits como parte do entregável.',
            'Currículo em PDF é formato de baixa largura de banda: mostra o que a pessoa alega ter feito, não como ela pensa. E perfil do GitHub é ruidoso — fork, exercício de curso e projeto abandonado misturados sem hierarquia. O gargalo de quem busca vaga de backend não é a habilidade, é demonstrá-la antes da entrevista técnica.',
            'Um portfólio que não afirma competência, exerce. O site em Next.js consome uma API Java 21 com Spring Boot 4 em produção, com PostgreSQL, Flyway e arquitetura hexagonal. O contrato OpenAPI gera o cliente TypeScript, então divergência entre as pontas quebra o build em vez de aparecer em produção. As regras de dependência não são promessa de README: são testes de ArchUnit que reprovam o build, e cada uma foi validada quebrando de propósito.',
            'Zero violações de acessibilidade medidas com axe-core nas quatro combinações de tema e largura, bundle inicial dentro do orçamento de 130 KB, e cada decisão registrada em ADR com a alternativa descartada e a consequência.',
            'https://github.com/CrySamuel/portfolio',
            'https://portfolio-web-coral-tau.vercel.app',
            TRUE,
            1::smallint,
            DATE '2026-08-09'
        )
),
-- Projeto fora da lista e removido, e os vinculos e as metricas dele vao junto
-- pelo ON DELETE CASCADE. Sem este DELETE, o seed saberia acrescentar e corrigir
-- mas nunca tirar - foi exatamente assim que o Music Style API saiu.
removidos AS (
    DELETE FROM project
    WHERE slug NOT IN (SELECT slug FROM desejados)
)
INSERT INTO project (slug, title, summary, problem, solution, outcome,
                     repo_url, live_url, featured, display_order, published_at)
SELECT slug, title, summary, problem, solution, outcome,
       repo_url, live_url, featured, display_order, published_at
FROM desejados
ON CONFLICT (slug) DO UPDATE SET
    title         = EXCLUDED.title,
    summary       = EXCLUDED.summary,
    problem       = EXCLUDED.problem,
    solution      = EXCLUDED.solution,
    outcome       = EXCLUDED.outcome,
    repo_url      = EXCLUDED.repo_url,
    live_url      = EXCLUDED.live_url,
    featured      = EXCLUDED.featured,
    display_order = EXCLUDED.display_order,
    published_at  = EXCLUDED.published_at,
    updated_at    = now();

-- Os vinculos, referenciando os dois lados por slug.
--
-- Os ids saem de consulta, e nao de constante: as duas colunas sao GENERATED
-- ALWAYS e o valor nao e conhecido por quem escreve o seed.
--
-- A escolha de quais tecnologias declarar e editorial, e menos e melhor: o chip
-- serve ao filtro, e uma lista longa demais transforma a secao numa nuvem de
-- palavras onde nada se destaca. Ficaram as que o projeto de fato exercita e que
-- alguem procuraria.
WITH desejados (projeto, tecnologia) AS (
    VALUES
        ('finai',     'python'),
        ('finai',     'sqlalchemy'),
        ('finai',     'llama-3'),
        ('finai',     'oracle-cloud'),
        ('finai',     'linux'),

        ('portfolio', 'java'),
        ('portfolio', 'spring-boot'),
        ('portfolio', 'postgresql'),
        ('portfolio', 'typescript'),
        ('portfolio', 'nextjs'),
        ('portfolio', 'docker'),
        ('portfolio', 'testcontainers'),
        ('portfolio', 'github-actions')
),
removidos AS (
    DELETE FROM project_tech pt
    WHERE NOT EXISTS (
        SELECT 1
        FROM desejados d
        JOIN project p    ON p.slug = d.projeto
        JOIN technology t ON t.slug = d.tecnologia
        WHERE p.id = pt.project_id
          AND t.id = pt.technology_id
    )
)
INSERT INTO project_tech (project_id, technology_id)
SELECT p.id, t.id
FROM desejados d
JOIN project p    ON p.slug = d.projeto
JOIN technology t ON t.slug = d.tecnologia
ON CONFLICT (project_id, technology_id) DO NOTHING;

-- Tecnologia fora da lista sai agora, e nao antes: ate o comando anterior ela
-- ainda podia estar vinculada, e o ON DELETE RESTRICT do vinculo recusaria a
-- remocao. A assimetria daquele RESTRICT existe para que "remover Docker do
-- catalogo" nunca apague o Docker dos projetos em silencio - aqui ela cobra o
-- preco dela, que e uma linha de ordem.
DELETE FROM technology
WHERE slug NOT IN (
    'python', 'typescript', 'java',
    'sqlalchemy', 'spring-boot', 'nextjs',
    'postgresql',
    'oracle-cloud', 'linux', 'docker', 'github-actions',
    'llama-3', 'testcontainers'
);

-- As metricas.
--
-- O MVP 3 se chama "prova de trabalho", e a secao 16 do plano escolhe o numero
-- como mitigacao do risco de escrever case generico: se nao ha numero, o case
-- ainda nao esta pronto.
--
-- **Os numeros do portfolio sao os estaveis de proposito.** A contagem de testes
-- seria o mais impressionante e foi recusada: ela muda a cada commit, e gravar
-- 392 aqui a deixaria errada no commit seguinte. Violacao de acessibilidade e
-- quantidade de ADR mudam raramente, e quando mudarem sao uma linha deste
-- arquivo.
WITH desejadas (projeto, label, value, display_order) AS (
    VALUES
        ('finai',     'Economia em um mês',          'R$ 800+', 0::smallint),
        ('finai',     'Investimento mensal',         'R$ 200+', 1::smallint),

        ('portfolio', 'Violações de acessibilidade', '0',       0::smallint),
        ('portfolio', 'Decisões em ADR',             '12',      1::smallint)
),
removidas AS (
    DELETE FROM project_metric pm
    WHERE NOT EXISTS (
        SELECT 1
        FROM desejadas d
        JOIN project p ON p.slug = d.projeto
        WHERE p.id = pm.project_id
          AND d.label = pm.label
    )
)
INSERT INTO project_metric (project_id, label, value, display_order)
SELECT p.id, d.label, d.value, d.display_order
FROM desejadas d
JOIN project p ON p.slug = d.projeto
ON CONFLICT (project_id, label) DO UPDATE SET
    value         = EXCLUDED.value,
    display_order = EXCLUDED.display_order,
    updated_at    = now();
