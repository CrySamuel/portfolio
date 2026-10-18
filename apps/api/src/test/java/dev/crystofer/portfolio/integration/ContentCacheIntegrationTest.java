package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.http.HttpStatus;

import dev.crystofer.portfolio.shared.config.CacheConfig;
import dev.crystofer.portfolio.shared.domain.ContentLanguage;
import dev.crystofer.portfolio.shared.domain.Slug;
import dev.crystofer.portfolio.support.fixtures.ProjectFixtures;

/**
 * O cache do conteudo do banco (secao 10.5 do plano), pela API inteira.
 *
 * <p>Os testes provam o comportamento que o {@code CacheConfig} promete, inclusive o lado ruim: uma
 * mudanca no banco nao aparece enquanto a entrada viver. Em producao isso nao custa nada, porque o
 * conteudo so muda por migracao, na subida da aplicacao; aqui fica escrito para que ninguem
 * descubra o efeito gravando direto no banco de producao.
 */
class ContentCacheIntegrationTest extends AbstractIntegrationTest {

  private static final String SLUG = "cacheado";

  @Autowired CacheManager cacheManager;

  @Autowired DataSource dataSource;

  @BeforeEach
  void partirDeUmProjetoConhecido() {
    ProjectFixtures.empty(jdbcTemplate);
    ProjectFixtures.insertProject(jdbcTemplate, SLUG, "Titulo original", true, 0, 0, 0);
  }

  @AfterEach
  void devolverOBancoAoSeed() {
    ProjectFixtures.reapplySeed(dataSource);
  }

  @Test
  @DisplayName("deve servir a segunda leitura do cache, sem ver a mudanca no banco")
  void shouldServeSecondRead_fromCache() {
    // given
    var primeira = getComChave("/api/v1/projects/" + SLUG, String.class);
    jdbcTemplate.update("UPDATE project SET title = ? WHERE slug = ?", "Titulo novo", SLUG);

    // when
    var segunda = getComChave("/api/v1/projects/" + SLUG, String.class);
    regiao(CacheConfig.PROJECT).clear();
    var depoisDeEsvaziar = getComChave("/api/v1/projects/" + SLUG, String.class);

    // then
    assertThat(primeira.getBody()).contains("Titulo original");
    assertThat(segunda.getBody()).contains("Titulo original");
    assertThat(depoisDeEsvaziar.getBody()).contains("Titulo novo");
  }

  @Test
  @DisplayName("deve guardar cada idioma na sua propria entrada")
  void shouldKeepLanguagesApart() {
    // when
    getComChaveEmIdioma("/api/v1/projects", "pt-BR", String.class);
    getComChaveEmIdioma("/api/v1/projects", "en-US", String.class);

    // then
    var projetos = regiao(CacheConfig.PROJECTS);
    assertThat(projetos.get(ContentLanguage.PT_BR)).isNotNull();
    assertThat(projetos.get(ContentLanguage.EN_US)).isNotNull();
  }

  /**
   * Slug inexistente nao ocupa memoria.
   *
   * <p>A rota e publica para quem tem a chave, e o slug vem da URL: se a resposta vazia entrasse no
   * cache, cada endereco inventado viraria uma entrada. O teto da regiao seguraria a memoria, mas
   * empurrando para fora os projetos de verdade.
   */
  @Test
  @DisplayName("nao deve guardar a resposta de um projeto que nao existe")
  void shouldNotCache_missingProject() {
    // when
    var resposta = getComChave("/api/v1/projects/nao-existe", String.class);

    // then
    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(
            regiao(CacheConfig.PROJECT)
                .get(new SimpleKey(new Slug("nao-existe"), ContentLanguage.PT_BR)))
        .isNull();
  }

  /**
   * A guarda contra a porta esquecida: um adaptador novo, ou um {@code @Cacheable} apagado, deixa a
   * regiao vazia depois da chamada e reprova aqui - e nao num teste de carga, semanas depois.
   */
  @Test
  @DisplayName("deve passar cada leitura de conteudo pela sua regiao")
  void shouldRouteEveryContentRead_throughItsRegion() {
    // when
    getComChave("/api/v1/profile", String.class);
    getComChave("/api/v1/experiences", String.class);
    getComChave("/api/v1/skills", String.class);
    getComChave("/api/v1/projects", String.class);
    getComChave("/api/v1/projects/" + SLUG, String.class);

    // then
    var idioma = ContentLanguage.padrao();
    assertThat(regiao(CacheConfig.PROFILE).get(idioma)).isNotNull();
    assertThat(regiao(CacheConfig.EXPERIENCES).get(idioma)).isNotNull();
    assertThat(regiao(CacheConfig.SKILLS).get(idioma)).isNotNull();
    assertThat(regiao(CacheConfig.PROJECTS).get(idioma)).isNotNull();
    assertThat(regiao(CacheConfig.PROJECT).get(new SimpleKey(new Slug(SLUG), idioma))).isNotNull();
  }

  private Cache regiao(String nome) {
    return Objects.requireNonNull(cacheManager.getCache(nome), nome);
  }
}
