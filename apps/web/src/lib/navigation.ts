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

/**
 * Perfis externos — **provisório**.
 *
 * O conteúdo real do perfil vem da API a partir do commit 22, e esta constante
 * morre lá. Até então fica aqui, e não dentro do rodapé, porque componente não
 * guarda dado (regra 6 da seção 8.7) e porque um único lugar é mais fácil de
 * apagar do que uma prop espalhada.
 *
 * Só o GitHub entra por enquanto: é a única URL que dá para confirmar a partir
 * do próprio repositório. LinkedIn e e-mail dependem de decisão do dono do
 * portfólio - publicar um endereço de e-mail é escolha dele, não do código.
 */
export const SOCIAL_LINKS = [
  { platform: 'github', href: 'https://github.com/CrySamuel', label: 'Perfil no GitHub' },
] as const;
