import { ApiError, type ProjectDetail } from '@portfolio/api-client';
import { notFound } from 'next/navigation';

import { routing, type Idioma } from '@/i18n/routing';
import { getProject, listProjects } from '@/lib/api/projects';

/**
 * O que os dois segmentos dinamicos entregam - o idioma e o projeto -, como o
 * Next 15 os entrega: em promessa.
 */
export interface Parametros {
  readonly params: Promise<{ readonly locale: string; readonly slug: string }>;
}

/**
 * Os slugs que o build pre-renderiza.
 *
 * <p><strong>E o {@code generateStaticParams} que decide se a rota e
 * estatica.</strong> Sem ele o segmento dinamico so seria resolvido por
 * requisicao, e cada visitante voltaria a esperar pelo cold start de ~1 min do
 * plano gratuito do Render - exatamente o que o ISR existe para evitar.
 *
 * <p><strong>A pagina e a imagem precisam declara-lo cada uma.</strong> Medido:
 * com o {@code generateStaticParams} so na {@code page.tsx}, o relatorio do
 * build marcou as duas paginas de projeto como {@code ●} e o
 * {@code opengraph-image} como {@code ƒ} - servido por requisicao. Nao se herda
 * do arquivo vizinho, e a diferenca importa: crawler de rede social desiste
 * rapido, e imagem gerada na hora, atras de um servico que hiberna, e como um
 * link compartilhado aparece sem preview.
 *
 * <p>A lista sai de {@code listProjects()}, que e a mesma chamada da listagem e
 * da home: uma segunda fonte de slugs poderia discordar da primeira, e o modo de
 * discordar seria um card apontando para uma pagina que o build nao gerou.
 *
 * <p>Lida no idioma padrao, e vale para todos: o slug e o mesmo em cada idioma (ADR-0013).
 */
export async function slugsDoCatalogo(): Promise<{ slug: string }[]> {
  const projetos = await listProjects(routing.defaultLocale);
  return projetos.map((projeto) => ({ slug: projeto.slug }));
}

/**
 * Os pares de idioma e slug que a imagem de compartilhamento pre-renderiza.
 *
 * <p>A pagina recebe o idioma do layout e so declara os slugs; a imagem nao
 * recebe nada, porque rota de imagem nao passa pelo layout - a mesma heranca
 * que faltou aos slugs (ver acima) falta tambem ao idioma. Medido: com so os
 * slugs, o build listou a imagem como {@code ●} e nao gravou nenhum PNG.
 */
export async function idiomasESlugsDoCatalogo(): Promise<{ locale: string; slug: string }[]> {
  const slugs = await slugsDoCatalogo();
  return routing.locales.flatMap((locale) => slugs.map(({ slug }) => ({ locale, slug })));
}

/**
 * O projeto do endereco, ou a pagina de 404.
 *
 * <p>E aqui que a resposta da API vira decisao de roteamento, e o lugar e este
 * por eliminacao: {@code lib/api} descreve o contrato da API e nao deveria
 * conhecer {@code notFound()}, enquanto a pagina e a imagem de compartilhamento
 * precisam da mesma traducao - duas copias divergiriam no primeiro ajuste.
 *
 * <p><strong>Dois status viram a mesma pagina, e eles significam coisas
 * diferentes.</strong> A API responde <strong>400</strong> para slug fora do
 * formato da URL e <strong>404</strong> para slug bem formado que nao existe. A
 * distincao e correta no protocolo e nao teria uso na tela: quem digitou
 * {@code /projetos/Slug%20Invalido} e quem digitou {@code /projetos/nao-existe}
 * chegaram ao mesmo lugar - um endereco sem projeto -, e uma tela de "requisicao
 * malformada" so explicaria o problema do servidor a quem tem o do visitante.
 *
 * <p><strong>Os demais erros sobem.</strong> API fora do ar e falha, nao
 * ausencia: transformar 500 em "projeto nao encontrado" apagaria o incidente e
 * publicaria uma pagina de 404 no lugar de um erro que precisa aparecer.
 */
export async function carregarProjeto(slug: string, idioma: Idioma): Promise<ProjectDetail> {
  try {
    return await getProject(slug, idioma);
  } catch (erro) {
    if (erro instanceof ApiError && (erro.status === 404 || erro.status === 400)) notFound();
    throw erro;
  }
}
