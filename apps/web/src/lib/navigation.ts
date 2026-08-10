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
 * A lista espelha `R__seed_profile.sql`, inclusive na ordem: enquanto as duas
 * existirem, divergir seria mostrar uma coisa no rodapé e servir outra pela API.
 * A duplicação tem prazo e é ela que o commit 22 elimina.
 *
 * Telefone não entra, por decisão do dono do portfólio. O e-mail entra porque
 * já consta do `package.json` e da autoria dos commits - publicá-lo aqui não
 * aumenta exposição, enquanto um número pessoal aumentaria e seria permanente
 * no histórico do git.
 */
export const SOCIAL_LINKS = [
  { platform: 'github', href: 'https://github.com/CrySamuel', label: 'Perfil no GitHub' },
  {
    platform: 'linkedin',
    href: 'https://www.linkedin.com/in/crystofer-samuel/',
    label: 'Perfil no LinkedIn',
  },
  {
    platform: 'email',
    href: 'mailto:crystoferdemetino@gmail.com',
    label: 'Enviar e-mail para Crystofer',
  },
] as const;
