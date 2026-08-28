/**
 * Os nomes e limites do formulário — **e nada que dependa do Zod**.
 *
 * <p>Este arquivo nasceu de uma medição, não de gosto por arquivo pequeno. As
 * duas pontas precisam destas constantes: o servidor, para montar o schema; o
 * `ContactForm`, que é Client Component, para desenhar os campos. Com tudo num
 * módulo só, importar `CAMPOS` no cliente arrastava o Zod junto — o bundler não
 * consegue provar que uma biblioteca inteira é descartável, e o `import` no topo
 * do módulo já basta para incluí-la.
 *
 * <p><strong>O preço estava medido:</strong> o bundle inicial foi de 119,8 KB
 * para <strong>136,9 KB</strong> comprimidos, estourando o teto de 130 KB da
 * §10.1. O relatório do `next build` não mostrava isso — ele não conta os chunks
 * de cliente do layout (§4.3) —, e só a medição por script pegou.
 *
 * <p><strong>A regra que fica:</strong> módulo importado por Client Component
 * carrega para o navegador tudo o que ele importa, inclusive o que não é usado
 * ali. Constante compartilhada entre as duas pontas precisa morar sozinha.
 */

/**
 * Os limites de cada campo, e eles não foram escolhidos aqui.
 *
 * São os mesmos de `V5__create_contact_message_table.sql`, do `ContactMessage`
 * do domínio e do `ContactRequest` da API — quatro cópias do mesmo número, e a
 * repetição é a que o próprio domínio já documenta: cada camada recusa num
 * momento diferente, e nenhuma torna a outra dispensável.
 *
 * O que esta cópia acrescenta é **quando**: aqui a recusa acontece antes de
 * qualquer requisição sair, com o erro embaixo do campo. Sem ela, digitar 6.000
 * caracteres viraria uma viagem até o Brasil e de volta para descobrir que o
 * texto era longo demais.
 */
export const LIMITES = {
  name: 120,
  // 254 é o máximo de um endereço de e-mail pelo RFC 5321 — 64 de parte local,
  // 1 de arroba, 189 de domínio. Não é número arbitrário nem margem de coluna.
  email: 254,
  subject: 150,
  message: 5_000,
} as const;

/**
 * O nome de cada campo no formulário.
 *
 * <p>Fonte única do `name` do input, da chave do erro e da leitura do
 * `FormData`. Três strings soltas divergiriam no primeiro ajuste, e a
 * divergência é silenciosa do pior jeito: o campo continua na tela, o erro
 * continua sendo calculado, e os dois deixam de se encontrar — o input fica sem
 * `aria-describedby` válido e o leitor de tela não anuncia nada.
 */
export const CAMPOS = ['name', 'email', 'subject', 'message'] as const;

export type CampoDoContato = (typeof CAMPOS)[number];

/** O nome do campo-armadilha. Não é um `CampoDoContato`: nenhuma pessoa o vê. */
export const CAMPO_ARMADILHA = 'website';

/**
 * O nome do marcador que só existe quando não há JavaScript.
 *
 * <p>Vive dentro de um `<noscript>`, então o navegador só o materializa quando
 * o script está desligado. Ver o comentário do `ContactForm`, onde o custo dessa
 * escolha está medido.
 */
export const CAMPO_SEM_SCRIPT = 'sem-script';

/** O nome do campo que o widget do Turnstile preenche sozinho. */
export const CAMPO_TURNSTILE = 'cf-turnstile-response';

/**
 * O estado que o formulário desenha, e **ele mora aqui por imposição do
 * runtime**, não por gosto.
 *
 * <p>O lugar natural seria ao lado da ação que o produz. Um módulo `'use server'`
 * só pode exportar funções assíncronas — cada exportação vira um endpoint —, e
 * um tipo mais uma constante ali fazem o build falhar com
 * `Only async functions are allowed to be exported`. Este arquivo já é o
 * contrato entre a ação e o formulário: é onde os nomes dos campos são
 * definidos, então é onde a forma da resposta pode ficar sem inventar um quinto
 * arquivo.
 *
 * <p>União discriminada, e não um objeto de campos opcionais: `enviado` não tem
 * mensagem de erro, `recusado` sempre tem, e um tipo que permitisse as duas
 * combinações erradas exigiria que cada leitor lembrasse de checar. Aqui o
 * compilador lembra.
 *
 * <p><strong>`valores` existe por causa de quem não tem JavaScript.</strong> Com
 * script, uma recusa não recarrega nada e o que foi digitado continua no campo.
 * Sem script, o navegador troca a página inteira pela resposta do servidor — e
 * sem estes valores de volta, o formulário reapareceria vazio, mandando a pessoa
 * digitar tudo outra vez para corrigir uma vírgula no e-mail.
 */
export type EstadoDoContato =
  | { readonly estado: 'inicial' }
  | { readonly estado: 'enviado' }
  | {
      readonly estado: 'recusado';
      /** A frase geral, anunciada em região viva. */
      readonly mensagem: string;
      /** O erro de cada campo, quando é de campo. */
      readonly erros: Readonly<Partial<Record<CampoDoContato, string>>>;
      /** O que a pessoa digitou, para o formulário não nascer vazio. */
      readonly valores: Readonly<Partial<Record<CampoDoContato, string>>>;
    };

export const ESTADO_INICIAL: EstadoDoContato = { estado: 'inicial' };
