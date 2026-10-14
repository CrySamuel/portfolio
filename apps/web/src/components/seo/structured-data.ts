import type { Profile, ProjectDetail, SkillCategory } from '@portfolio/api-client';
import type { BreadcrumbList, Graph, Person, SoftwareSourceCode, Thing, WebSite } from 'schema-dts';

import { SITE_URL } from '@/lib/site';

/**
 * Os dados estruturados do site, como o schema.org os descreve.
 *
 * <p>Tipados pelo {@code schema-dts}, que so traz tipos: um nome de propriedade
 * errado - {@code knowAbout} no lugar de {@code knowsAbout} - reprova o build em
 * vez de virar uma chave que o Google ignora em silencio.
 *
 * <p>Todo endereco sai absoluto. JSON-LD e lido fora da pagina, por quem nao tem
 * contra o que resolver um caminho relativo - a mesma razao do
 * {@code metadataBase} em {@code lib/site}.
 */

function endereco(caminho: string): string {
  return new URL(caminho, SITE_URL).href;
}

/**
 * Identificadores estaveis das duas entidades do site, para que se refiram uma a
 * outra por {@code @id} sem se repetir. O fragmento e convencao: nao precisa
 * existir na pagina, so precisa ser unico e nao mudar.
 */
const ID_DA_PESSOA = endereco('/#pessoa');
const ID_DO_SITE = endereco('/#site');

/** Junta as entidades de uma pagina no formato que o {@code JsonLd} emite. */
export function grafo(...entidades: Thing[]): Graph {
  return { '@context': 'https://schema.org', '@graph': entidades };
}

/**
 * A pessoa por tras do portfolio.
 *
 * <p><strong>{@code sameAs} e o campo que mais importa aqui.</strong> E ele que
 * diz ao Google que o site, o GitHub e o LinkedIn sao a mesma pessoa - e o que
 * alimenta o painel de conhecimento numa busca pelo nome, que e a busca que um
 * recrutador faz depois de receber o curriculo (secao 11.1 do plano). Sai dos
 * links do perfil, menos o de e-mail, que vai no proprio campo.
 *
 * <p>{@code knowsAbout} sao as competencias da secao de skills, na ordem
 * editorial dela. Uma lista escrita a mao aqui divergiria da que aparece na
 * tela no primeiro ajuste.
 */
export function pessoa(perfil: Profile, competencias: readonly SkillCategory[]): Person {
  const email = perfil.socialLinks.find((link) => link.platform === 'email');

  return {
    '@type': 'Person',
    '@id': ID_DA_PESSOA,
    name: perfil.fullName,
    jobTitle: perfil.headline,
    description: perfil.bio,
    url: endereco('/'),
    ...(email === undefined ? {} : { email: email.url.replace(/^mailto:/, '') }),
    sameAs: perfil.socialLinks.filter((link) => link.platform !== 'email').map((link) => link.url),
    knowsAbout: competencias.flatMap((categoria) => categoria.skills.map((skill) => skill.name)),
  };
}

/**
 * O site como entidade, com a pessoa como autora.
 *
 * <p><strong>Sem {@code potentialAction} de busca</strong>, que a secao 11.3 do
 * plano previa para "habilitar a caixa de busca do site". O site nao tem busca
 * para onde a acao apontar, e o Google aposentou a caixa de busca nos
 * resultados em novembro de 2024 - declarar seria marcacao para um recurso que
 * nao existe dos dois lados.
 */
export function site(perfil: Profile): WebSite {
  return {
    '@type': 'WebSite',
    '@id': ID_DO_SITE,
    name: perfil.fullName,
    url: endereco('/'),
    inLanguage: 'pt-BR',
    author: { '@id': ID_DA_PESSOA },
  };
}

/** Um degrau da trilha de navegacao: o nome que aparece e o caminho do site. */
export interface Degrau {
  readonly nome: string;
  readonly caminho: string;
}

/**
 * A trilha de navegacao, que o Google mostra no lugar do endereco cru no
 * resultado de busca - "Inicio > Projetos > FinAI" em vez de uma URL.
 */
export function trilha(degraus: readonly Degrau[]): BreadcrumbList {
  return {
    '@type': 'BreadcrumbList',
    itemListElement: degraus.map((degrau, indice) => ({
      '@type': 'ListItem',
      position: indice + 1,
      name: degrau.nome,
      item: endereco(degrau.caminho),
    })),
  };
}

/**
 * Um projeto como codigo-fonte.
 *
 * <p>{@code programmingLanguage} sao as tecnologias que a API classifica como
 * linguagem - o resto da stack (framework, banco, infraestrutura) nao e
 * linguagem, e chamar Spring Boot de linguagem de programacao seria o tipo de
 * imprecisao que um avaliador tecnico nota. {@code codeRepository} e a data so
 * entram quando existem: os dois sao nulaveis no contrato.
 *
 * <p>O autor vai por extenso, e nao por {@code @id}: a pessoa so e declarada no
 * grafo da home, e uma referencia que aponta para fora da pagina nao e resolvida
 * por quem le esta.
 */
export function codigoFonte(projeto: ProjectDetail, perfil: Profile): SoftwareSourceCode {
  return {
    '@type': 'SoftwareSourceCode',
    name: projeto.title,
    description: projeto.summary,
    url: endereco(`/projetos/${projeto.slug}`),
    ...(projeto.repoUrl === null ? {} : { codeRepository: projeto.repoUrl }),
    programmingLanguage: projeto.technologies
      .filter((tecnologia) => tecnologia.category === 'language')
      .map((tecnologia) => tecnologia.name),
    ...(projeto.publishedAt === null ? {} : { datePublished: projeto.publishedAt }),
    author: { '@type': 'Person', name: perfil.fullName, url: endereco('/') },
  };
}
