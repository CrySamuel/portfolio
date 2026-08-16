package dev.crystofer.portfolio.github.adapter.out.github.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Um repositorio como {@code GET /users/{username}/repos} o devolve.
 *
 * <p>{@code fork} e {@code archived} entram sem ir para o dominio, e e a unica razao pela qual eles
 * existem aqui: sao filtros. Fork nao e codigo escrito pela pessoa, e a distribuicao de linguagens
 * do perfil ficaria contando o repositorio de outra gente; e um projeto arquivado continua sendo
 * dela, mas nao deveria disputar a primeira posicao com o que ela mantem hoje.
 *
 * @param name nome do repositorio
 * @param description descricao curta; nulo quando nao ha
 * @param htmlUrl o endereco que uma pessoa abre - e nao a URL de API
 * @param language linguagem predominante segundo o GitHub; nulo quando nao ha
 * @param stargazersCount estrelas recebidas
 * @param pushedAt instante do ultimo push, em UTC
 * @param fork se e copia de outro repositorio
 * @param archived se foi arquivado pelo dono
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepositoryResponse(
    String name,
    String description,
    @JsonProperty("html_url") String htmlUrl,
    String language,
    @JsonProperty("stargazers_count") int stargazersCount,
    @JsonProperty("pushed_at") OffsetDateTime pushedAt,
    boolean fork,
    boolean archived) {}
