import type { components } from './generated/api';

/** O perfil publico, exatamente como o contrato o descreve. */
export type Profile = components['schemas']['Profile'];

/** Um perfil externo do dono do portfolio. */
export type SocialLink = components['schemas']['SocialLink'];

/** Uniao literal - `'github' | 'linkedin' | 'email'`, e nao `string`. */
export type SocialPlatform = SocialLink['platform'];

/**
 * Uma passagem profissional da timeline.
 *
 * `endDate` e `string | null`, e o nulo carrega significado: e ele que define
 * cargo atual. O contrato declara o campo como obrigatorio **e** nulavel, entao
 * o tipo gerado obriga quem consome a tratar o caso - que e exatamente o do
 * badge "Atual".
 */
export type Experience = components['schemas']['Experience'];

/**
 * Um grupo de competencias, ja agrupado pelo servidor.
 *
 * O agrupamento e regra de negocio e chega pronto: o cliente desenha grupos, sem
 * percorrer lista plana decidindo onde comeca cada cabecalho.
 */
export type SkillCategory = components['schemas']['SkillCategory'];

/** Uma competencia. `yearsOfExperience` e `number | null` - ausencia difere de zero. */
export type Skill = components['schemas']['Skill'];

/** Uniao literal - `'basic' | 'intermediate' | 'advanced'`, e nao `string`. */
export type Proficiency = Skill['proficiency'];

/**
 * Um projeto como a listagem o exibe.
 *
 * Nao traz `problem`, `solution` nem `outcome`, e tambem nao traz `repoUrl` nem
 * `liveUrl` - as duas omissoes sao do contrato, e a segunda e de acessibilidade:
 * o card precisa de uma unica area de foco, e link dentro de card que ja e link
 * cria alvo aninhado. O tipo torna isso impossivel de errar, porque os campos
 * nao existem para o componente desenhar.
 */
export type ProjectSummary = components['schemas']['ProjectSummary'];

/** Um projeto com a narrativa completa, os enderecos e as metricas. */
export type ProjectDetail = components['schemas']['ProjectDetail'];

/** Uma tecnologia declarada por um projeto. `iconSlug` e `string | null`. */
export type Technology = components['schemas']['Technology'];

/** Uniao literal das cinco familias, e nao `string`. */
export type TechnologyCategory = Technology['category'];

/** Um numero que sustenta o resultado de um projeto. O valor carrega a unidade. */
export type ProjectMetric = components['schemas']['ProjectMetric'];

/**
 * O retrato do perfil publico no GitHub.
 *
 * Chega **sempre**, inclusive com o GitHub fora do ar: a API responde 200 com o
 * retrato vazio - listas vazias e contadores em zero - em vez de erro, que e o
 * ADR-0008 aparecendo na borda. Quem consome desenha o estado vazio; nao ha
 * caminho de excecao a tratar.
 */
export type GitHubStats = components['schemas']['GitHubStats'];

/**
 * A fatia de uma linguagem, em porcentagem ja calculada.
 *
 * O peso interno do dominio nao vem junto de proposito: ele e uma unidade que so
 * significa algo la dentro, e publica-lo obrigaria o cliente a somar e dividir
 * para chegar a este mesmo numero - com dois clientes podendo somar diferente.
 */
export type LanguageShare = components['schemas']['LanguageShare'];

/**
 * Um repositorio em destaque.
 *
 * `description` e `primaryLanguage` sao `string | null`, e os dois casos
 * acontecem de verdade no perfil real - o tipo obriga quem desenha a tratar.
 */
export type Repository = components['schemas']['Repository'];

/**
 * O corpo de `POST /api/v1/contact`.
 *
 * <strong>O unico tipo deste pacote que descreve o que entra</strong>, e nao o
 * que sai. Os outros sao formas que a API produz; este e uma forma que ela
 * aceita, entao quem erra o formato aqui e este lado - e o compilador diz antes
 * de a requisicao existir.
 *
 * `website` e o campo-armadilha, opcional no contrato de proposito: ele existe
 * para chegar vazio. Preenchido, a API responde 202 e nao grava nada - o
 * silencio e a defesa, e esta descrito no controlador.
 */
export type ContactSubmission = components['schemas']['ContactRequest'];

/** Resposta que nao foi 2xx, ou que nao chegou. */
export class ApiError extends Error {
  /** `0` quando a requisicao nem chegou a ter resposta (rede, timeout). */
  readonly status: number;
  readonly url: string;

  /**
   * Segundos ate a proxima tentativa, quando a resposta traz `Retry-After`.
   *
   * <p>`null` quando o cabecalho nao veio - que e o caso de tudo o que nao e
   * 429. Guarda-lo aqui e o que separa um "tente de novo" vago de uma frase com
   * numero: quem desenha a tela nao tem acesso a resposta, so a este erro.
   */
  readonly retryAfterSeconds: number | null;

  constructor(
    status: number,
    url: string,
    message: string,
    options?: { cause?: unknown; retryAfterSeconds?: number | null },
  ) {
    super(message, options);
    this.name = 'ApiError';
    this.status = status;
    this.url = url;
    this.retryAfterSeconds = options?.retryAfterSeconds ?? null;
  }
}

export interface ApiClientOptions {
  /** Raiz da API, sem barra final. */
  readonly baseUrl: string;
  /** Corta a espera por tentativa. Padrao: 5s. */
  readonly timeoutMs?: number;
  /** Novas tentativas depois da primeira. Padrao: 2. */
  readonly retries?: number;
  /** Cabecalhos fixos - e por onde a chave de servico vai entrar. */
  readonly headers?: Readonly<Record<string, string>>;
}

/**
 * Opcoes de cache do Next, declaradas aqui em vez de importadas.
 *
 * O pacote nao depende do Next de proposito: ele descreve o contrato da API e
 * seria usavel por um script ou por um teste. Repetir esta forma minima custa
 * seis linhas; a alternativa custaria uma dependencia de framework num pacote
 * que nao renderiza nada.
 */
export interface RequestOptions {
  /**
   * Sobrepoe o timeout do cliente nesta chamada.
   *
   * Existe porque nem toda chamada tem alguem esperando. A leitura que alimenta
   * a pre-renderizacao e a revalidacao do ISR roda no build e em segundo plano,
   * onde vale a pena aguardar um servico hibernado acordar; a mesma espera numa
   * requisicao de visitante seria inaceitavel. Um numero so no cliente
   * obrigaria a escolher entre falhar o build e travar a pagina.
   */
  readonly timeoutMs?: number;

  /**
   * Cabecalhos so desta chamada, somados aos fixos do cliente.
   *
   * Existe por um caso concreto: o limite de taxa do contato conta por
   * `X-Forwarded-For`, e quem fala com a API e o servidor do site, nao o
   * visitante. Sem repassar a origem, todos os visitantes dividiriam o mesmo
   * balde de cinco mensagens por hora - o limite valeria para o site inteiro em
   * vez de para cada remetente.
   *
   * Colisao com um cabecalho fixo do cliente e resolvida a favor daqui: o fixo
   * e o padrao, este e a excecao declarada na chamada.
   */
  readonly headers?: Readonly<Record<string, string>>;

  readonly next?: {
    readonly revalidate?: number | false;
    // `string[]`, e nao `readonly string[]`: o Next declara a propriedade como
    // array mutavel, e o compilador confere esta forma contra a dele na hora do
    // fetch. Foi o que apontou a divergencia - o tipo declarado aqui nao e
    // documentacao solta, e verificado.
    readonly tags?: string[];
  };
  readonly signal?: AbortSignal;
}

export interface ApiClient {
  getProfile(options?: RequestOptions): Promise<Profile>;

  /**
   * A timeline profissional, ja em ordem cronologica decrescente.
   *
   * A ordem e do servidor - mais precisamente, do dominio dele - e nao uma
   * gentileza da serializacao. Quem consome nao deve reordenar: seria um segundo
   * lugar decidindo a mesma coisa, e o caminho pelo qual duas telas do mesmo
   * sistema passam a mostrar ordens diferentes.
   *
   * Devolve lista vazia quando nao ha nenhuma passagem cadastrada, e nao um erro.
   */
  listExperiences(options?: RequestOptions): Promise<Experience[]>;

  /**
   * As competencias agrupadas por categoria, ja ordenadas.
   *
   * Categorias em ordem editorial; dentro de cada uma, do maior nivel para o
   * menor. Quem consome nao deve reordenar nem reagrupar - seria um segundo
   * lugar decidindo a mesma coisa.
   *
   * Devolve lista vazia quando nao ha nada cadastrado, e nao um erro.
   */
  listSkills(options?: RequestOptions): Promise<SkillCategory[]>;

  /**
   * O catalogo de projetos, em ordem editorial.
   *
   * Devolve o resumo de cada um - sem a narrativa e sem os enderecos, que sao do
   * detalhe. A omissao e do contrato, e a dos enderecos e de acessibilidade: o
   * card precisa de uma unica area de foco.
   *
   * Devolve lista vazia quando nao ha nada cadastrado, e nao um erro.
   */
  listProjects(options?: RequestOptions): Promise<ProjectSummary[]>;

  /**
   * Um projeto pelo slug, com narrativa, enderecos e metricas.
   *
   * Os dois casos de ausencia chegam como `ApiError` e sao distinguiveis pelo
   * status, porque significam coisas diferentes: **400** e slug fora do formato
   * da URL - endereco malformado -, e **404** e slug bem formado que nao existe.
   * Quem chama decide o que fazer com cada um; aqui os dois viram erro, e nao
   * `null`, para que ignorar o caso exija escrever o `catch`.
   */
  getProjectBySlug(slug: string, options?: RequestOptions): Promise<ProjectDetail>;

  /**
   * O retrato do GitHub, com linguagens e repositorios em destaque.
   *
   * <strong>E a unica leitura sem caminho de erro.</strong> As outras podem
   * responder 404 ou 400; esta responde 200 sempre, inclusive quando o GitHub
   * esta fora - devolvendo o retrato vazio. Quem consome nao escreve `catch`:
   * escreve o estado vazio.
   *
   * As linguagens vem da maior fatia para a menor, e os repositorios em ordem de
   * destaque. Nenhuma das duas deve ser reordenada aqui - a ordem e do dominio.
   */
  getGitHubStats(options?: RequestOptions): Promise<GitHubStats>;

  /**
   * Envia uma mensagem do formulario de contato.
   *
   * <p><strong>A unica escrita deste cliente</strong>, e a assimetria com as
   * leituras e deliberada em dois pontos.
   *
   * <p><strong>Nao retenta.</strong> As leituras retentam porque repetir um GET
   * nao muda nada do outro lado; repetir um POST pode gravar a mesma mensagem
   * duas vezes e disparar dois e-mails. E o caso do erro de rede em particular:
   * um `ApiError` de status 0 nao distingue "nao chegou" de "chegou e a resposta
   * se perdeu", entao a tentativa seguinte e um palpite com efeito colateral. A
   * promessa de que a mensagem nao se perde e da API - ela persiste antes de
   * tentar enviar o e-mail -, e nao de uma segunda tentativa daqui.
   *
   * <p><strong>Espera mais.</strong> Sem retentativa, o unico amortecedor contra
   * a hibernacao do plano gratuito e o tempo de uma tentativa so. Quem chama
   * passa um `timeoutMs` maior; o padrao curto do cliente foi calibrado para
   * leitura com visitante esperando, e aqui o visitante ja apertou um botao.
   *
   * <p>Resolve com `void`: a API responde <strong>202 sem corpo</strong>, porque
   * mensagem de contato nao tem representacao publica para devolver.
   *
   * <p>O <strong>429</strong> chega como `ApiError` com `retryAfterSeconds`
   * preenchido; o <strong>400</strong>, com o `detail` do Problem Details.
   */
  submitContact(message: ContactSubmission, options?: RequestOptions): Promise<void>;
}

/**
 * O corpo de uma escrita, embrulhado.
 *
 * <p>Um objeto de um campo so, e nao o valor cru, porque `undefined` e um corpo
 * legitimo em JSON e precisa ser distinguivel de "esta e uma leitura". Sem o
 * embrulho, `request(path, options, undefined)` seria ambiguo - e a ambiguidade
 * decidiria entre GET e POST.
 */
interface Corpo {
  readonly valor: unknown;
}

const DEFAULT_TIMEOUT_MS = 5_000;
const DEFAULT_RETRIES = 2;
const BASE_BACKOFF_MS = 150;

/**
 * Cliente tipado do contrato publicado.
 *
 * <p>Retry e timeout moram aqui, e nao em cada chamador, porque sao decisoes
 * sobre a mesma API: a de producao hiberna no free tier (ADR-0006), entao a
 * primeira requisicao depois de um periodo parado e lenta ou falha - e uma
 * segunda tentativa resolve o caso mais comum sem ninguem ver erro.
 */
export function createApiClient(options: ApiClientOptions): ApiClient {
  const {
    baseUrl,
    timeoutMs = DEFAULT_TIMEOUT_MS,
    retries = DEFAULT_RETRIES,
    headers = {},
  } = options;

  const raiz = baseUrl.replace(/\/+$/, '');

  async function request<T>(
    path: string,
    requestOptions: RequestOptions,
    corpo?: Corpo,
  ): Promise<T> {
    const url = `${raiz}${path}`;

    // Escrita nao retenta - ver o javadoc de submitContact. O numero fica em
    // zero em vez de o laco ganhar um `if`, para que exista **um** caminho de
    // requisicao e nao dois que precisem concordar.
    const maximoDeRetentativas = corpo === undefined ? retries : 0;

    for (let tentativa = 0; ; tentativa++) {
      try {
        return await executar<T>(url, requestOptions, corpo);
      } catch (cause) {
        // Cancelamento pedido por quem chamou nao e falha a ser contornada.
        if (requestOptions.signal?.aborted === true) throw cause;
        if (tentativa >= maximoDeRetentativas || !ehTemporario(cause)) throw cause;

        await espera(BASE_BACKOFF_MS * 2 ** tentativa);
      }
    }
  }

  async function executar<T>(
    url: string,
    requestOptions: RequestOptions,
    corpo?: Corpo,
  ): Promise<T> {
    let resposta: Response;

    try {
      resposta = await fetch(url, {
        headers: {
          Accept: 'application/json',
          ...(corpo === undefined ? {} : { 'Content-Type': 'application/json' }),
          ...headers,
          // Por ultimo: o cabecalho da chamada vence o fixo do cliente.
          ...requestOptions.headers,
        },
        signal: sinal(requestOptions.timeoutMs ?? timeoutMs, requestOptions.signal),
        // Espalhados condicionalmente: com exactOptionalPropertyTypes ligado,
        // passar `next: undefined` nao e o mesmo que nao passar `next`.
        ...(requestOptions.next ? { next: requestOptions.next } : {}),
        ...(corpo === undefined ? {} : { method: 'POST', body: JSON.stringify(corpo.valor) }),
      });
    } catch (cause) {
      throw new ApiError(0, url, `Falha de rede ao chamar ${url}`, { cause });
    }

    if (!resposta.ok) {
      throw new ApiError(resposta.status, url, await descrever(resposta, url), {
        retryAfterSeconds: segundosAteRetentar(resposta),
      });
    }

    // 202 e 204 nao trazem corpo, e `resposta.json()` num corpo vazio lanca
    // SyntaxError - um erro de parse no lugar de um sucesso. Quem chama uma
    // escrita ja declara `Promise<void>`, entao o undefined que sai daqui e o
    // valor certo com o tipo certo.
    if (semCorpo(resposta)) return undefined as T;

    return (await resposta.json()) as T;
  }

  return {
    getProfile(requestOptions: RequestOptions = {}) {
      return request<Profile>('/api/v1/profile', requestOptions);
    },

    listExperiences(requestOptions: RequestOptions = {}) {
      return request<Experience[]>('/api/v1/experiences', requestOptions);
    },

    listSkills(requestOptions: RequestOptions = {}) {
      return request<SkillCategory[]>('/api/v1/skills', requestOptions);
    },

    listProjects(requestOptions: RequestOptions = {}) {
      return request<ProjectSummary[]>('/api/v1/projects', requestOptions);
    },

    getProjectBySlug(slug: string, requestOptions: RequestOptions = {}) {
      // encodeURIComponent mesmo sabendo que slug valido nao tem nada a
      // escapar: o valor vem da URL do visitante, e concatenar entrada externa
      // num caminho sem escapar e como se monta um path traversal. Com o
      // escape, `../profile` chega a API como segmento literal e volta 400.
      return request<ProjectDetail>(`/api/v1/projects/${encodeURIComponent(slug)}`, requestOptions);
    },

    getGitHubStats(requestOptions: RequestOptions = {}) {
      return request<GitHubStats>('/api/v1/github/stats', requestOptions);
    },

    submitContact(message: ContactSubmission, requestOptions: RequestOptions = {}) {
      // `undefined` e nao `void` como argumento de tipo: `void` ali e o que a
      // regra no-invalid-void-type reprova, com razao - ele descreve "sem valor
      // de retorno", nao um valor. O metodo continua prometendo `Promise<void>`
      // na interface, e `Promise<undefined>` satisfaz isso.
      return request<undefined>('/api/v1/contact', requestOptions, { valor: message });
    },
  };
}

/**
 * Mensagem util a partir de um erro da API.
 *
 * A API responde RFC 9457, entao `detail` e a frase escrita para ser lida. Sem
 * ela sobra o status, que ao menos nao inventa explicacao.
 */
async function descrever(resposta: Response, url: string): Promise<string> {
  const inicio = `${String(resposta.status)} em ${url}`;

  try {
    const corpo: unknown = await resposta.json();
    if (typeof corpo === 'object' && corpo !== null && 'detail' in corpo) {
      const { detail } = corpo;
      if (typeof detail === 'string' && detail.length > 0) return `${inicio}: ${detail}`;
    }
  } catch {
    // Corpo ausente ou nao-JSON: o status ja diz o suficiente.
  }

  return inicio;
}

/**
 * Vale tentar de novo?
 *
 * 5xx e falha do outro lado, que costuma passar. 429 e pedido explicito de
 * espera. 4xx nao: requisicao errada segue errada na segunda vez, e repeti-la
 * so aumenta a conta de quem esta sendo chamado.
 */
function ehTemporario(cause: unknown): boolean {
  if (!(cause instanceof ApiError)) return false;
  return cause.status === 0 || cause.status === 429 || cause.status >= 500;
}

function sinal(timeoutMs: number, doChamador?: AbortSignal): AbortSignal {
  const porTempo = AbortSignal.timeout(timeoutMs);
  // Um sinal novo por tentativa: reaproveitar o anterior faria a segunda
  // tentativa nascer ja cancelada.
  return doChamador ? AbortSignal.any([porTempo, doChamador]) : porTempo;
}

/**
 * O `Retry-After` em segundos, quando ele veio e faz sentido.
 *
 * <p>O RFC 9110 permite duas formas: um numero de segundos ou uma data HTTP.
 * Esta API sempre manda a primeira, e so ela e lida - interpretar a data
 * exigiria confiar no relogio de quem recebe, e um relogio adiantado
 * produziria um "tente em -40s" que a tela nao sabe desenhar.
 */
function segundosAteRetentar(resposta: Response): number | null {
  const cabecalho = resposta.headers.get('Retry-After');
  if (cabecalho === null) return null;

  const segundos = Number(cabecalho);
  return Number.isInteger(segundos) && segundos >= 0 ? segundos : null;
}

/**
 * A resposta veio sem corpo?
 *
 * <p>O status decide primeiro, porque 204 e 205 <em>proibem</em> corpo. Os
 * outros dois testes cobrem o 202 desta API, que responde vazio por escolha e
 * nao por regra do protocolo - e sao dois porque nenhum sozinho basta: nem todo
 * servidor manda `Content-Length` numa resposta vazia, e a ausencia de
 * `Content-Type` e o que sobra quando nao ha o que tipar.
 */
function semCorpo(resposta: Response): boolean {
  return (
    resposta.status === 204 ||
    resposta.status === 205 ||
    resposta.headers.get('Content-Length') === '0' ||
    resposta.headers.get('Content-Type') === null
  );
}

function espera(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
