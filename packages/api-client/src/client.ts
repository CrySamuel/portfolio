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

/** Resposta que nao foi 2xx, ou que nao chegou. */
export class ApiError extends Error {
  /** `0` quando a requisicao nem chegou a ter resposta (rede, timeout). */
  readonly status: number;
  readonly url: string;

  constructor(status: number, url: string, message: string, options?: { cause?: unknown }) {
    super(message, options);
    this.name = 'ApiError';
    this.status = status;
    this.url = url;
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

  async function request<T>(path: string, requestOptions: RequestOptions): Promise<T> {
    const url = `${raiz}${path}`;

    for (let tentativa = 0; ; tentativa++) {
      try {
        return await executar<T>(url, requestOptions);
      } catch (cause) {
        // Cancelamento pedido por quem chamou nao e falha a ser contornada.
        if (requestOptions.signal?.aborted === true) throw cause;
        if (tentativa >= retries || !ehTemporario(cause)) throw cause;

        await espera(BASE_BACKOFF_MS * 2 ** tentativa);
      }
    }
  }

  async function executar<T>(url: string, requestOptions: RequestOptions): Promise<T> {
    let resposta: Response;

    try {
      resposta = await fetch(url, {
        headers: { Accept: 'application/json', ...headers },
        signal: sinal(requestOptions.timeoutMs ?? timeoutMs, requestOptions.signal),
        // Espalhado condicionalmente: com exactOptionalPropertyTypes ligado,
        // passar `next: undefined` nao e o mesmo que nao passar `next`.
        ...(requestOptions.next ? { next: requestOptions.next } : {}),
      });
    } catch (cause) {
      throw new ApiError(0, url, `Falha de rede ao chamar ${url}`, { cause });
    }

    if (!resposta.ok) {
      throw new ApiError(resposta.status, url, await descrever(resposta, url));
    }

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

function espera(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
