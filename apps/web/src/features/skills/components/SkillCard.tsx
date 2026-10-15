import { Badge } from '@portfolio/ui';
import type { Proficiency, Skill } from '@portfolio/api-client';
import { getTranslations } from 'next-intl/server';
import type { ReactNode } from 'react';

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
export async function SkillCard({ skill }: { skill: Skill }): Promise<ReactNode> {
  const { name, proficiency, yearsOfExperience } = skill;
  const t = await getTranslations('Skills');

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
          <span className="text-caption text-fg-subtle">
            {t('years', { anos: yearsOfExperience })}
          </span>
        )}
      </div>

      <Badge variant={VARIANTES[proficiency]} className="shrink-0">
        {t(proficiency)}
      </Badge>
    </li>
  );
}
