'use client';

import { ThemeProvider as NextThemeProvider, useTheme } from 'next-themes';
import { useEffect, type ReactNode } from 'react';

/**
 * Cores de fundo dos dois temas, iguais ao --color-bg de tokens.css.
 *
 * Sao os unicos valores hexadecimais do lado do cliente, e a regra 7.8 nao os
 * alcanca: <meta name="theme-color"> nao aceita var(), o navegador le o atributo
 * antes de qualquer CSS. Se --color-bg mudar, mudam aqui junto.
 */
const THEME_COLOR = { dark: '#08080a', light: '#ffffff' } as const;

/**
 * Tema claro/escuro da F02.
 *
 * O anti-FOUC e do next-themes: ele injeta um script inline que le o
 * localStorage e a media query e escreve o atributo no <html> antes da primeira
 * pintura. E por isso que o tema tem de vir de biblioteca e nao de um efeito -
 * um useEffect roda depois da pintura, e o flash ja aconteceu.
 *
 * O CSS ja estava pronto desde o commit 10: tokens.css tem o bloco
 * [data-theme='light'] com a paleta clara completa, validada em contraste. Este
 * commit so liga o interruptor.
 */
export function ThemeProvider({ children }: { children: ReactNode }): ReactNode {
  return (
    <NextThemeProvider
      // Casa com o seletor [data-theme='light'] que tokens.css ja usa.
      attribute="data-theme"
      defaultTheme="system"
      enableSystem
      // Sem isto, trocar de tema anima cada cor da pagina pela transicao dos
      // componentes - dezenas de elementos interpolando ao mesmo tempo. O
      // next-themes injeta um CSS que desliga transicoes durante a troca.
      disableTransitionOnChange
      // Desligado de proposito: color-scheme ja e declarado em tokens.css, nos
      // dois temas. Deixar o next-themes escrever a mesma coisa em style inline
      // criaria uma segunda fonte de verdade, e a inline venceria a do token.
      enableColorScheme={false}
    >
      <ThemeColorMeta />
      {children}
    </NextThemeProvider>
  );
}

/**
 * Mantem o <meta name="theme-color"> em dia com o tema resolvido.
 *
 * O layout declara duas metas, uma por prefers-color-scheme: e o que tinge a
 * barra do navegador corretamente na primeira visita, antes de qualquer
 * JavaScript. So que a media query segue o sistema, e escolha manual nao mexe
 * no sistema - sem isto, quem forcasse o claro num aparelho escuro ficaria com
 * a pagina branca e a barra preta.
 *
 * As duas metas recebem a mesma cor, entao nao importa qual delas o navegador
 * escolhe. E quando o tema volta para "system", resolvedTheme passa a ser a
 * preferencia do sistema - o mesmo codigo continua certo, sem caso especial.
 */
function ThemeColorMeta(): null {
  const { resolvedTheme } = useTheme();

  useEffect(() => {
    if (resolvedTheme !== 'dark' && resolvedTheme !== 'light') return;

    const cor = THEME_COLOR[resolvedTheme];
    document.querySelectorAll<HTMLMetaElement>('meta[name="theme-color"]').forEach((meta) => {
      meta.content = cor;
    });
  }, [resolvedTheme]);

  return null;
}
