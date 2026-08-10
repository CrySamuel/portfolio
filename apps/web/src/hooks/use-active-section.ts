'use client';

import { useEffect, useState } from 'react';

import { NAVBAR_HEIGHT } from '@/lib/navigation';

/**
 * Devolve o id da seção que está sendo lida agora.
 *
 * IntersectionObserver, e não listener de scroll: o observer é avaliado fora da
 * thread principal e só acorda quando o cruzamento muda. Um handler de scroll
 * dispararia dezenas de vezes por segundo durante a rolagem - justamente quando
 * a página menos pode gastar thread principal, porque é aí que o INP é medido.
 *
 * A `rootMargin` recorta a viewport na metade de cima, começando logo abaixo da
 * navbar fixa. Sem recorte nenhum, qualquer seção parcialmente visível conta, e
 * num monitor grande três estão visíveis ao mesmo tempo - a pergunta certa é "o
 * que está sob os olhos", não "o que está na tela".
 *
 * A faixa é generosa de propósito. Uma janela estreita - do tipo `-20% 0px -70%`
 * - deixa só 10% da altura da viewport, e entre duas seções ela fica **vazia**:
 * nenhum item aceso, ou o item errado aceso porque o estado anterior ficou.
 */
export function useActiveSection(ids: readonly string[]): string | null {
  const [ativo, setAtivo] = useState<string | null>(null);

  useEffect(() => {
    // As seções chegam ao longo dos MVPs seguintes. Observar só o que existe
    // evita que o hook quebre - e faz o menu funcionar parcialmente enquanto o
    // resto não chegou, em vez de não funcionar.
    const alvos = ids
      .map((id) => document.getElementById(id))
      .filter((el): el is HTMLElement => el !== null);

    if (alvos.length === 0) return;

    const visiveis = new Set<string>();

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) visiveis.add(entry.target.id);
          else visiveis.delete(entry.target.id);
        }

        // Com mais de uma seção na faixa, vale a última na ordem do documento:
        // é a que está mais abaixo, e portanto a que o leitor acabou de
        // alcançar descendo. A primeira seria a que ele está deixando.
        let atual: string | null = null;
        for (const id of ids) {
          if (visiveis.has(id)) atual = id;
        }
        if (atual !== null) setAtivo(atual);
      },
      { rootMargin: `-${String(NAVBAR_HEIGHT)}px 0px -55% 0px` },
    );

    alvos.forEach((el) => {
      observer.observe(el);
    });
    return () => {
      observer.disconnect();
    };
  }, [ids]);

  return ativo;
}
