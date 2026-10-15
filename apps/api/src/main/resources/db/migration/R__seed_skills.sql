-- Conteudo das competencias.
--
-- Migracao REPETIVEL, como os outros seeds: editar este arquivo e fazer deploy e
-- o jeito de atualizar o portfolio, e rodar duas vezes tem de deixar o banco no
-- mesmo estado.
--
-- Os niveis foram atribuidos a partir das evidencias dos repositorios publicos
-- do dono - dependencias declaradas em pom.xml, build.gradle e requirements.txt,
-- mais o que este proprio repositorio exercita - e aprovados por ele.
--
-- **Nenhuma competencia e 'advanced', e a escolha e do dono.** Um ano de
-- carreira reivindicando nivel avancado levanta duvida em quem entrevista; a
-- escala inteira parando em intermediario passa a impressao oposta, e continua
-- verdadeira.
--
-- **years_of_experience fica nulo em todas.** A coluna e nulavel exatamente para
-- isso: nao havia numero honesto a declarar por competencia, e inventar um seria
-- pior do que omitir. Quando houver, e so preencher aqui.

-- As categorias primeiro: skill referencia skill_category, entao a ordem entre os
-- dois comandos importa.
--
-- O display_order e o unico jeito de a ordem existir - ela e editorial, e nao
-- derivavel do dado, ao contrario da timeline (que sai de start_date) e das
-- competencias dentro de cada grupo (que saem de nivel e nome).
--
-- Linguagens vem primeiro porque e o que a vaga de backend procura. IA vem em
-- seguida por ser o diferencial declarado no resumo do curriculo.
WITH categorias_desejadas (name, display_order) AS (
    VALUES
        ('Linguagens & Frameworks',        0::smallint),
        ('Inteligência Artificial & Dados', 1::smallint),
        ('Bancos de Dados',                 2::smallint),
        ('Infraestrutura & Versionamento',  3::smallint)
),
-- Categoria fora da lista e removida, e as competencias dela vao junto pelo
-- ON DELETE CASCADE. Sem este DELETE, o seed saberia acrescentar e corrigir mas
-- nunca tirar.
removidas AS (
    DELETE FROM skill_category
    WHERE name NOT IN (SELECT name FROM categorias_desejadas)
)
INSERT INTO skill_category (name, display_order)
SELECT name, display_order FROM categorias_desejadas
ON CONFLICT (name) DO UPDATE SET
    display_order = EXCLUDED.display_order,
    updated_at    = now();

-- As competencias, referenciando a categoria pelo nome.
--
-- O category_id sai de consulta, e nao de constante: a coluna e GENERATED ALWAYS
-- e o valor nao e conhecido por quem escreve o seed. O join por nome funciona
-- porque skill_category.name e unico.
WITH desejadas (categoria, name, proficiency, years_of_experience) AS (
    VALUES
        -- Cinco repositorios em Java com Spring, incluindo este. Data JPA,
        -- Security, JWT e MongoDB aparecem em mais de um.
        ('Linguagens & Frameworks', 'Java',                       'intermediate', NULL::smallint),
        ('Linguagens & Frameworks', 'Spring Boot',                'intermediate', NULL),
        ('Linguagens & Frameworks', 'Spring Data JPA / Hibernate', 'intermediate', NULL),
        ('Linguagens & Frameworks', 'Spring Security & JWT',      'basic',        NULL),
        ('Linguagens & Frameworks', 'Python',                     'intermediate', NULL),
        ('Linguagens & Frameworks', 'Flask',                      'basic',        NULL),
        ('Linguagens & Frameworks', 'SQLAlchemy',                 'basic',        NULL),

        -- O comparativo de ML usa GridSearch, Optuna e undersampling, com artigo
        -- versionado - metodologia, e nao tutorial. RAG e NLP ficam em basico
        -- porque constam do curriculo sem repositorio que os demonstre.
        ('Inteligência Artificial & Dados', 'Machine Learning',       'intermediate', NULL),
        ('Inteligência Artificial & Dados', 'Integração com LLMs',    'intermediate', NULL),
        ('Inteligência Artificial & Dados', 'Pandas / NumPy',         'intermediate', NULL),
        ('Inteligência Artificial & Dados', 'NLP / TF-IDF',           'basic',        NULL),
        ('Inteligência Artificial & Dados', 'RAG',                    'basic',        NULL),

        -- Oracle fica em basico e sem repositorio que o comprove: consta do
        -- curriculo, e tira-lo daqui criaria divergencia entre os dois
        -- documentos. Vale saber que a pergunta pode vir numa entrevista.
        ('Bancos de Dados', 'PostgreSQL', 'intermediate', NULL),
        ('Bancos de Dados', 'MongoDB',    'basic',        NULL),
        ('Bancos de Dados', 'MySQL',      'basic',        NULL),
        ('Bancos de Dados', 'Oracle',     'basic',        NULL),

        -- Docker, Git e CI/CD sao os tres que este repositorio exercita a vista
        -- de qualquer avaliador: imagem multi-stage, commits padronizados e tres
        -- workflows.
        ('Infraestrutura & Versionamento', 'Docker',                'intermediate', NULL),
        ('Infraestrutura & Versionamento', 'Git / GitHub',          'intermediate', NULL),
        ('Infraestrutura & Versionamento', 'CI/CD (GitHub Actions)', 'intermediate', NULL),
        ('Infraestrutura & Versionamento', 'Oracle Cloud',          'basic',        NULL),
        ('Infraestrutura & Versionamento', 'Linux',                 'basic',        NULL)
),
removidas AS (
    DELETE FROM skill s
    WHERE NOT EXISTS (
        SELECT 1
        FROM desejadas d
        JOIN skill_category c ON c.name = d.categoria
        WHERE c.id = s.category_id
          AND d.name = s.name
    )
)
INSERT INTO skill (category_id, name, proficiency, years_of_experience)
SELECT c.id, d.name, d.proficiency, d.years_of_experience
FROM desejadas d
JOIN skill_category c ON c.name = d.categoria
ON CONFLICT (category_id, name) DO UPDATE SET
    proficiency         = EXCLUDED.proficiency,
    years_of_experience = EXCLUDED.years_of_experience,
    updated_at          = now();

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

WITH traducoes (name, name_en) AS (
    VALUES
        ('Linguagens & Frameworks', 'Languages & Frameworks'),
        ('Inteligência Artificial & Dados', 'Artificial Intelligence & Data'),
        ('Bancos de Dados', 'Databases'),
        ('Infraestrutura & Versionamento', 'Infrastructure & Version Control')
),
removidas AS (
    DELETE FROM skill_category_translation t
    WHERE t.locale = 'en-US'
      AND NOT EXISTS (
        SELECT 1
        FROM traducoes d
        JOIN skill_category c ON c.name = d.name
        WHERE c.id = t.category_id
    )
)
INSERT INTO skill_category_translation (category_id, locale, name)
SELECT c.id, 'en-US', d.name_en
FROM traducoes d
JOIN skill_category c ON c.name = d.name
ON CONFLICT (category_id, locale) DO UPDATE SET
    name = EXCLUDED.name;

-- So as competencias cujo nome muda. Java, PostgreSQL e Docker se chamam assim
-- em qualquer idioma, e uma linha de traducao repetindo o nome seria uma segunda
-- copia esperando para divergir. A API cai para o original quando nao ha linha.
WITH traducoes (categoria, name, name_en) AS (
    VALUES
        ('Inteligência Artificial & Dados', 'Integração com LLMs', 'LLM Integration')
),
removidas AS (
    DELETE FROM skill_translation t
    WHERE t.locale = 'en-US'
      AND NOT EXISTS (
        SELECT 1
        FROM traducoes d
        JOIN skill_category c ON c.name = d.categoria
        JOIN skill s ON s.category_id = c.id AND s.name = d.name
        WHERE s.id = t.skill_id
    )
)
INSERT INTO skill_translation (skill_id, locale, name)
SELECT s.id, 'en-US', d.name_en
FROM traducoes d
JOIN skill_category c ON c.name = d.categoria
JOIN skill s ON s.category_id = c.id AND s.name = d.name
ON CONFLICT (skill_id, locale) DO UPDATE SET
    name = EXCLUDED.name;
