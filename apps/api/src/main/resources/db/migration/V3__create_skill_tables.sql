-- Competencias agrupadas por categoria (secao 3.7 do plano).
--
-- Terceira migracao versionada. Duas tabelas num arquivo so porque elas nascem
-- juntas e uma nao faz sentido sem a outra: skill sem categoria nao tem onde
-- aparecer, e categoria sem skill e um cabecalho vazio.

CREATE TABLE skill_category (
    id            BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- Chave natural, e e sobre ela que o ON CONFLICT do seed repetivel vai
    -- decidir entre inserir e atualizar - a mesma razao do `singleton` em
    -- profile e da tripla em experience.
    name          VARCHAR(60) NOT NULL,

    -- Aqui a ordem **precisa** ser guardada, e a diferenca em relacao a
    -- experience e o ponto. La a ordem sai de start_date, entao uma coluna
    -- seria segunda fonte de verdade; aqui nao ha nada no dado de onde deduzir
    -- que "Linguagens" vem antes de "Infraestrutura" - a escolha e editorial, e
    -- o unico lugar onde ela pode morar e uma coluna.
    display_order SMALLINT    NOT NULL DEFAULT 0,

    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT skill_category_name_uk UNIQUE (name)
);

COMMENT ON TABLE skill_category IS 'Agrupamento editorial das competencias.';
COMMENT ON COLUMN skill_category.display_order IS 'Ordem de exibicao; nao e derivavel do dado.';

CREATE TABLE skill (
    id                  BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- ON DELETE CASCADE porque a competencia nao existe fora de uma categoria.
    -- A integridade fica declarada aqui e nao depende de o adaptador lembrar.
    category_id         BIGINT      NOT NULL REFERENCES skill_category (id) ON DELETE CASCADE,

    name                VARCHAR(60) NOT NULL,

    -- Texto, e nao tipo ENUM do Postgres. O enum de verdade vive no dominio
    -- (commit 30); aqui guarda-se o codigo em minusculo, que e o mesmo formato
    -- que o front recebe - a mesma escolha de social_link.platform.
    --
    -- O tipo ENUM nativo foi recusado por um motivo pratico: acrescentar valor a
    -- ele e DDL, e remover exige recriar o tipo inteiro. Com VARCHAR + CHECK, a
    -- lista de valores permitidos e uma linha de migracao.
    --
    -- **O CHECK nao e redundante com o enum Java.** Ele guarda a coluna contra
    -- escrita que nao passe pela aplicacao - e toda escrita deste projeto e
    -- assim, porque conteudo entra por migracao (ADR-0004). Sem ele, um seed com
    -- 'avancado' em vez de 'advanced' gravaria em silencio e so quebraria na
    -- leitura, longe da causa.
    proficiency         VARCHAR(20) NOT NULL,

    -- Nulavel de proposito: nem toda competencia tem um numero honesto a
    -- declarar. A secao 3.7 pede o campo, e a F05 o exibe ao lado do rotulo
    -- textual - quando ele existe. Inventar "1 ano" para preencher seria pior do
    -- que omitir.
    years_of_experience SMALLINT,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Tres niveis, e nao cinco. A F05 ja recusa barra de percentual por ser
    -- arbitraria e indefensavel em entrevista; escala longa demais tem o mesmo
    -- defeito, so que disfarcado de precisao. Com tres, cada rotulo significa
    -- alguma coisa e "avancado" continua custando caro.
    CONSTRAINT skill_proficiency_ck
        CHECK (proficiency IN ('basic', 'intermediate', 'advanced')),

    -- Tempo negativo e dado impossivel; zero e legitimo para quem comecou agora.
    CONSTRAINT skill_years_ck
        CHECK (years_of_experience IS NULL OR years_of_experience >= 0),

    -- A mesma competencia nao se repete dentro da categoria. Entre categorias
    -- pode: "SQL" cabe em Bancos de Dados e em Linguagens, e forcar escolha
    -- unica seria o schema opinando sobre taxonomia.
    --
    -- O nome inclui as duas colunas, e nao por gosto: restricao unica cria
    -- indice, e indice compartilha namespace com todos os outros do schema -
    -- chamar esta de skill_category_name_uk colidiria com a de skill_category.
    CONSTRAINT skill_category_id_name_uk UNIQUE (category_id, name)
);

COMMENT ON TABLE skill IS 'Competencia tecnica, sempre dentro de uma categoria.';
COMMENT ON COLUMN skill.proficiency IS 'Codigo em minusculo: basic, intermediate ou advanced.';
COMMENT ON COLUMN skill.years_of_experience IS 'Nulo quando nao ha numero honesto a declarar.';

-- A leitura real e sempre "as competencias desta categoria". O indice cobre a
-- clausula do join.
--
-- Nao ha coluna de ordem aqui, e a diferenca em relacao a skill_category e
-- proposital: dentro de uma categoria a ordem sai do proprio dado - nivel
-- primeiro, nome para desempatar -, e quem a estabelece e o dominio, no commit
-- 30. Guardar tambem uma coluna criaria duas fontes de verdade para a mesma
-- ordem, que e o erro que a timeline evitou.
CREATE INDEX skill_category_id_idx ON skill (category_id);

-- Nao ha indice em skill_category: a tabela tem meia duzia de linhas, e indice
-- em tabela desse tamanho custa escrita sem economizar leitura - o planejador
-- faz seq scan de qualquer jeito. Mesmo raciocinio que dispensou indice em
-- profile.
