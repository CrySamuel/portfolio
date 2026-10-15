import { defineConfig, devices } from '@playwright/test';

const PORTA = 3000;

/**
 * Testes de ponta a ponta e de acessibilidade, contra o site construido de verdade.
 *
 * <p><strong>O servidor e o {@code next start}, nunca o dev server.</strong> O dev server renderiza
 * por requisicao e mente sobre o que o build pre-renderiza (secao 4.43 do ESTADO); o que se testa
 * aqui e o que vai para producao. Por isso a build vem antes, fora daqui: com as chaves de teste do
 * Turnstile, que a Cloudflare publica para isso - a do site sempre aprova -, e com a API de pe, que
 * a build consulta para gerar as paginas.
 *
 * <p><strong>A API e a de verdade</strong>, com o banco semeado pelas migracoes - no CI, um Postgres
 * de servico e o jar da API (workflow e2e). Um duble daria dado inventado por quem escreve o teste,
 * e o portao confirmaria o que ele mesmo produziu: a razao que tirou a build do CI do web no commit
 * 23 vale aqui tambem.
 *
 * <p>Tres navegadores desktop, como pede o plano, e um celular - no Chromium, que e onde a largura
 * pequena importa para a navegacao e para o contraste medido pelo axe.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: process.env.CI !== undefined,
  // Uma retentativa no CI, e o relatorio a marca como instavel: o widget do Turnstile depende da
  // rede ate a Cloudflare, que e a unica parte deste teste fora do nosso controle.
  retries: process.env.CI === undefined ? 0 : 1,
  reporter: process.env.CI === undefined ? 'list' : [['github'], ['html', { open: 'never' }]],
  use: {
    baseURL: `http://localhost:${String(PORTA)}`,
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
    { name: 'celular', use: { ...devices['Pixel 7'] } },
  ],
  webServer: {
    command: `pnpm start --port ${String(PORTA)}`,
    url: `http://localhost:${String(PORTA)}`,
    reuseExistingServer: process.env.CI === undefined,
    timeout: 60_000,
  },
});
