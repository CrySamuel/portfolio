import AxeBuilder from '@axe-core/playwright';
import { test as base, expect, type Page } from '@playwright/test';

/** O que vem de dentro do widget do Turnstile - inclusive os blob: que ele cria. */
const DO_TURNSTILE = /^(blob:)?https:\/\/challenges\.cloudflare\.com\//;

/**
 * O {@code test} de todos os arquivos daqui, com uma regra a mais: **nenhum erro no console**.
 *
 * <p>E a guarda que nao precisa de teste proprio para cada defeito. Quebra de hidratacao, violacao
 * da CSP ("Applying inline style violates the following Content Security Policy directive", no Chromium), excecao nao tratada e recurso que nao carregou
 * aparecem todos como erro no console - e o teste que passava pela pagina reprova, qualquer que
 * fosse o assunto dele. E o que faltava para trocar o {@code 'unsafe-inline'} do estilo por hash
 * sem medo de quebrar o tema em silencio (ADR-0012).
 *
 * <p>Duas excecoes, e so duas. A 404: o proprio documento responde 404, e o navegador o registra
 * como recurso que falhou - quem a testa declara o que tolera. E o que acontece dentro do widget do
 * Turnstile, que e iframe da Cloudflare: medido no commit 52, o WebKit registra falha num
 * {@code blob:} interno dele a cada carga. Nao e codigo deste repositorio, o mesmo criterio que o
 * tira do axe; e se o widget falhar de um jeito que nos afete - sem token -, o teste do envio
 * reprova por conta propria.
 */
export const test = base.extend<{ errosTolerados: readonly RegExp[]; console: string[] }>({
  errosTolerados: [[], { option: true }],

  console: [
    async ({ page, errosTolerados }, use) => {
      const erros: string[] = [];
      page.on('pageerror', (erro) => erros.push(`excecao: ${erro.message}`));
      page.on('console', (mensagem) => {
        if (mensagem.type() !== 'error') return;
        // O endereco junto: "Failed to load resource" sozinho nao diz qual recurso, e o WebKit
        // nem sempre poe o status no texto.
        const origem = mensagem.location().url;
        if (DO_TURNSTILE.test(origem)) return;
        const texto = `${mensagem.text()} [${origem}]`;
        if (!errosTolerados.some((padrao) => padrao.test(texto))) erros.push(texto);
      });

      await use(erros);

      expect(erros, 'erros no console do navegador').toEqual([]);
    },
    { auto: true },
  ],
});

export { expect };

/** O que a 404 de verdade escreve no console: o documento que respondeu 404. */
export const DOCUMENTO_404 = /status of 404/;

/**
 * As violacoes do axe na pagina, nos criterios que o plano exige (WCAG 2.2 AA) mais as boas
 * praticas.
 *
 * <p>O widget do Turnstile fica de fora. E um iframe da Cloudflare, que este repositorio nao
 * escreve e nao pode corrigir; o que e nosso em volta dele - o rotulo, a frase de protecao, o botao
 * - continua dentro da analise.
 */
export async function violacoesDeAcessibilidade(page: Page): Promise<string[]> {
  const resultado = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'])
    .exclude('iframe[src*="challenges.cloudflare.com"]')
    .analyze();

  return resultado.violations.map(
    (violacao) =>
      `${violacao.id} (${String(violacao.nodes.length)}): ${violacao.nodes
        .slice(0, 3)
        .map((no) => no.target.join(' '))
        .join(' | ')}`,
  );
}
