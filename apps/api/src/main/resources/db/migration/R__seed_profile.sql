-- Conteudo do perfil.
--
-- Migracao REPETIVEL (prefixo R__), e nao versionada. A escolha resolve dois
-- problemas de uma vez:
--
--   1. A secao 3.7 exige o seed "em migration separada e idempotente". Repetivel
--      e a forma que o proprio Flyway da para isso: ela roda sempre que o
--      checksum do arquivo muda, e depois de todas as versionadas.
--   2. A secao 5.3 reserva V2 a V6 para as proximas tabelas. Um V2__seed_profile
--      colidiria com o V2__create_experience_table do commit 24.
--
-- O ganho pratico e o da secao 2.2: atualizar o conteudo do portfolio passa a
-- ser editar este arquivo e fazer deploy. Nao ha migracao nova a cada correcao
-- de texto, e o historico da mudanca fica no git, que e onde se procura.
--
-- Por isso o arquivo tem uma obrigacao: rodar duas vezes tem de deixar o banco
-- exatamente no mesmo estado. Os dois comandos abaixo sao upsert por isso.

-- Conteudo informado pelo dono do portfolio. A bio continua sendo a redacao que
-- afirma apenas o que este repositorio comprova por si mesmo, e pode ser trocada
-- a qualquer momento - e para isso que esta migracao e repetivel.
--
-- O resume_url deixou de ser nulo no commit 28, quando o PDF entrou em
-- apps/web/public/resume/. O caminho e **relativo**, e a escolha repete a do
-- ProblemTypes: fixar https://portfolio.dev/... seria publicar um endereco que
-- ainda nao controlamos, e troca-lo depois quebraria quem ja consumisse. O
-- proprio site serve o arquivo, entao a raiz e ele.
--
-- O nome do arquivo tem o idioma no fim porque o commit 51 traz en-US, e a
-- versao em ingles vai conviver com esta.

INSERT INTO profile (full_name, headline, bio, location, resume_url, available_for_work)
VALUES (
    'Crystofer Demetino',
    'Desenvolvedor Backend — Java & Spring Boot',
    'Desenvolvedor backend com foco em Java e Spring Boot. Trabalho com arquitetura '
        || 'hexagonal, migrações versionadas, testes de integração com Testcontainers e '
        || 'APIs REST documentadas em OpenAPI. Este portfólio é a própria demonstração '
        || 'disso: o conteúdo desta página vem de uma API Java com PostgreSQL, e o '
        || 'código está público no GitHub.',
    'Remoto · Brasil',
    '/resume/crystofer-cv-pt-br.pdf',
    TRUE
)
-- O ON CONFLICT so e possivel porque a coluna singleton tem UNIQUE. Sem ela nao
-- haveria chave sobre a qual reconhecer "a linha do perfil" - a tabela nao tem
-- chave natural - e o upsert teria de virar DELETE seguido de INSERT, que
-- descartaria os ids e quebraria as FKs de social_link a cada execucao.
ON CONFLICT (singleton) DO UPDATE SET
    full_name          = EXCLUDED.full_name,
    headline           = EXCLUDED.headline,
    bio                = EXCLUDED.bio,
    location           = EXCLUDED.location,
    resume_url         = EXCLUDED.resume_url,
    available_for_work = EXCLUDED.available_for_work,
    updated_at         = now();

-- A lista `desejados` e a fonte de verdade dos links: o que esta nela existe no
-- banco, o que nao esta e removido. Sem o DELETE, o seed saberia acrescentar e
-- corrigir mas nunca tirar - e a frase "editar este arquivo atualiza o
-- portfolio" seria falsa justamente no caso em que mais importa, o de despublicar
-- um perfil.
--
-- O DELETE mora numa CTE porque o PostgreSQL executa CTE que modifica dados
-- exatamente uma vez e ate o fim, referenciada ou nao. Assim a lista aparece uma
-- vez so no arquivo. A ordem entre as duas sub-instrucoes nao importa: elas
-- operam sobre conjuntos disjuntos - o DELETE so alcanca plataformas fora da
-- lista, o INSERT so alcanca as de dentro.
--
-- O profile_id vem de consulta, e nao de constante: a coluna id e GENERATED
-- ALWAYS, entao o valor nao e conhecido por quem escreve o seed. Como a tabela
-- tem uma linha so, o CROSS JOIN produz exatamente uma tupla por plataforma.
--
-- Nao ha telefone aqui, e a ausencia e uma decisao. Numero pessoal em
-- repositorio publico e permanente: sai do banco com uma edicao, mas segue
-- recuperavel por `git log -p`, e a secao 17.3 proibe force push na main
-- publicada. O e-mail nao tem esse custo - ja consta do package.json e da
-- autoria dos commits, entao publica-lo aqui nao aumenta exposicao.
WITH desejados (platform, url, display_order) AS (
    VALUES
        ('github',   'https://github.com/CrySamuel',                  0),
        ('linkedin', 'https://www.linkedin.com/in/crystofer-samuel/', 1),
        ('email',    'mailto:crystoferdemetino@gmail.com',            2)
),
removidos AS (
    DELETE FROM social_link
    WHERE platform NOT IN (SELECT platform FROM desejados)
)
INSERT INTO social_link (profile_id, platform, url, display_order)
SELECT p.id, d.platform, d.url, d.display_order
FROM profile p
CROSS JOIN desejados d
ON CONFLICT (profile_id, platform) DO UPDATE SET
    url           = EXCLUDED.url,
    display_order = EXCLUDED.display_order,
    updated_at    = now();
