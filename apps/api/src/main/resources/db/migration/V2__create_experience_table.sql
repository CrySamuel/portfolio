-- Timeline profissional (secao 3.7 do plano).
--
-- Segunda migracao versionada, e a primeira tabela que o commit 16 reservou ao
-- mandar o seed para um R__ repetivel em vez de gastar o V2 com ele.
--
-- A tabela nao se liga a profile por chave estrangeira, e a ausencia e
-- deliberada: profile e singleton por construcao, entao uma coluna profile_id
-- aqui repetiria em toda linha um valor que so pode ser um. A relacao existe no
-- dominio, nao precisa de coluna para existir no banco.

CREATE TABLE experience (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    company     VARCHAR(120) NOT NULL,

    -- `role` e palavra-chave nao reservada no PostgreSQL, entao vale como nome
    -- de coluna sem aspas. Mantido porque e o nome que a secao 3.7 especifica, e
    -- renomear a coluna para fugir de um conflito que nao existe custaria a
    -- correspondencia entre plano e schema.
    role        VARCHAR(120) NOT NULL,

    -- DATE, e nao TIMESTAMPTZ: o que se registra e o mes de entrada e de saida,
    -- nao um instante. Guardar hora obrigaria a inventar uma, e a comparacao
    -- entre periodos passaria a depender de fuso - para um dado que ninguem
    -- informa com essa precisao.
    start_date  DATE         NOT NULL,

    -- Nulo e o que significa "cargo atual", e e essa a leitura que o badge
    -- "Atual" da DoD do MVP 2 usa. Ausencia de data de saida e a forma honesta
    -- de dizer que ela ainda nao existe; a alternativa seria uma data no futuro
    -- ou um booleano redundante, que poderia divergir da propria data.
    end_date    DATE,

    description TEXT         NOT NULL,

    -- Lista de destaques (secao 3.7). jsonb evita uma tabela filha para uma
    -- colecao de texto lida sempre inteira e escrita quase nunca.
    --
    -- O default '[]' existe para que nao haja dois jeitos de dizer "sem
    -- destaques". Com NULL permitido, o mapeamento teria de tratar nulo e vazio
    -- como a mesma coisa em todo lugar que tocasse a coluna - e bastaria um
    -- lugar esquecido para virar NullPointerException.
    highlights  JSONB        NOT NULL DEFAULT '[]'::jsonb,

    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- jsonb aceita qualquer JSON valido, inclusive `"texto"`, `42` e `{}`. Sem
    -- esta restricao a coluna prometeria uma lista e entregaria o que fosse
    -- escrito nela, e o erro apareceria no mapeamento, longe da causa.
    CONSTRAINT experience_highlights_is_array_ck
        CHECK (jsonb_typeof(highlights) = 'array'),

    -- Periodo que termina antes de comecar e dado impossivel, nao preferencia de
    -- apresentacao. A regra fica no banco porque e onde ela vale para o seed,
    -- para a aplicacao e para qualquer INSERT feito a mao.
    CONSTRAINT experience_period_ck
        CHECK (end_date IS NULL OR end_date >= start_date),

    -- Chave natural do registro, e a razao dela e a mesma do `singleton` em
    -- profile: e sobre ela que o ON CONFLICT do seed repetivel vai decidir entre
    -- inserir e atualizar. Sem uma chave natural, o seed teria de apagar tudo e
    -- reinserir a cada execucao, descartando os ids.
    --
    -- A tripla e o que identifica uma passagem: a mesma pessoa pode voltar a
    -- mesma empresa, e pode acumular dois cargos na mesma empresa ao mesmo
    -- tempo - o que nao pode e a mesma empresa, no mesmo cargo, comecando no
    -- mesmo dia duas vezes.
    CONSTRAINT experience_company_role_start_uk UNIQUE (company, role, start_date)
);

COMMENT ON TABLE experience IS 'Timeline profissional exibida na secao Sobre.';
COMMENT ON COLUMN experience.end_date IS 'Nulo significa cargo atual.';
COMMENT ON COLUMN experience.highlights IS 'Array JSON de destaques da posicao.';

-- A leitura real e sempre "a timeline inteira, da mais recente para a mais
-- antiga". O indice cobre essa ordenacao, evitando o sort.
--
-- Sem clausula NULLS: start_date e NOT NULL, entao ela nunca se aplicaria - e
-- NULLS FIRST ja e o default de DESC, a ponto de o Postgres descartar a
-- clausula ao guardar a definicao do indice.
CREATE INDEX experience_start_date_desc_idx
    ON experience (start_date DESC);

-- Nao ha coluna display_order aqui, e a diferenca em relacao a social_link e
-- proposital. Em social_link a ordem e arbitraria e so existe se for guardada;
-- na timeline ela e derivavel de start_date. Guardar as duas coisas criaria duas
-- fontes de verdade para a mesma ordem, e elas divergiriam na primeira correcao
-- de data feita sem lembrar de renumerar.
--
-- Quem de fato ordena e o dominio, no commit 25 - o indice acima e redundancia
-- deliberada, do mesmo modo que o @OrderBy de social_link e. Isso ja custou uma
-- suposicao errada uma vez: mudanca de consulta nao pode ser usada como
-- argumento de que a ordem esta garantida.
