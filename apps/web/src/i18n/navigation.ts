import { routing, type Idioma } from '@/i18n/routing';

/**
 * O endereco publico de uma rota num idioma - {@code /projetos/finai} em portugues.
 *
 * <p><strong>Escrita aqui, e nao o {@code getPathname} do {@code createNavigation} do next-intl - e
 * a troca foi medida.</strong> O {@code createNavigation} cria tambem o {@code Link} da biblioteca,
 * que e componente de cliente, e importar o modulo basta para o build registrar esse {@code Link}: o
 * bundle de toda pagina passou a levar o {@code Link} e o pedaco do use-intl que ele usa, sem um
 * unico link dele na tela.
 *
 * <p>A regra e a do {@code as-needed} de i18n/routing: o idioma padrao fica sem prefixo, os demais o
 * levam. Quem desenha o link continua sendo o {@code next/link}, que ja esta no bundle.
 */
export function caminhoPublico({ href, locale }: { href: string; locale: Idioma }): string {
  // Comparado como string porque a regra vale para qualquer numero de idiomas;
  // com um so, a comparacao com o tipo literal seria sempre verdadeira.
  const padrao: string = routing.defaultLocale;
  if (locale === padrao) return href;
  return href === '/' ? `/${locale}` : `/${locale}${href}`;
}
