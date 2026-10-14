-- Traducoes do conteudo exibido, uma tabela por entidade com texto (ADR-0013).
--
-- O portugues continua nas tabelas originais, como idioma base: nenhuma coluna
-- existente muda de lugar, e a leitura em portugues segue exatamente como era.
-- Cada tabela daqui guarda o mesmo texto noutro idioma, e a API cai para o
-- original quando a linha traduzida nao existe.
--
-- Quatro decisoes valem para as seis tabelas, e ficam escritas uma vez:
--
-- 1. **A chave e o par (entidade, idioma), sem id proprio** - a mesma escolha de
--    project_tech. O par e a identidade da linha, entao traduzir duas vezes a
--    mesma coisa para o mesmo idioma fica impossivel por construcao. A PK tambem
--    serve a unica consulta que estas tabelas recebem: todas as traducoes de um
--    lote de entidades, pela coluna da esquerda.
--
-- 2. **O idioma aceita so 'en-US'.** 'pt-BR' fica de fora de proposito: ele e o
--    idioma das tabelas originais, e uma linha 'pt-BR' aqui seria segunda fonte
--    para o mesmo texto - as duas divergiriam na primeira correcao. Acrescentar
--    um idioma e ampliar o CHECK por migracao, e e assim que deve ser: idioma
--    novo e decisao, nao dado inserido.
--
-- 3. **Os limites de tamanho repetem os das colunas originais.** O resumo do
--    projeto tem 280 caracteres por causa do cartao de compartilhamento; a
--    traducao dele tem o mesmo cartao para caber. Foi esse o motivo de recusar
--    jsonb na propria linha, que nao guarda limite nenhum.
--
-- 4. **ON DELETE CASCADE em todas.** Traducao nao existe sem o original, e a
--    integridade fica declarada aqui em vez de depender de o seed lembrar.
--
-- Sem created_at e updated_at, como project_tech: nada le essas colunas, e
-- traducao e filha do original - a data que importa e a dele.

CREATE TABLE profile_translation (
    profile_id BIGINT       NOT NULL REFERENCES profile (id) ON DELETE CASCADE,
    locale     VARCHAR(10)  NOT NULL,
    headline   VARCHAR(180) NOT NULL,
    bio        TEXT         NOT NULL,

    -- Nulavel como no original, e com a mesma leitura: sem local traduzido, vale
    -- o local do original. "Remoto · Brasil" e "Remote · Brazil" diferem; uma
    -- cidade pode nao diferir, e ai a linha traduzida simplesmente nao o traz.
    location   VARCHAR(120),

    CONSTRAINT profile_translation_pk PRIMARY KEY (profile_id, locale),
    CONSTRAINT profile_translation_locale_ck CHECK (locale IN ('en-US'))
);

COMMENT ON TABLE profile_translation IS 'Perfil em outro idioma; o portugues fica em profile.';

CREATE TABLE experience_translation (
    experience_id BIGINT       NOT NULL REFERENCES experience (id) ON DELETE CASCADE,
    locale        VARCHAR(10)  NOT NULL,
    role          VARCHAR(120) NOT NULL,
    description   TEXT         NOT NULL,

    -- A mesma forma do original, e o mesmo CHECK: o jsonb aceita qualquer JSON,
    -- e um objeto aqui so quebraria na leitura, longe do seed que o gravou.
    highlights    JSONB        NOT NULL DEFAULT '[]'::jsonb,

    CONSTRAINT experience_translation_pk PRIMARY KEY (experience_id, locale),
    CONSTRAINT experience_translation_locale_ck CHECK (locale IN ('en-US')),
    CONSTRAINT experience_translation_highlights_is_array_ck
        CHECK (jsonb_typeof(highlights) = 'array')
);

COMMENT ON TABLE experience_translation IS 'Cargo, descricao e destaques em outro idioma.';

CREATE TABLE skill_category_translation (
    category_id BIGINT      NOT NULL REFERENCES skill_category (id) ON DELETE CASCADE,
    locale      VARCHAR(10) NOT NULL,
    name        VARCHAR(60) NOT NULL,

    CONSTRAINT skill_category_translation_pk PRIMARY KEY (category_id, locale),
    CONSTRAINT skill_category_translation_locale_ck CHECK (locale IN ('en-US')),

    -- O original e unico por nome, e a traducao e unica por nome dentro do
    -- idioma: duas categorias chamadas "Databases" na mesma pagina seriam o
    -- mesmo cabecalho duas vezes.
    CONSTRAINT skill_category_translation_locale_name_uk UNIQUE (locale, name)
);

COMMENT ON TABLE skill_category_translation IS 'Nome da categoria de competencia em outro idioma.';

-- A unica tabela em que a ausencia de traducao e o caso comum. Java, PostgreSQL
-- e Docker se chamam assim em qualquer idioma; so o que e descricao, e nao nome
-- proprio - "Integracao com LLMs" -, ganha linha aqui.
CREATE TABLE skill_translation (
    skill_id BIGINT      NOT NULL REFERENCES skill (id) ON DELETE CASCADE,
    locale   VARCHAR(10) NOT NULL,
    name     VARCHAR(60) NOT NULL,

    CONSTRAINT skill_translation_pk PRIMARY KEY (skill_id, locale),
    CONSTRAINT skill_translation_locale_ck CHECK (locale IN ('en-US'))
);

COMMENT ON TABLE skill_translation IS 'Nome da competencia em outro idioma, quando ele muda.';

CREATE TABLE project_translation (
    project_id BIGINT       NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    locale     VARCHAR(10)  NOT NULL,
    title      VARCHAR(120) NOT NULL,
    summary    VARCHAR(280) NOT NULL,
    problem    TEXT         NOT NULL,
    solution   TEXT         NOT NULL,
    outcome    TEXT         NOT NULL,

    CONSTRAINT project_translation_pk PRIMARY KEY (project_id, locale),
    CONSTRAINT project_translation_locale_ck CHECK (locale IN ('en-US'))
);

COMMENT ON TABLE project_translation IS 'Titulo e narrativa do projeto em outro idioma; o slug nao muda.';

-- O valor tambem e traduzivel, e o motivo e a unidade: "R$ 800+" e "24/7" nao
-- mudam, mas um valor como "3 meses" muda, e a coluna precisa comportar os dois.
CREATE TABLE project_metric_translation (
    metric_id BIGINT      NOT NULL REFERENCES project_metric (id) ON DELETE CASCADE,
    locale    VARCHAR(10) NOT NULL,
    label     VARCHAR(60) NOT NULL,
    value     VARCHAR(40) NOT NULL,

    CONSTRAINT project_metric_translation_pk PRIMARY KEY (metric_id, locale),
    CONSTRAINT project_metric_translation_locale_ck CHECK (locale IN ('en-US'))
);

COMMENT ON TABLE project_metric_translation IS 'Rotulo e valor da metrica em outro idioma.';
