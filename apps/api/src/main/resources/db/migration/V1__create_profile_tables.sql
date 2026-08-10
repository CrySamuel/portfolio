-- Perfil e links sociais (secao 3.7 do plano).
--
-- Primeira migracao do projeto: e ela que cria a tabela flyway_schema_history e
-- estabelece o schema como algo versionado. Nao existe ddl-auto em lugar nenhum
-- da configuracao, e essa ausencia e deliberada (ADR-0004): o Hibernate nunca
-- altera a estrutura: ele so a valida, a partir do commit 18.

CREATE TABLE profile (
    -- IDENTITY, e nao SERIAL. SERIAL e sintaxe legada do Postgres: cria uma
    -- sequence solta, cujo dono e o default da coluna, e que sobrevive ao DROP
    -- TABLE. IDENTITY e SQL padrao e a sequence pertence a coluna.
    --
    -- ALWAYS impede INSERT com id explicito sem OVERRIDING SYSTEM VALUE. E o que
    -- garante que o seed nao fixe chaves - ele referencia a linha por consulta.
    id                 BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    full_name          VARCHAR(120) NOT NULL,
    headline           VARCHAR(180) NOT NULL,
    bio                TEXT         NOT NULL,

    -- Nulavel de proposito: sao dados pessoais que so o dono do portfolio pode
    -- informar. Coluna NOT NULL obrigaria o seed a inventar um valor, e dado
    -- inventado sobre uma pessoa real e pior do que ausencia de dado.
    location           VARCHAR(120),
    resume_url         VARCHAR(500),

    -- Default conservador: quem nao declarou disponibilidade nao esta anunciando
    -- disponibilidade.
    available_for_work BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- O portfolio tem um dono, entao a tabela tem uma linha. Sem esta restricao,
    -- um seed executado duas vezes criaria um segundo perfil e a API passaria a
    -- escolher em silencio qual dos dois devolver.
    --
    -- O truque e o par NOT NULL + CHECK (singleton) + UNIQUE: a coluna so aceita
    -- TRUE, e TRUE so cabe uma vez. A regra fica no banco, que e onde ela
    -- sobrevive a qualquer bug de aplicacao.
    singleton          BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT profile_singleton_ck CHECK (singleton),
    CONSTRAINT profile_singleton_uk UNIQUE (singleton)
);

COMMENT ON TABLE profile IS 'Perfil publico do portfolio. Sempre exatamente uma linha.';
COMMENT ON COLUMN profile.singleton IS 'Coluna tecnica: existe apenas para restringir a tabela a uma linha.';

CREATE TABLE social_link (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- ON DELETE CASCADE porque o link nao existe sem o perfil. A integridade
    -- fica declarada aqui e nao depende de o adaptador lembrar de apagar.
    profile_id    BIGINT       NOT NULL REFERENCES profile (id) ON DELETE CASCADE,

    platform      VARCHAR(40)  NOT NULL,
    url           VARCHAR(500) NOT NULL,
    display_order SMALLINT     NOT NULL DEFAULT 0,

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Uma conta por plataforma. E tambem a chave que torna o seed idempotente:
    -- e sobre ela que o ON CONFLICT do R__seed_profile decide entre inserir e
    -- atualizar.
    CONSTRAINT social_link_profile_platform_uk UNIQUE (profile_id, platform)
);

-- A leitura real e sempre "os links deste perfil, na ordem de exibicao". O
-- indice cobre a clausula e a ordenacao de uma vez.
--
-- Nao ha indice equivalente em profile de proposito: a tabela tem uma linha, e
-- indice em tabela de uma linha custa escrita sem economizar leitura - o
-- planejador faz seq scan de qualquer jeito.
CREATE INDEX social_link_profile_id_display_order_idx
    ON social_link (profile_id, display_order);

COMMENT ON TABLE social_link IS 'Perfis externos do dono do portfolio (GitHub, LinkedIn, e-mail).';
