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

-- ATENCAO - conteudo provisorio.
--
-- Tres campos ficam vazios porque sao fatos sobre uma pessoa real e so ela pode
-- informa-los: location, resume_url e available_for_work (que assume o default
-- conservador, FALSE). O texto da bio abaixo afirma apenas o que este
-- repositorio comprova por si mesmo. Substituir isto e editar este arquivo -
-- nao e um bloqueio de engenharia (secao 17, risco de conteudo).

INSERT INTO profile (full_name, headline, bio, location, resume_url, available_for_work)
VALUES (
    'Crystofer Demetino',
    'Desenvolvedor Backend — Java & Spring Boot',
    'Desenvolvedor backend com foco em Java e Spring Boot. Trabalho com arquitetura '
        || 'hexagonal, migrações versionadas, testes de integração com Testcontainers e '
        || 'APIs REST documentadas em OpenAPI. Este portfólio é a própria demonstração '
        || 'disso: o conteúdo desta página vem de uma API Java com PostgreSQL, e o '
        || 'código está público no GitHub.',
    NULL,
    NULL,
    FALSE
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

-- Um unico link, e nao por esquecimento: e a unica URL confirmavel a partir
-- deste repositorio. LinkedIn e e-mail dependem de decisao do dono - publicar um
-- endereco de e-mail e escolha dele, nao do codigo. A mesma regra ja vale no
-- front, em lib/navigation.ts, e as duas listas se encontram no commit 22.
--
-- O profile_id vem de consulta, e nao de constante: a coluna id e GENERATED
-- ALWAYS, entao o valor nao e conhecido por quem escreve o seed. Como a tabela
-- tem uma linha so, o CROSS JOIN produz exatamente uma tupla por plataforma.
INSERT INTO social_link (profile_id, platform, url, display_order)
SELECT p.id, v.platform, v.url, v.display_order
FROM profile p
CROSS JOIN (VALUES
    ('github', 'https://github.com/CrySamuel', 0)
) AS v (platform, url, display_order)
ON CONFLICT (profile_id, platform) DO UPDATE SET
    url           = EXCLUDED.url,
    display_order = EXCLUDED.display_order,
    updated_at    = now();
