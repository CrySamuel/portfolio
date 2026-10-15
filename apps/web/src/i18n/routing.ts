// Import so de tipo, e o efeito dele e de ordem, e nao de uso - ver LocaleDoNextIntl, no fim.
import type { Locale } from 'next-intl';
import { defineRouting } from 'next-intl/routing';

/**
 * Os idiomas do site e a forma das URLs (ADR-0013).
 *
 * <p><strong>{@code as-needed}: o idioma padrao nao tem prefixo.</strong> O portugues continua em
 * {@code /} e {@code /projetos}, exatamente onde sempre esteve - no curriculo, no LinkedIn e nos
 * cartoes ja compartilhados. Por dentro, as rotas moram em {@code app/[locale]}, e o middleware
 * reescreve o endereco sem prefixo para a rota do idioma padrao. Um endereco com o prefixo do padrao
 * ({@code /pt-BR/...}) redireciona para o sem prefixo: duas URLs para a mesma pagina seriam conteudo
 * duplicado para o buscador.
 *
 * <p><strong>Sem os cabecalhos {@code Link} de alternativas.</strong> O middleware do next-intl
 * pode anuncia-las por cabecalho HTTP, e o {@code hreflang} ja sai na metadata de cada pagina. Duas
 * fontes para a mesma declaracao divergiriam no primeiro ajuste de uma delas.
 */
export const routing = defineRouting({
  locales: ['pt-BR'],
  defaultLocale: 'pt-BR',
  localePrefix: 'as-needed',
  alternateLinks: false,
});

export type Idioma = (typeof routing.locales)[number];

/**
 * Se o valor e um dos idiomas do site - o {@code hasLocale} do next-intl, escrito aqui porque ele
 * mora no pacote raiz, que o lint proibe (ver eslint.config.mjs).
 */
export function ehIdioma(valor: string | undefined): valor is Idioma {
  return routing.locales.some((idioma) => idioma === valor);
}

/**
 * O idioma como o next-intl o enxerga - o mesmo {@link Idioma}, pela declaracao de i18n/types.d.ts.
 *
 * <p><strong>Existe pelo import, e a razao e uma medicao do commit 51.</strong> A declaracao chega
 * aos tipos do next-intl por um {@code export *}, e o TypeScript so a junta quando resolve o pacote
 * raiz. Se antes disso algum arquivo pedir um tipo a next-intl/server - o {@code getLocale}, o
 * {@code getTranslations} -, o resultado fica guardado sem a declaracao, para o programa inteiro:
 * idioma vira {@code string} e {@code t('chave-errada')} compila calado. Enquanto os componentes
 * importavam do pacote raiz, a ordem dava certo por acaso; quando o lint o proibiu, ela deixou de
 * dar.
 *
 * <p>Este arquivo esta entre os primeiros que o compilador confere - quase todo o resto do i18n
 * depende dele -, e o import no topo forca a resolucao antes de qualquer outro. Se a ordem um dia
 * mudar, o sintoma e alto: o idioma de {@code getLocale()} deixa de caber em {@link Idioma}, e os
 * lugares que passam um para o outro reprovam a compilacao.
 */
export type LocaleDoNextIntl = Locale;
