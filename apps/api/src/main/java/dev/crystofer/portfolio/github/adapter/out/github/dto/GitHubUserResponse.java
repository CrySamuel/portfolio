package dev.crystofer.portfolio.github.adapter.out.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * O que interessa de {@code GET /users/{username}}.
 *
 * <p>A resposta real tem mais de trinta campos - avatar, empresa, plano, contadores de seguidores,
 * uma duzia de URLs de API. Declarar so os dois usados nao e preguica: cada campo trazido para ca
 * vira um campo a manter quando o GitHub mudar o formato dele, e nenhum deles apareceria na tela.
 *
 * <p>{@code ignoreUnknown} explicito, e nao herdado da configuracao. O Jackson 3 e o Spring Boot ja
 * sao tolerantes por padrao, mas essa e uma configuracao global que alguem pode endurecer um dia
 * por um motivo legitimo - e ai a integracao quebraria por causa de um campo novo que o GitHub
 * acrescentou e que nao usamos.
 *
 * @param login o nome de usuario como o GitHub o escreve, com a capitalizacao dele
 * @param publicRepos quantos repositorios publicos existem no total
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubUserResponse(String login, @JsonProperty("public_repos") int publicRepos) {}
