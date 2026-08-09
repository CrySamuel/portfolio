'use client';

import { Button } from '@portfolio/ui';
import { useTheme } from 'next-themes';
import type { ReactNode } from 'react';

/**
 * Alterna claro e escuro (secao 8.3).
 *
 * Nada aqui depende de estado do React para decidir o que mostrar, e essa e a
 * decisao central do componente. O caminho usual - um `mounted` que so libera o
 * conteudo depois da hidratacao - existe para fugir do erro de hidratacao, ja
 * que o servidor nao sabe qual tema o navegador vai aplicar. O preco e visivel:
 * ou o botao aparece vazio no primeiro quadro, ou aparece com o icone errado.
 *
 * Aqui os dois icones e os dois rotulos sao sempre renderizados, identicos no
 * servidor e no cliente - nao ha o que divergir -, e quem esconde um dos pares
 * e o CSS, a partir do atributo que o script anti-FOUC ja escreveu no <html>.
 * Sem efeito, sem quadro intermediario, sem deslocamento.
 *
 * O par visivel por padrao e o do tema escuro, que e o padrao do site: sem
 * JavaScript nenhum o atributo nunca aparece, e o botao continua coerente.
 */
export function ThemeToggle(): ReactNode {
  const { setTheme, resolvedTheme } = useTheme();

  return (
    <Button
      variant="ghost"
      size="icon"
      // resolvedTheme so fica indefinido antes da montagem, e antes da montagem
      // nao existe clique - o React ainda nao ligou o handler.
      onClick={() => {
        setTheme(resolvedTheme === 'light' ? 'dark' : 'light');
      }}
    >
      <SunIcon className="[[data-theme=light]_&]:hidden" />
      <MoonIcon className="hidden [[data-theme=light]_&]:block" />

      {/*
        O nome acessivel anuncia a ACAO, nao o estado (secao 11): "Ativar tema
        claro", e nao "Tema escuro". Quem usa leitor de tela precisa saber o que
        o botao faz, nao o que ele ja e.

        display:none tira o texto da arvore de acessibilidade, entao exatamente
        um dos dois rotulos e anunciado - nunca os dois.
      */}
      <span className="sr-only [[data-theme=light]_&]:hidden">Ativar tema claro</span>
      <span className="sr-only hidden [[data-theme=light]_&]:block">Ativar tema escuro</span>
    </Button>
  );
}

/** Icones decorativos: quem nomeia o botao sao os rotulos acima. */
function SunIcon({ className }: { className?: string }): ReactNode {
  return (
    <svg
      aria-hidden="true"
      className={className}
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
    >
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
    </svg>
  );
}

function MoonIcon({ className }: { className?: string }): ReactNode {
  return (
    <svg
      aria-hidden="true"
      className={className}
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M21 12.79A9 9 0 1 1 11.21 3a7 7 0 0 0 9.79 9.79Z" />
    </svg>
  );
}
