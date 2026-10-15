import { z } from 'zod';

import { LIMITES } from '@/features/contact/schemas/contact-fields';

/** As chaves de validação, em `ContactValidation` nas mensagens. */
export type ChaveDeValidacao =
  | 'nameRequired'
  | 'nameTooLong'
  | 'emailRequired'
  | 'emailTooLong'
  | 'emailInvalid'
  | 'subjectRequired'
  | 'subjectTooLong'
  | 'messageRequired'
  | 'messageTooLong';

/** Quem traduz as mensagens de validação — o `getTranslations('ContactValidation')` da ação. */
export type TradutorDeValidacao = (chave: ChaveDeValidacao, valores?: { limite: number }) => string;

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
 *
 * <p><strong>Fábrica, e não constante</strong>, porque a mensagem depende do
 * idioma da requisição: a mesma Server Action atende o português e o inglês. As
 * frases moram nas mensagens; os limites continuam vindo de `contact-fields`.
 */
export function criarContactSchema(t: TradutorDeValidacao) {
  return z.object({
    name: z
      .string()
      .trim()
      .min(1, t('nameRequired'))
      .max(LIMITES.name, t('nameTooLong', { limite: LIMITES.name })),

    email: z
      .string()
      .trim()
      .min(1, t('emailRequired'))
      .max(LIMITES.email, t('emailTooLong', { limite: LIMITES.email }))
      .pipe(z.email(t('emailInvalid'))),

    subject: z
      .string()
      .trim()
      .min(1, t('subjectRequired'))
      .max(LIMITES.subject, t('subjectTooLong', { limite: LIMITES.subject })),

    message: z
      .string()
      .trim()
      .min(1, t('messageRequired'))
      .max(LIMITES.message, t('messageTooLong', { limite: LIMITES.message })),
  });
}

export type ContatoValidado = z.infer<ReturnType<typeof criarContactSchema>>;
