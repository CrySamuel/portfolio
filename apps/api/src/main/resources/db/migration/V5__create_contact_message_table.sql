-- Mensagens do formulario de contato (secao 3.7 do plano).
--
-- Quinta migracao versionada, e a primeira tabela deste schema que existe para
-- **receber escrita do visitante**. Todas as anteriores guardam conteudo que
-- entra por migracao (ADR-0004) e sai por leitura; esta inverte a direcao, e
-- quase toda decisao abaixo vem dessa diferenca - limite de tamanho, recusa de
-- texto vazio e ausencia de PII desnecessaria deixam de ser zelo e viram a
-- fronteira entre o sistema e a internet.
--
-- E tambem a unica tabela cujas linhas **mudam depois de inseridas**: o
-- email_status caminha de PENDING para SENT ou FAILED. O Fluxo B da secao 5.4
-- depende disso - a mensagem e persistida antes de qualquer tentativa de envio,
-- entao o provedor de e-mail pode estar fora do ar sem que nada se perca.

CREATE TABLE contact_message (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- Os limites sao a primeira barreira, e existem antes de qualquer validacao
    -- de aplicacao. Bean Validation recusa no controlador; isto recusa mesmo que
    -- alguem escreva direto no banco um dia - e, ao contrario do controlador,
    -- nao depende de ninguem lembrar de anotar o campo novo.
    name          VARCHAR(120) NOT NULL,

    -- 254 e o teto de um endereco de e-mail pelo RFC 5321. Nao e numero
    -- escolhido: e o maximo que pode existir, entao qualquer valor maior e
    -- entrada malformada e nao mensagem legitima truncada.
    email         VARCHAR(254) NOT NULL,

    subject       VARCHAR(150) NOT NULL,

    -- TEXT com CHECK, e nao VARCHAR(5000). Os dois recusam o mesmo tamanho, mas
    -- o CHECK diz o motivo em voz alta na definicao da tabela e permite fixar
    -- **os dois extremos** na mesma restricao - o teto contra abuso e o piso
    -- contra mensagem vazia.
    message       TEXT         NOT NULL,

    -- **Hash, e nao o IP.** Endereco de IP e dado pessoal sob a LGPD, e guardar
    -- o original obrigaria a politica de retencao, base legal e resposta a
    -- pedido de exclusao - custo desproporcional para o unico uso que ele tem
    -- aqui, que e reconhecer o mesmo remetente numa auditoria.
    --
    -- SHA-256 em hexadecimal da exatamente 64 caracteres, entao CHAR e nao
    -- VARCHAR: o tamanho e fixo por construcao e o tipo diz isso.
    --
    -- O sal fica na configuracao da aplicacao, **fora do banco**. Com o sal
    -- junto dos hashes, um vazamento do dump devolveria os IPs por forca bruta -
    -- o espaco de enderecos IPv4 tem 4 bilhoes de itens, o que uma GPU percorre
    -- em minutos.
    --
    -- Nulavel de proposito: atras de proxy a origem nem sempre e determinavel, e
    -- um nulo honesto e melhor que o hash de uma string vazia, que agruparia
    -- visitantes distintos sob a mesma chave e mentiria na auditoria.
    ip_hash       CHAR(64),

    -- Sem limite de tamanho e nulavel: user agent e cabecalho que o cliente
    -- escolhe, entao qualquer teto aqui seria arbitrario, e cliente nenhum e
    -- obrigado a mandar.
    user_agent    TEXT,

    -- VARCHAR + CHECK, e nao tipo ENUM do PostgreSQL - a mesma decisao do
    -- proficiency no V3, e pelo mesmo motivo: acrescentar valor a um ENUM e DDL,
    -- e remover exige recriar o tipo inteiro. Com CHECK, a lista de valores
    -- permitidos e uma linha de migracao.
    --
    -- **Maiusculas aqui e minusculas no proficiency, e a diferenca tem razao.**
    -- Os niveis de competencia sao valor de contrato publico: aparecem no
    -- openapi.json e viram uniao literal no cliente TypeScript, entao seguem a
    -- convencao de JSON. Este status nunca sai da API - e estado interno de
    -- entrega -, entao segue a convencao de constante do Java, que e onde ele e
    -- lido.
    email_status  VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Presente aqui pela razao que faltava nas outras tabelas: esta e a unica
    -- cujas linhas realmente mudam depois de inseridas. Omiti-lo justamente na
    -- tabela que sofre UPDATE seria o inverso do que a coluna existe para fazer,
    -- e o job de reprocessamento precisa saber **quando** a ultima tentativa
    -- aconteceu para nao insistir em rajada.
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- NOT NULL nao impede string vazia, e o formulario chega pela internet. Sem
    -- estes quatro, uma requisicao com "" em todos os campos grava uma linha
    -- valida para o banco e inutil para quem for ler - e o defeito so apareceria
    -- na caixa de entrada.
    CONSTRAINT contact_message_name_ck    CHECK (btrim(name) <> ''),
    CONSTRAINT contact_message_subject_ck CHECK (btrim(subject) <> ''),

    -- O e-mail nao ganha CHECK de formato, e a ausencia e deliberada. Expressao
    -- regular para endereco valido ou e curta e erra, ou e a do RFC 5322 e
    -- ninguem revisa. A validacao de forma fica no Bean Validation, onde o erro
    -- volta ao visitante com o campo certo; aqui garante-se o que o banco
    -- consegue garantir de verdade, que e nao estar vazio.
    CONSTRAINT contact_message_email_ck   CHECK (btrim(email) <> ''),

    -- Os dois extremos na mesma restricao. O teto de 5000 e o mesmo que o
    -- formulario anuncia ao visitante; ele existe aqui para que o limite nao
    -- dependa do JavaScript da pagina, que qualquer um desliga.
    CONSTRAINT contact_message_message_ck
        CHECK (char_length(btrim(message)) BETWEEN 1 AND 5000),

    -- Tres estados, e o PENDING e o default porque a mensagem nasce persistida e
    -- ainda nao enviada - essa e a ordem que faz nenhuma se perder.
    CONSTRAINT contact_message_email_status_ck
        CHECK (email_status IN ('PENDING', 'SENT', 'FAILED'))
);

COMMENT ON TABLE contact_message IS
    'Mensagem recebida pelo formulario de contato, com o estado da entrega por e-mail.';
COMMENT ON COLUMN contact_message.ip_hash IS
    'SHA-256 do IP de origem com sal da aplicacao. Nulo quando a origem nao pode ser determinada.';
COMMENT ON COLUMN contact_message.email_status IS
    'PENDING ao nascer, SENT apos entrega confirmada, FAILED quando o provedor recusou.';

-- **Um indice, e ele tem consulta.** O job de reprocessamento do commit 47
-- procura exatamente por FAILED, e essa e a unica consulta deste schema que
-- roda sozinha, em intervalo fixo, sem ninguem olhando.
--
-- Parcial de proposito: em operacao normal quase toda linha e SENT, entao um
-- indice sobre a coluna inteira gastaria escrita e disco para indexar o valor
-- que a consulta nunca procura. O parcial indexa so as linhas que interessam, e
-- em operacao saudavel ele fica praticamente vazio.
--
-- Este e o primeiro indice explicito do schema, e o contraste com o V4 e o
-- argumento: la nao havia nenhum porque as tabelas tem unidades de linhas e sao
-- lidas por inteiro. Aqui a tabela cresce com o tempo e a consulta busca um
-- subconjunto raro - a situacao em que indice deixa de ser custo e vira leitura.
CREATE INDEX contact_message_failed_idx
    ON contact_message (created_at)
    WHERE email_status = 'FAILED';
