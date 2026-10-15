import { randomInt } from 'node:crypto';

import type { Page } from '@playwright/test';

import { expect, test } from './fixtures';

/**
 * Uma origem por teste, no bloco de documentacao do IPv6 (RFC 3849).
 *
 * <p>A API limita cinco mensagens por hora por {@code X-Forwarded-For}. Sem origem propria, quatro
 * navegadores e algumas execucoes por hora esgotariam o balde, e o teste de sucesso passaria a
 * receber o 429 - uma falha que nao e do codigo. A rota so altera as requisicoes ao proprio site: o
 * cabecalho a mais numa requisicao a Cloudflare dispararia preflight de CORS no widget.
 */
async function comOrigemPropria(page: Page): Promise<void> {
  const origem = `2001:db8:${randomInt(0xffff).toString(16)}:${randomInt(0xffff).toString(16)}::1`;
  await page.route(
    (url) => url.hostname === 'localhost',
    (rota) =>
      rota.continue({ headers: { ...rota.request().headers(), 'x-forwarded-for': origem } }),
  );
}

test.describe('contato', () => {
  test.beforeEach(async ({ page }) => {
    await comOrigemPropria(page);
  });

  /*
   * Espacos passam pelo `required` do navegador e caem no `trim` do servidor: e o jeito de chegar
   * a validacao da Server Action, que e a que responde no idioma e liga o erro ao campo.
   */
  test('o servidor recusa campos em branco, ligando cada erro ao campo', async ({ page }) => {
    await page.goto('/#contato');
    const formulario = page.locator('#contato form');
    await formulario.getByLabel('Nome', { exact: true }).fill('   ');
    await formulario.getByLabel('E-mail', { exact: true }).fill('ana@exemplo.com');
    await formulario.getByLabel('Assunto', { exact: true }).fill('   ');
    await formulario.getByLabel('Mensagem', { exact: true }).fill('   ');
    await formulario.getByRole('button', { name: 'Enviar mensagem' }).click();

    await expect(page.locator('#contato').getByRole('alert')).toContainText(
      'Confira os campos marcados abaixo.',
    );
    const nome = formulario.getByLabel('Nome', { exact: true });
    await expect(nome).toHaveAttribute('aria-invalid', 'true');
    await expect(nome).toHaveAccessibleDescription('Escreva seu nome.');
    await expect(formulario.getByLabel('E-mail', { exact: true })).not.toHaveAttribute(
      'aria-invalid',
    );
  });

  test('em ingles, a recusa vem em ingles', async ({ page }) => {
    await page.goto('/en-US#contato');
    const formulario = page.locator('#contato form');
    await formulario.getByLabel('Name', { exact: true }).fill('   ');
    await formulario.getByLabel('Email', { exact: true }).fill('ana@example.com');
    await formulario.getByLabel('Subject', { exact: true }).fill('Role');
    await formulario.getByLabel('Message', { exact: true }).fill('Hello');
    await formulario.getByRole('button', { name: 'Send message' }).click();

    await expect(page.locator('#contato').getByRole('alert')).toContainText(
      'Check the fields marked below.',
    );
    await expect(formulario.getByLabel('Name', { exact: true })).toHaveAccessibleDescription(
      'Enter your name.',
    );
  });

  /*
   * O caminho inteiro: o widget com a chave de teste da Cloudflare entrega o token, a Server Action
   * o confere, e a API grava a mensagem. No CI o banco e descartavel; na maquina de
   * desenvolvimento, e o local.
   */
  test('envia, e anuncia o sucesso na regiao viva', async ({ page }) => {
    await page.goto('/#contato');
    const formulario = page.locator('#contato form');
    await formulario.getByLabel('Nome', { exact: true }).fill('Teste de ponta a ponta');
    await formulario.getByLabel('E-mail', { exact: true }).fill('e2e@exemplo.com');
    await formulario.getByLabel('Assunto', { exact: true }).fill('Playwright');
    await formulario
      .getByLabel('Mensagem', { exact: true })
      .fill('Mensagem enviada pelo teste de ponta a ponta.');

    await expect(formulario.locator('input[name="cf-turnstile-response"]')).toHaveValue(/.+/, {
      timeout: 20_000,
    });
    await formulario.getByRole('button', { name: 'Enviar mensagem' }).click();

    await expect(page.getByText('Mensagem enviada.')).toBeVisible({ timeout: 20_000 });
  });

  test.describe('sem JavaScript', () => {
    test.use({ javaScriptEnabled: false });

    test('o formulario funciona pelo envio nativo', async ({ page }) => {
      await page.goto('/#contato');
      const formulario = page.locator('#contato form');
      await formulario.getByLabel('Nome', { exact: true }).fill('Teste sem JavaScript');
      await formulario.getByLabel('E-mail', { exact: true }).fill('e2e@exemplo.com');
      await formulario.getByLabel('Assunto', { exact: true }).fill('Envio nativo');
      await formulario
        .getByLabel('Mensagem', { exact: true })
        .fill('Mensagem enviada sem JavaScript pelo teste.');
      await formulario.getByRole('button', { name: 'Enviar mensagem' }).click();

      await expect(page.getByText('Mensagem enviada.')).toBeVisible();
    });
  });
});
