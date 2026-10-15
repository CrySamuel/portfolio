import { expect, test } from './fixtures';

test.describe('home', () => {
  test('carrega as secoes da navegacao, no idioma padrao', async ({ page }) => {
    await page.goto('/');

    await expect(page.locator('html')).toHaveAttribute('lang', 'pt-BR');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    for (const secao of ['sobre', 'projetos', 'skills', 'contato']) {
      await expect(page.locator(`#${secao}`)).toBeAttached();
    }
  });

  test('o skip link e o primeiro foco e leva o foco ao conteudo', async ({ page, browserName }) => {
    // O WebKit, como o Safari, nao para em link com Tab sem uma preferencia do sistema ligada.
    // Nao e defeito da pagina, e o mesmo teste nos outros motores cobre o comportamento.
    test.skip(browserName === 'webkit', 'Tab do WebKit pula links por padrao');

    await page.goto('/');
    await page.keyboard.press('Tab');

    const atalho = page.getByRole('link', { name: 'Pular para o conteúdo' });
    await expect(atalho).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(page.locator('#conteudo')).toBeFocused();
  });

  /*
   * A guarda da troca de tema sob a CSP. Se o estilo que o next-themes injeta for barrado - o risco
   * de trocar o 'unsafe-inline' do style-src por hash (ADR-0012) -, o navegador escreve a recusa
   * no console, e o fixture reprova o teste mesmo que o atributo mude.
   */
  test('o tema alterna, persiste e nao viola a CSP', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.goto('/');
    const raiz = page.locator('html');
    await expect(raiz).toHaveAttribute('data-theme', 'dark');

    await page.getByRole('button', { name: 'Ativar tema claro' }).click();
    await expect(raiz).toHaveAttribute('data-theme', 'light');

    await page.reload();
    await expect(raiz).toHaveAttribute('data-theme', 'light');
  });

  test('o seletor leva a mesma pagina no outro idioma, e volta', async ({ page }) => {
    await page.goto('/projetos/finai');

    await page.getByRole('link', { name: /Read in English/ }).click();
    await expect(page).toHaveURL('/en-US/projects/finai');
    await expect(page.locator('html')).toHaveAttribute('lang', 'en-US');

    await page.getByRole('link', { name: /Ler em português/ }).click();
    await expect(page).toHaveURL('/projetos/finai');
    await expect(page.locator('html')).toHaveAttribute('lang', 'pt-BR');
  });
});
