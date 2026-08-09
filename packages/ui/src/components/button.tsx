import { Slot, Slottable } from '@radix-ui/react-slot';
import { cva, type VariantProps } from 'class-variance-authority';
import type { ComponentPropsWithRef, ReactNode } from 'react';

import { cn } from '../lib/cn';

/**
 * Variantes da secao 8.2 do plano.
 *
 * Nenhuma cor literal aparece aqui - so utilidades geradas a partir dos tokens
 * do commit 10 (regra 7.8). Trocar --color-accent no tema muda o botao inteiro
 * sem editar este arquivo.
 *
 * O anel de foco tambem nao aparece: ele e aplicado uma unica vez em
 * :focus-visible pelo globals.css, a partir do token --focus-ring. Redeclarar
 * aqui criaria uma segunda fonte de verdade para uma regra que a secao 7.5
 * chama de nao negociavel.
 */
const buttonVariants = cva(
  [
    'relative inline-flex items-center justify-center gap-2',
    'rounded-md font-medium whitespace-nowrap',
    'transition-colors duration-(--duration-fast) ease-(--ease-out)',
    // O cursor de "proibido" mente sobre o estado: o botao continua focavel e
    // continua anunciado. Basta o esmaecimento, que vale para disabled e para
    // loading (via aria-disabled).
    'aria-disabled:pointer-events-none aria-disabled:opacity-50',
    'disabled:pointer-events-none disabled:opacity-50',
  ],
  {
    variants: {
      variant: {
        primary: 'bg-accent-solid text-white hover:bg-accent-solid/90',
        secondary: 'bg-surface-2 text-fg hover:bg-surface-2/80',
        outline: 'border border-border-interactive text-fg hover:bg-surface-2',
        ghost: 'text-fg-muted hover:bg-surface-2 hover:text-fg',
        link: 'text-accent underline-offset-4 hover:underline',
      },
      size: {
        // O alvo de toque de 44x44 da secao 11 vale para os tres tamanhos
        // clicaveis. O sm mede 36px de altura visual, entao a area sensivel e
        // estendida por um ::after invisivel - a alternativa seria engordar o
        // botao e perder o proposito do tamanho.
        sm: 'h-9 px-3 text-body-sm after:absolute after:inset-x-0 after:top-1/2 after:h-11 after:-translate-y-1/2',
        md: 'h-11 px-4 text-body-sm',
        lg: 'h-12 px-6 text-body',
        icon: 'size-11 p-0',
      },
    },
    defaultVariants: { variant: 'primary', size: 'md' },
  },
);

export interface ButtonProps
  extends ComponentPropsWithRef<'button'>, VariantProps<typeof buttonVariants> {
  /** Renderiza o filho no lugar do <button>, herdando os estilos. */
  asChild?: boolean;
  /** Mostra o indicador de progresso e bloqueia a acao, sem perder o foco. */
  loading?: boolean;
}

/**
 * O `ref` e uma prop comum, e nao um forwardRef.
 *
 * O plano pede forwardRef (contrato 8.2, item 1), e o motivo citado - Radix e
 * bibliotecas de formulario precisam alcancar o no do DOM - continua valendo. O
 * que mudou foi o mecanismo: no React 19 componentes de funcao recebem `ref`
 * diretamente, e forwardRef entrou em depreciacao. O contrato e o mesmo com uma
 * camada a menos.
 */
export function Button({
  className,
  variant,
  size,
  asChild = false,
  loading = false,
  onClick,
  type,
  children,
  ...props
}: ButtonProps): ReactNode {
  const Component = asChild ? Slot : 'button';

  return (
    <Component
      // Carregando nao usa o `disabled` nativo: ele retiraria o botao da ordem
      // de tabulacao no instante em que a acao comeca, e quem acabou de
      // aciona-lo perderia o foco para o <body> - com ele, o lugar na pagina e
      // o contexto anunciado pelo leitor de tela. O aria-disabled comunica o
      // mesmo estado mantendo o foco.
      //
      // Um `disabled` vindo de quem compoe e outra coisa: e uma escolha
      // explicita pela semantica nativa, entao passa direto em ...props.
      aria-disabled={loading ? true : undefined}
      aria-busy={loading ? true : undefined}
      className={cn(buttonVariants({ variant, size }), className)}
      {...props}
      // O pointer-events da variante barra o mouse, mas nao o teclado: o botao
      // segue focavel enquanto carrega - essa e a intencao -, e Enter num botao
      // focado dispara a acao do mesmo jeito. Faltam as duas travas do teclado.
      //
      // A primeira e a acao padrao. Um botao sem `type` dentro de um <form> e
      // submit, entao Enter reenviaria o formulario com a primeira submissao
      // ainda em voo. Trocar por "button" enquanto carrega remove esse caminho
      // sem depender de JavaScript nenhum - e isso importa: o Button e Server
      // Component por padrao, e uma funcao passada aqui na renderizacao do
      // servidor derruba a pagina ("Event handlers cannot be passed to Client
      // Component props").
      type={asChild ? undefined : loading ? 'button' : type}
      // A segunda e o handler de quem compoe, e so existe quando ele existe -
      // dai o undefined no lugar de um wrapper incondicional. Quem passa
      // onClick ja esta num Client Component, entao aqui a funcao e legitima.
      onClick={
        onClick
          ? (event) => {
              if (loading) {
                event.preventDefault();
                event.stopPropagation();
                return;
              }
              onClick(event);
            }
          : undefined
      }
    >
      {loading ? <Spinner /> : null}
      {/*
        Slottable, e nao {children} direto. Com asChild o Slot exige um unico
        filho elemento, e spinner + children sao dois - o componente quebrava em
        tempo de renderizacao com `Slot failed to slot onto its children`,
        mesmo com loading falso, porque o `null` ja conta como filho. O
        Slottable marca qual dos filhos e o alvo da fusao e deixa o resto ao
        redor. Sem asChild ele apenas repassa os filhos.
      */}
      <Slottable>{children}</Slottable>
    </Component>
  );
}

/**
 * O rotulo permanece visivel ao lado do indicador: troca-lo por "Carregando"
 * mudaria a largura do botao e deslocaria o que estiver ao lado.
 *
 * Sob prefers-reduced-motion a regra global do globals.css zera a duracao da
 * animacao e o circulo fica parado. E o comportamento desejado - quem pediu
 * menos movimento nao deveria receber um giro perpetuo -, e o estado continua
 * sendo comunicado pelo aria-busy e pelo esmaecimento, que nao dependem de
 * animacao nenhuma.
 */
function Spinner(): ReactNode {
  return (
    <svg
      aria-hidden="true"
      className="size-4 shrink-0 animate-spin"
      viewBox="0 0 24 24"
      fill="none"
    >
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeOpacity="0.25" strokeWidth="3" />
      <path
        d="M22 12a10 10 0 0 0-10-10"
        stroke="currentColor"
        strokeWidth="3"
        strokeLinecap="round"
      />
    </svg>
  );
}

export { buttonVariants };
