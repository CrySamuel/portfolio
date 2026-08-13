import { Badge } from '@portfolio/ui';
import type { Proficiency, Skill } from '@portfolio/api-client';
import type { ReactNode } from 'react';

/**
 * Rotulo em portugues de cada nivel.
 *
 * <strong>O texto e o que comunica o nivel, e nao a cor.</strong> E o criterio
 * 1.4.1 da WCAG e um dos aceites da F05: quem nao distingue as cores, quem usa
 * leitor de tela e quem imprime a pagina recebem a mesma informacao. A variante
 * do selo apenas reforca, para quem enxerga, o que a palavra ja diz.
 *
 * O mapa e exaustivo por construcao - `Record<Proficiency, ...>` sobre a uniao
 * literal do contrato. Um nivel novo na API reprova o build do front, em vez de
 * chegar como rotulo vazio na tela.
 */
const ROTULOS: Record<Proficiency, string> = {
  basic: 'Básico',
  intermediate: 'Intermediário',
  advanced: 'Avançado',
};

/**
 * Variante visual por nivel, do mais discreto ao mais destacado.
 *
 * Nao ha vermelho nem amarelo aqui de proposito: sao cores de alerta na paleta,
 * e "nivel basico" nao e um problema a ser sinalizado.
 */
const VARIANTES: Record<Proficiency, 'neutral' | 'outline' | 'accent'> = {
  basic: 'neutral',
  intermediate: 'outline',
  advanced: 'accent',
};

/**
 * Uma competencia: nome, nivel em texto e, quando declarado, o tempo.
 *
 * <p>Server Component, sem estado - custo zero no bundle do cliente.
 *
 * <p><strong>Sem barra de percentual.</strong> E decisao de produto do plano:
 * numeros como "Java 85%" sao arbitrarios, indefensaveis numa entrevista e
 * cairam em descredito. O que fica e rotulo textual e, quando ha, anos.
 */
export function SkillCard({ skill }: { skill: Skill }): ReactNode {
  const { name, proficiency, yearsOfExperience } = skill;

  return (
    <li className="flex items-center justify-between gap-3 rounded-md border border-border bg-surface px-4 py-3">
      <div className="flex min-w-0 flex-col gap-0.5">
        <span className="truncate text-body text-fg">{name}</span>

        {/*
          O contrato declara yearsOfExperience como `number | null`, e o tipo
          gerado obriga a tratar o caso. Ausencia e diferente de zero: zero e de
          quem comecou agora, e aparece como "menos de 1 ano".
        */}
        {yearsOfExperience === null ? null : (
          <span className="text-caption text-fg-subtle">{formatarTempo(yearsOfExperience)}</span>
        )}
      </div>

      <Badge variant={VARIANTES[proficiency]} className="shrink-0">
        {ROTULOS[proficiency]}
      </Badge>
    </li>
  );
}

/** Zero vira "menos de 1 ano" - "0 anos" leria como ausencia, que e outra coisa. */
function formatarTempo(anos: number): string {
  if (anos === 0) return 'menos de 1 ano';
  return anos === 1 ? '1 ano' : `${String(anos)} anos`;
}
