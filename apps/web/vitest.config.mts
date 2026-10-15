import { fileURLToPath } from 'node:url';

import { defineConfig } from 'vitest/config';

/**
 * Testes de unidade do web: logica pura e a Server Action, sem navegador.
 *
 * <p>O que precisa de pagina de verdade - rotas, hidratacao, acessibilidade - e do Playwright, em
 * {@code e2e/}. Aqui fica o que da para provar chamando uma funcao: a traducao de caminhos entre
 * idiomas e os ramos do envio do contato, que antes do commit 52 so tinham sido exercitados a mao.
 *
 * <p>Ambiente {@code node}, e nao {@code jsdom}: nada aqui renderiza componente, e o jsdom so
 * acrescentaria tempo e uma imitacao de DOM para testes que nao a usam.
 */
export default defineConfig({
  resolve: {
    // O mesmo alias do tsconfig. Sem ele, o primeiro import de '@/...' dentro do codigo testado
    // falharia na resolucao, e nao no teste.
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
});
