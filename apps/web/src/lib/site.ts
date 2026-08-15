import 'server-only';

/**
 * A raiz publica do site, em forma absoluta.
 *
 * <p>Existe por causa de um requisito que so aparece fora do navegador de quem
 * visita: <strong>crawler de rede social nao resolve URL relativa</strong>. O
 * WhatsApp, o LinkedIn e o X leem o {@code og:image} do HTML e vao busca-lo por
 * conta propria, sem a pagina ao redor - se o endereco vier como
 * {@code /projetos/finai/opengraph-image}, nao ha contra o que resolve-lo, e o
 * link compartilhado aparece sem imagem. E por isso que o Next exige um
 * {@code metadataBase} para tornar absolutos os enderecos de metadata.
 *
 * <p><strong>Nao ha valor fixo no codigo, e a razao e que o endereco vai
 * mudar.</strong> Hoje o site responde num subdominio gratuito da Vercel; o
 * dominio proprio esta decidido e adiado. Um literal aqui seria uma segunda
 * fonte de verdade para algo que ja e configuracao de deploy, e ficaria errado
 * em silencio - sem quebrar build nem teste, so mandando os crawlers para o
 * lugar antigo.
 *
 * <p>A ordem de resolucao vai do mais explicito ao mais desesperado:
 *
 * <ol>
 *   <li>{@code SITE_URL} - cadastrada a mao, e a unica que sobrevive a troca de
 *       hospedagem. E a que o commit 50 vai exigir, quando o sitemap e o
 *       {@code robots.txt} passarem a depender dela;
 *   <li>{@code VERCEL_PROJECT_PRODUCTION_URL} - o dominio de producao do
 *       projeto, estavel entre deploys;
 *   <li>{@code VERCEL_URL} - o endereco daquele deploy especifico. Funciona,
 *       mas muda a cada publicacao: serve de rede de seguranca, nao de escolha;
 *   <li>{@code http://localhost:3000} - desenvolvimento.
 * </ol>
 *
 * <p>Nenhuma das tres passa pelo {@code env.ts} de proposito. Aquele modulo
 * derruba a aplicacao quando falta variavel, e essa severidade e correta para a
 * URL da API e para a chave de servico - sem elas o site nao tem o que mostrar.
 * Aqui nao ha nada a derrubar: as quatro fontes formam uma cadeia que sempre
 * termina em valor valido, e a unica forma de errar - cadastrar uma URL
 * malformada - falha no {@code new URL} abaixo, durante o build.
 */
function resolverRaiz(): string {
  const explicita = process.env.SITE_URL;
  if (explicita !== undefined && explicita.length > 0) return explicita;

  const producao = process.env.VERCEL_PROJECT_PRODUCTION_URL;
  if (producao !== undefined && producao.length > 0) return `https://${producao}`;

  const deploy = process.env.VERCEL_URL;
  if (deploy !== undefined && deploy.length > 0) return `https://${deploy}`;

  return 'http://localhost:3000';
}

export const SITE_URL = new URL(resolverRaiz());
