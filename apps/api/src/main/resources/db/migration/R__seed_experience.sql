-- Conteudo da timeline profissional.
--
-- Migracao REPETIVEL, pela mesma razao do R__seed_profile: a secao 3.7 exige o
-- seed idempotente, e atualizar o portfolio passa a ser editar este arquivo e
-- fazer deploy. Nao ha migracao nova a cada correcao de texto, e o historico da
-- mudanca fica no git, que e onde se procura.
--
-- A obrigacao que vem junto: rodar duas vezes tem de deixar o banco exatamente
-- no mesmo estado.
--
-- Conteudo informado pelo dono, a partir do curriculo dele. Duas observacoes
-- sobre fidelidade, porque as duas foram decididas e nao inferidas:
--
--   1. Na Stefanini o cargo era suporte tecnico - hardware e instalacao de
--      software. As automacoes em Python foram feitas por iniciativa propria,
--      fora das atribuicoes, e o texto diz isso. O curriculo em PDF as enquadra
--      como atribuicao do cargo; aqui nao, porque tecnico de suporte que escreve
--      codigo para resolver o proprio problema e uma informacao melhor do que
--      "ja era desenvolvedor", alem de ser a verdadeira.
--   2. O destaque publica o caso concreto - quatro horas viraram duas - em vez do
--      agregado de "aproximadamente 40%" que o PDF traz. Os dois convivem, um e
--      media e o outro e um caso, mas afirmar 50% no agregado contradiria o
--      curriculo de quem le os dois lado a lado.

-- A lista `desejadas` e a fonte de verdade: o que esta nela existe no banco, o
-- que nao esta e removido. Sem o DELETE, o seed saberia acrescentar e corrigir
-- mas nunca tirar, e a frase "editar este arquivo atualiza o portfolio" seria
-- falsa justamente no caso em que mais importa, o de despublicar uma posicao.
--
-- O DELETE mora numa CTE porque o PostgreSQL executa CTE que modifica dados
-- exatamente uma vez e ate o fim, referenciada ou nao. Assim a lista aparece uma
-- vez so no arquivo.
--
-- A chave da comparacao e a tripla (company, role, start_date), a mesma que a
-- V2 declara unica - e e por existir essa chave natural que o upsert e possivel
-- sem DELETE geral, preservando os ids.
--
-- Os tipos vao explicitos no VALUES porque o PostgreSQL infere o tipo da coluna
-- da CTE pela primeira linha: sem o DATE e o ::jsonb, as datas viriam como texto
-- e a comparacao com a coluna date falharia.
WITH desejadas (company, role, start_date, end_date, description, highlights) AS (
    VALUES
        (
            'DXC Technology (FEMSA Coca-Cola)',
            'Técnico de Suporte Júnior',
            DATE '2026-01-01',
            -- Nulo: posicao atual. E daqui que sai o badge "Atual" da interface.
            NULL::date,
            'Sustentação de ambiente corporativo de Data Center em regime de missão '
                || 'crítica, com foco em diagnóstico de incidentes complexos, conformidade '
                || 'de SLA e padronização do conhecimento técnico da equipe.',
            '[
               "Diagnóstico e resolução de incidentes de alta complexidade em Data Center, garantindo a disponibilidade contínua dos serviços",
               "Monitoramento de conformidade de SLA, reduzindo tempos de resposta por análise técnica e identificação de causa raiz",
               "Criação e manutenção da base de conhecimento, padronizando a resolução de falhas de infraestrutura para toda a equipe"
             ]'::jsonb
        ),
        (
            'Stefanini (Vivo)',
            'Técnico de Suporte N1',
            DATE '2025-03-01',
            DATE '2025-12-31',
            'Suporte técnico N1 ao ambiente corporativo da Vivo: manutenção de hardware, '
                || 'instalação de software e administração de acessos em estações de '
                || 'trabalho. As automações em Python partiram de iniciativa própria, para '
                || 'encurtar o tempo dos próprios atendimentos.',
            '[
               "Automação em Python de rotinas do próprio atendimento, por iniciativa própria: chamados de quatro horas passaram a levar duas",
               "Manutenção de hardware e instalação de software em estações de trabalho, com foco na estabilidade do ambiente e na experiência do usuário",
               "Administração de identidades, acessos e políticas de segurança via Active Directory"
             ]'::jsonb
        )
),
removidas AS (
    DELETE FROM experience e
    WHERE NOT EXISTS (
        SELECT 1
        FROM desejadas d
        WHERE d.company = e.company
          AND d.role = e.role
          AND d.start_date = e.start_date
    )
)
INSERT INTO experience (company, role, start_date, end_date, description, highlights)
SELECT company, role, start_date, end_date, description, highlights
FROM desejadas
ON CONFLICT (company, role, start_date) DO UPDATE SET
    end_date    = EXCLUDED.end_date,
    description = EXCLUDED.description,
    highlights  = EXCLUDED.highlights,
    updated_at  = now();

-- Nao ha ORDER BY aqui, e a ausencia e proposital: a ordem de insercao nao
-- significa nada para a leitura. Quem ordena a timeline e o Timeline do dominio,
-- e semear em ordem daria a impressao falsa de que este arquivo participa disso.

-- ---------------------------------------------------------------------------
-- Traducao en-US (ADR-0013).
--
-- Mora neste arquivo, e nao num seed so de traducoes, por causa de como o
-- Flyway roda repetiveis: so quando o proprio arquivo muda. Num arquivo
-- separado, uma correcao aqui que recriasse uma linha apagaria a traducao dela
-- em cascata, e o seed das traducoes - sem mudanca - nao rodaria de novo. Junto,
-- quem muda o original reaplica a traducao no mesmo passo.
--
-- O mesmo padrao do resto do arquivo: a lista e a fonte de verdade nos dois
-- sentidos, com upsert pela chave e DELETE do que saiu dela. A chave do
-- original e a natural, e nao o id, que o seed nunca conhece.
-- ---------------------------------------------------------------------------

WITH traducoes (company, role, start_date, role_en, description, highlights) AS (
    VALUES
        (
            'DXC Technology (FEMSA Coca-Cola)',
            'Técnico de Suporte Júnior',
            DATE '2026-01-01',
            'Junior Support Technician',
            'Supporting a mission-critical corporate Data Center environment, focused on diagnosing complex incidents, SLA compliance and standardizing the team''s technical knowledge.',
            '["Diagnosed and resolved high-complexity Data Center incidents, keeping services continuously available", "Monitored SLA compliance, cutting response times through technical analysis and root cause identification", "Built and maintained the knowledge base, standardizing how infrastructure failures are resolved across the team"]'::jsonb
        ),
        (
            'Stefanini (Vivo)',
            'Técnico de Suporte N1',
            DATE '2025-03-01',
            'Tier 1 Support Technician',
            'Tier 1 technical support for Vivo''s corporate environment: hardware maintenance, software installation and access management on workstations. The Python automations were my own initiative, to shorten my own ticket handling time.',
            '["Automated my own support routines in Python, on my own initiative: four-hour tickets came down to two", "Hardware maintenance and software installation on workstations, focused on environment stability and user experience", "Identity, access and security policy administration through Active Directory"]'::jsonb
        )
),
removidas AS (
    DELETE FROM experience_translation t
    WHERE t.locale = 'en-US'
      AND NOT EXISTS (
        SELECT 1
        FROM traducoes d
        JOIN experience e
          ON e.company = d.company AND e.role = d.role AND e.start_date = d.start_date
        WHERE e.id = t.experience_id
    )
)
INSERT INTO experience_translation (experience_id, locale, role, description, highlights)
SELECT e.id, 'en-US', d.role_en, d.description, d.highlights
FROM traducoes d
JOIN experience e
  ON e.company = d.company AND e.role = d.role AND e.start_date = d.start_date
ON CONFLICT (experience_id, locale) DO UPDATE SET
    role        = EXCLUDED.role,
    description = EXCLUDED.description,
    highlights  = EXCLUDED.highlights;
