/**
 * As seções navegáveis da home, em ordem de aparição.
 *
 * Fonte única: a navbar do desktop, o menu mobile e o rastreio de seção ativa
 * leem daqui. Três listas separadas divergiriam no primeiro ajuste, e a
 * divergência é do tipo que não quebra nada - só faz um link parar de acender.
 *
 * `id` é a âncora no documento e a chave do IntersectionObserver.
 */
export interface NavItem {
  readonly id: NavId;
  readonly label: string;
}

// A secao do GitHub **nao** entra aqui, e a ausencia foi medida. Ela e a unica
// da home que some quando um terceiro esta fora do ar (ver GitHubSection), e um
// item de navbar apontando para ela viraria ancora morta justamente no cenario
// de falha - conferido: com o GitHub inalcancavel, `#github` fica sem alvo no
// documento. A navbar descreve a estrutura da pagina, e essa descricao precisa
// ser verdadeira sempre. A seccao mantem o `id`, entao link direto continua
// funcionando.
export const NAV_IDS = ['sobre', 'projetos', 'skills', 'contato'] as const;

/**
 * Os ids sao a ancora no documento e a chave da mensagem com o rotulo -
 * {@code Navigation.sobre}. Iguais nos dois idiomas: o realce da navbar depende
 * deles, e ancora nao pesa em busca. O rotulo e que muda, e vem traduzido pelo
 * layout, porque a navbar e componente de cliente.
 *
 * <p>Precisa ser estavel: {@code useActiveSection} recebe esta lista como
 * dependencia de efeito, e um array montado no corpo do componente seria novo a
 * cada render - o IntersectionObserver seria destruido e recriado sem parar.
 */
export type NavId = (typeof NAV_IDS)[number];

/** Id do <main>, alvo do skip link. */
export const MAIN_ID = 'conteudo';

/** Altura da navbar fixa, em pixels. Usada pelo recorte do observer. */
export const NAVBAR_HEIGHT = 64;

// SOCIAL_LINKS morava aqui e saiu no commit 22, como estava previsto: os
// perfis externos passaram a vir de GET /api/v1/profile. A duplicacao com
// R__seed_profile.sql tinha prazo, e o prazo era este.
