-- Catalogo de projetos (secao 3.7 do plano).
--
-- Quarta migracao versionada, e a maior: quatro tabelas num arquivo so, pela
-- mesma razao que juntou skill e skill_category no V3 - elas nascem juntas e
-- nenhuma faz sentido sozinha. Projeto sem tecnologia nao aparece no filtro,
-- tecnologia sem projeto e um chip que nao filtra nada, e metrica sem projeto
-- nao tem onde ser exibida.
--
-- O MVP 3 se chama "prova de trabalho", e o schema e onde essa exigencia vira
-- estrutura: problem, solution e outcome sao NOT NULL, entao um projeto sem
-- narrativa nao entra no banco. A alternativa - colunas nulaveis e um card que
-- some quando o texto falta - transformaria a regra editorial em detalhe de
-- apresentacao, que e onde ela para de valer.

CREATE TABLE project (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- A URL e contrato publico (secao 3.8), entao o slug e escolhido, nao
    -- derivado do id. O UNIQUE existe por isso, e nao por desempenho.
    slug          VARCHAR(80)  NOT NULL,

    title         VARCHAR(120) NOT NULL,

    -- O limite tem motivo, e ele e da tela: a DoD do MVP 3 exige cards de altura
    -- consistente, e texto sem teto empurra essa garantia para o CSS, onde ela
    -- vira truncamento com reticencias - que esconde conteudo em vez de evitar
    -- que ele exista. Com VARCHAR(280) o limite e recusado na escrita, longe do
    -- visitante e perto de quem escreveu o texto.
    summary       VARCHAR(280) NOT NULL,

    -- A narrativa que a secao 1.2 promete ao cliente e ao tech lead. Sao TEXT e
    -- nao VARCHAR porque aqui nao ha limite de layout a defender: os tres vivem
    -- na pagina de detalhe, que rola.
    problem       TEXT         NOT NULL,
    solution      TEXT         NOT NULL,
    outcome       TEXT         NOT NULL,

    -- Nulaveis, e as duas juntas podem faltar. Projeto sob acordo de
    -- confidencialidade e trabalho real sem link publico - e o lugar dele e a
    -- timeline, nao o catalogo. Mas transformar isso em CHECK exigiria decidir
    -- hoje, no schema, uma politica de conteudo que ainda nao foi tomada, e
    -- desfazer um CHECK de migracao ja aplicada custa outra migracao.
    repo_url      TEXT,
    live_url      TEXT,

    -- Caminho do arquivo servido pelo web, nao URL: as imagens do portfolio sao
    -- estaticas e passam pelo next/image, que exige caminho local para otimizar.
    cover_image   TEXT,

    -- Destaque na home (secao 6, F06). Booleano e nao um campo de posicao
    -- separado: "esta na home" e uma decisao de sim ou nao, e a ordem entre os
    -- destacados ja sai de display_order.
    featured      BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Aqui a ordem precisa ser guardada, e a comparacao com experience e o
    -- ponto. La a ordem sai de start_date, entao uma coluna seria segunda fonte
    -- de verdade. Aqui a ordem e editorial: o primeiro card e o projeto mais
    -- forte, nao o mais recente - e "mais forte" nao esta em nenhuma outra
    -- coluna. Mesma razao de skill_category.display_order.
    display_order SMALLINT     NOT NULL DEFAULT 0,

    -- DATE pelo mesmo motivo de experience.start_date: o que se registra e o mes
    -- em que o projeto ficou pronto, nao um instante. O sufixo _at sugere
    -- timestamp e foi mantido porque e o nome que a secao 3.7 especifica -
    -- renomear para published_on custaria a correspondencia entre plano e
    -- schema, do mesmo modo que manter `role` custou em experience.
    --
    -- Nulavel de proposito, pela regra que skill.years_of_experience ja
    -- estabeleceu: nem todo projeto tem uma data honesta a declarar, e inventar
    -- uma para preencher a coluna seria pior do que omitir.
    published_at  DATE,

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT project_slug_uk UNIQUE (slug),

    -- O mesmo formato que o value object Slug vai validar no commit 34, e a
    -- duplicacao e deliberada - a mesma escolha do CHECK de skill.proficiency.
    -- O conteudo deste projeto entra por migracao (ADR-0004), ou seja, sem
    -- passar pela aplicacao: um slug com espaco ou maiuscula escrito direto num
    -- INSERT gravaria em silencio e quebraria a URL depois, longe da causa.
    CONSTRAINT project_slug_format_ck
        CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),

    -- Endereco sem esquema e o erro que nao aparece: `github.com/user/repo` num
    -- href vira caminho relativo, o navegador o resolve contra o proprio site e
    -- devolve 404 - sem erro no console, sem linha no log.
    CONSTRAINT project_repo_url_ck
        CHECK (repo_url IS NULL OR repo_url LIKE 'https://%'),
    CONSTRAINT project_live_url_ck
        CHECK (live_url IS NULL OR live_url LIKE 'https://%')
);

COMMENT ON TABLE project IS 'Catalogo de projetos com narrativa problema, solucao e resultado.';
COMMENT ON COLUMN project.slug IS 'Identificador da URL publica; minusculas, digitos e hifens.';
COMMENT ON COLUMN project.featured IS 'Verdadeiro para os projetos em destaque na home.';
COMMENT ON COLUMN project.published_at IS 'Nulo quando nao ha data honesta a declarar.';

CREATE TABLE technology (
    id        BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- O nome e exibido como esta escrito: "Spring Boot", "PostgreSQL". A
    -- capitalizacao e parte do dado, porque marca escrita errada e ruido para
    -- quem avalia.
    name      VARCHAR(60) NOT NULL,

    -- O slug serve a query string do filtro (commit 38), que precisa ser
    -- compartilhavel e indexavel - entao ele nao pode conter espaco nem
    -- acentuacao.
    slug      VARCHAR(60) NOT NULL,

    -- Codigo em minusculo, e nao texto livre, pela mesma razao de
    -- skill.proficiency: sem a lista fechada, "Backend" e "backend" viram dois
    -- grupos no filtro e ninguem percebe ate a tela mostrar os dois.
    --
    -- Nao e chave estrangeira para skill_category, e a recusa e arquitetural. A
    -- secao 2.8 proibe um modulo conhecer o outro, e skill_category pertence ao
    -- modulo profile enquanto technology pertence a projects. Alem disso as duas
    -- taxonomias respondem a perguntas diferentes: uma agrupa competencias para
    -- leitura, a outra agrupa chips para filtragem.
    category  VARCHAR(20) NOT NULL,

    -- Previsto pela secao 3.7 e nulavel, porque provavelmente vai ficar nulo. A
    -- DoD do MVP 2 ja registrou a divida: logos de linguagens e frameworks sao
    -- marcas registradas, cada projeto com politica propria de uso, e desenhar
    -- aproximacoes ficaria pior do que nao ter icone.
    icon_slug VARCHAR(60),

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT technology_name_uk UNIQUE (name),
    CONSTRAINT technology_slug_uk UNIQUE (slug),

    CONSTRAINT technology_slug_format_ck
        CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),

    CONSTRAINT technology_category_ck
        CHECK (category IN ('language', 'framework', 'database', 'infrastructure', 'tool'))
);

COMMENT ON TABLE technology IS 'Tecnologia usada por um projeto; alimenta o filtro do catalogo.';
COMMENT ON COLUMN technology.category IS 'Codigo em minusculo: language, framework, database, infrastructure ou tool.';
COMMENT ON COLUMN technology.icon_slug IS 'Nulo enquanto nao houver sprite de icones proprio.';

-- A juncao N:N. Nao ha coluna id, e a ausencia e o ponto: o par **e** a
-- identidade da linha, entao a chave primaria composta torna impossivel
-- declarar a mesma tecnologia duas vezes no mesmo projeto. Com um id proprio
-- essa garantia dependeria de um UNIQUE acrescentado ao lado, que alguem pode
-- esquecer de escrever.
CREATE TABLE project_tech (
    -- CASCADE porque o vinculo nao existe fora do projeto: apagado o projeto,
    -- suas tecnologias declaradas deixam de significar alguma coisa.
    project_id    BIGINT NOT NULL REFERENCES project (id) ON DELETE CASCADE,

    -- RESTRICT, e a assimetria e deliberada. Tecnologia existe por si, e
    -- CASCADE aqui faria "remover Docker do catalogo" apagar em silencio o
    -- Docker de todos os projetos que o declaram - a perda apareceria como chip
    -- faltando na tela, sem nada no log. Com RESTRICT o banco recusa, e quem
    -- quiser mesmo remover precisa desvincular antes, explicitamente.
    technology_id BIGINT NOT NULL REFERENCES technology (id) ON DELETE RESTRICT,

    CONSTRAINT project_tech_pk PRIMARY KEY (project_id, technology_id)
);

COMMENT ON TABLE project_tech IS 'Vinculo N:N entre projeto e tecnologia.';

CREATE TABLE project_metric (
    id            BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    project_id    BIGINT      NOT NULL REFERENCES project (id) ON DELETE CASCADE,

    label         VARCHAR(60) NOT NULL,

    -- Texto, e nao NUMERIC. O exemplo da secao 3.7 e "p95" com "80ms", e o valor
    -- carrega unidade: "40%", "4h para 2h", "24/7". Guardar numero obrigaria uma
    -- coluna de unidade ao lado e ainda assim nao caberia o terceiro caso -
    -- entao o tipo seguiria mentindo, so que com mais colunas.
    --
    -- `value` e palavra-chave nao reservada no PostgreSQL, entao vale como nome
    -- de coluna sem aspas. Mantido pela mesma razao que manteve `role` em
    -- experience: e o nome que a secao 3.7 especifica.
    value         VARCHAR(40) NOT NULL,

    -- Editorial, como em project e em skill_category: a metrica mais forte vem
    -- primeiro, e "mais forte" nao esta em nenhuma outra coluna. A secao 3.7 nao
    -- preve esta coluna; sem ela a ordem seria a de insercao, que e a ordem que
    -- o planejador quiser devolver.
    display_order SMALLINT    NOT NULL DEFAULT 0,

    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Duas metricas com o mesmo rotulo no mesmo projeto sao a mesma metrica
    -- escrita duas vezes, e a tela mostraria as duas. O par tambem e a chave
    -- natural sobre a qual o ON CONFLICT do seed repetivel vai decidir.
    CONSTRAINT project_metric_project_id_label_uk UNIQUE (project_id, label)
);

COMMENT ON TABLE project_metric IS 'Numero que sustenta o resultado declarado pelo projeto.';
COMMENT ON COLUMN project_metric.value IS 'Valor com unidade, como texto: 80ms, 40%, 24/7.';

-- Nao ha nenhum CREATE INDEX neste arquivo, e a ausencia contraria o objetivo
-- escrito para este commit ("indices em slug e featured"). Os dois casos, em
-- separado:
--
-- **slug** ja tem indice. Restricao unica cria um no PostgreSQL, entao
-- project_slug_uk e technology_slug_uk cobrem as buscas por slug do commit 36 -
-- um CREATE INDEX ao lado criaria uma segunda estrutura para a mesma coluna,
-- paga em toda escrita e nunca escolhida pelo planejador.
--
-- **featured** nao tem, de proposito, e o motivo e o mesmo que dispensou indice
-- em skill_category e em profile: o catalogo tem unidades de linhas. Abaixo de
-- algumas centenas o planejador faz seq scan de qualquer jeito, porque ler a
-- tabela inteira custa menos que percorrer indice e voltar ao heap. O indice
-- so seria pago - em escrita e em disco - sem nunca ser lido.
--
-- As chaves estrangeiras tambem nao precisam de indice proprio, com uma
-- excecao. project_tech.project_id e project_metric.project_id sao a coluna
-- inicial de um indice que ja existe - a chave primaria composta de um, a chave
-- unica do outro -, e indice composto serve consulta pela coluna da esquerda.
-- Sobra project_tech.technology_id, que e a direcao consultada pelo filtro do
-- commit 38 e nao e coberta por nada. Se o catalogo um dia crescer, e o
-- primeiro indice a criar - e por isso fica escrito aqui.
