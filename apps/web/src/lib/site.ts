import 'server-only';

import { resolverRaiz } from '@/lib/origem';

/**
 * A raiz publica do site, em forma absoluta - a ordem das fontes e o motivo estao em
 * {@link resolverRaiz}. Este modulo e so de servidor; o {@code next.config} le a origem direto de
 * lib/origem.
 */
export const SITE_URL = new URL(resolverRaiz());
