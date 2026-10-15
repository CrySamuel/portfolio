import type messages from '@/i18n/messages/pt-BR.json';
import type { routing } from '@/i18n/routing';

/**
 * Chaves de mensagem tipadas a partir do arquivo em portugues.
 *
 * <p>{@code t('Hero.githb')} reprova a compilacao, e nao vira a chave impressa na tela. E o
 * portugues que define o formato porque e o idioma completo por construcao: o conteudo nasce nele.
 */
declare module 'next-intl' {
  interface AppConfig {
    Locale: (typeof routing.locales)[number];
    Messages: typeof messages;
  }
}
