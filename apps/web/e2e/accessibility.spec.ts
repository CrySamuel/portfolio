import { DOCUMENTO_404, expect, test, violacoesDeAcessibilidade } from './fixtures';

/** Toda rota do site, nos dois idiomas, com o status que ela deve responder. */
const ROTAS = [
  ['/', 200],
  ['/projetos', 200],
  ['/projetos/finai', 200],
  ['/en-US', 200],
  ['/en-US/projects', 200],
  ['/en-US/projects/finai', 200],
  ['/rota-inexistente', 404],
  ['/projetos/nao-existe', 404],
  ['/en-US/rota-inexistente', 404],
] as const;

/*
 * Zero violacoes do axe em toda rota, nos dois temas - o criterio da DoD do MVP 6. Os projetos do
 * Playwright dao os tres motores e a largura de celular.
 *
 * O tema vem da preferencia do sistema, emulada antes de a pagina abrir, e nao de um clique no
 * botao: a troca dispara a transicao de cor, e contraste medido no meio dela mente (secao 4.46 do
 * ESTADO - medido de novo no commit 51, com 42 falsos).
 */
for (const tema of ['light', 'dark'] as const) {
  test.describe(`acessibilidade no tema ${tema}`, () => {
    test.use({ colorScheme: tema, errosTolerados: [DOCUMENTO_404] });

    for (const [rota, status] of ROTAS) {
      test(rota, async ({ page }) => {
        const resposta = await page.goto(rota);

        expect(resposta?.status()).toBe(status);
        await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
        await expect(page.locator('html')).toHaveAttribute('data-theme', tema);
        expect(await violacoesDeAcessibilidade(page)).toEqual([]);
      });
    }
  });
}
