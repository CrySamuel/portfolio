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
  readonly id: string;
  readonly label: string;
}

export const NAV_ITEMS: readonly NavItem[] = [
  { id: 'sobre', label: 'Sobre' },
  { id: 'projetos', label: 'Projetos' },
  { id: 'skills', label: 'Skills' },
  { id: 'contato', label: 'Contato' },
];

/**
 * Os ids, derivados uma única vez.
 *
 * Precisa ser estável: `useActiveSection` recebe esta lista como dependência de
 * efeito, e um `NAV_ITEMS.map(...)` no corpo do componente devolveria um array
 * novo a cada render - o IntersectionObserver seria destruído e recriado sem
 * parar, o que anula justamente a razão de usar observer em vez de listener.
 */
export const NAV_IDS: readonly string[] = NAV_ITEMS.map((item) => item.id);

/** Id do <main>, alvo do skip link. */
export const MAIN_ID = 'conteudo';

/** Altura da navbar fixa, em pixels. Usada pelo recorte do observer. */
export const NAVBAR_HEIGHT = 64;

// SOCIAL_LINKS morava aqui e saiu no commit 22, como estava previsto: os
// perfis externos passaram a vir de GET /api/v1/profile. A duplicacao com
// R__seed_profile.sql tinha prazo, e o prazo era este.
