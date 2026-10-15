import { DOCUMENTO_404, expect, test } from './fixtures';

test.describe('projetos', () => {
  test('o catalogo filtra por tecnologia, e "Todos" desfaz', async ({ page }) => {
    await page.goto('/projetos');
    const cards = page.getByRole('main').getByRole('article');
    const total = await cards.count();
    expect(total).toBeGreaterThan(1);

    const filtro = page.getByRole('list', { name: 'Filtrar por tecnologia' });
    const tecnologia = filtro.getByRole('button').nth(1);
    const nome = (await tecnologia.textContent())?.trim() ?? '';
    await tecnologia.click();

    await expect(tecnologia).toHaveAttribute('aria-pressed', 'true');
    const visiveis = await cards.count();
    expect(visiveis).toBeGreaterThan(0);
    expect(visiveis).toBeLessThanOrEqual(total);
    for (const card of await cards.all()) await expect(card).toContainText(nome);

    await filtro.getByRole('button', { name: 'Todos' }).click();
    await expect(cards).toHaveCount(total);
  });

  test('o card leva ao projeto, e o projeto de volta ao catalogo', async ({ page }) => {
    await page.goto('/projetos');
    const primeiro = page.getByRole('main').getByRole('article').first().getByRole('link');
    const titulo = (await primeiro.textContent())?.trim() ?? '';

    await primeiro.click();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(titulo);
    for (const bloco of ['Problema', 'Solução', 'Resultado']) {
      await expect(page.getByRole('heading', { level: 2, name: bloco })).toBeVisible();
    }

    await page.getByRole('link', { name: 'Todos os projetos' }).click();
    await expect(page).toHaveURL('/projetos');
  });

  test('o projeto em ingles mora no endereco em ingles', async ({ page }) => {
    const resposta = await page.goto('/en-US/projects/finai');

    expect(resposta?.status()).toBe(200);
    await expect(page.locator('html')).toHaveAttribute('lang', 'en-US');
    await expect(page.getByRole('heading', { level: 2, name: 'Problem' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'All projects' })).toHaveAttribute(
      'href',
      '/en-US/projects',
    );
  });

  test.describe('404', () => {
    test.use({ errosTolerados: [DOCUMENTO_404] });

    test('projeto inexistente responde 404, com a tela propria', async ({ page }) => {
      const resposta = await page.goto('/projetos/nao-existe');

      expect(resposta?.status()).toBe(404);
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Este projeto não existe');
      await expect(page.getByRole('link', { name: 'Ver todos os projetos' })).toHaveAttribute(
        'href',
        '/projetos',
      );
    });

    test('endereco sem rota responde 404 dentro do layout, nos dois idiomas', async ({ page }) => {
      for (const [rota, titulo] of [
        ['/rota-inexistente', 'Esta página não existe'],
        ['/en-US/rota-inexistente', "This page doesn't exist"],
      ] as const) {
        const resposta = await page.goto(rota);
        expect(resposta?.status()).toBe(404);
        await expect(page.getByRole('heading', { level: 1 })).toHaveText(titulo);
        await expect(page.getByRole('navigation').first()).toBeVisible();
      }
    });
  });
});
