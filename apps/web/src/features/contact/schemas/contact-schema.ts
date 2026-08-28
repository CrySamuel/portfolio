import { z } from 'zod';

import { LIMITES } from '@/features/contact/schemas/contact-fields';

/**
 * A forma de uma mensagem de contato válida.
 *
 * <p><strong>As mensagens de erro são escritas para serem lidas por quem
 * digitou</strong>, e não para descrever a regra violada. "O e-mail tem formato
 * inválido" diz o que a API responderia; "Confira o e-mail — parece faltar
 * alguma coisa" diz o que a pessoa faz a seguir. As duas pontas recusam o mesmo
 * valor; só uma delas fala com gente.
 *
 * <p><strong>Não há comprimento mínimo além de "não vazio", e a ausência é
 * deliberada.</strong> Exigir, digamos, 20 caracteres na mensagem inventaria uma
 * regra que a API aceita — o formulário passaria a recusar o que o backend
 * grava, e a divergência apareceria como um campo que não deixa enviar sem
 * motivo visível. Onde o servidor não impõe piso, este arquivo não inventa um.
 */
export const contactSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Escreva seu nome.')
    .max(LIMITES.name, `O nome precisa caber em ${String(LIMITES.name)} caracteres.`),

  email: z
    .string()
    .trim()
    .min(1, 'Escreva seu e-mail — é por ele que a resposta volta.')
    .max(LIMITES.email, `O e-mail precisa caber em ${String(LIMITES.email)} caracteres.`)
    .pipe(z.email('Confira o e-mail: parece faltar alguma coisa.')),

  subject: z
    .string()
    .trim()
    .min(1, 'Escreva um assunto.')
    .max(LIMITES.subject, `O assunto precisa caber em ${String(LIMITES.subject)} caracteres.`),

  message: z
    .string()
    .trim()
    .min(1, 'Escreva a mensagem.')
    .max(LIMITES.message, `A mensagem precisa caber em ${String(LIMITES.message)} caracteres.`),
});

export type ContatoValidado = z.infer<typeof contactSchema>;
