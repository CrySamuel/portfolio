package dev.crystofer.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.crystofer.portfolio.integration.AbstractIntegrationTest;

/**
 * O contexto sobe contra um Postgres 16 de verdade.
 *
 * <p>Ate o commit 17 este teste excluia o DataSource, porque nao havia banco alcancavel no CI. Essa
 * saida acabou no commit 18: o adaptador de persistencia e {@code @Component} e depende de
 * repositorio JPA, entao o contexto simplesmente nao carrega sem banco. Continuar excluindo
 * exigiria excluir tambem o adaptador, depois o proximo, ate sobrar um teste que nao testa nada - o
 * padrao de guarda muda da secao 4.1.
 *
 * <p>O que se ganha vai alem de consertar o teste. Com um banco real no CI, cada push passa a
 * verificar automaticamente aquilo que ate entao so tinha verificacao manual: as migracoes Flyway
 * aplicam do zero, e o {@code ddl-auto: validate} confere que as entidades batem com o schema que
 * elas criaram. Era a lacuna registrada no corpo do commit 16.
 *
 * <p>A declaracao do container saiu daqui no commit 21 e vive em {@link AbstractIntegrationTest}.
 * Enquanto havia um teste so, ter o campo nesta classe era o mais simples; com dois, seriam dois
 * Postgres subindo por execucao para provar a mesma coisa. Herdar tambem alinha a configuracao de
 * contexto entre as classes, e o Spring monta o contexto uma vez para as duas.
 *
 * <p><strong>Requisito:</strong> {@code ./mvnw verify} e o hook de pre-push precisam de Docker
 * rodando.
 */
class PortfolioApplicationTests extends AbstractIntegrationTest {

  @Test
  @DisplayName("deve subir o contexto da aplicacao com o schema migrado")
  void shouldLoadApplicationContext() {
    // O proprio carregamento e a assercao. Para chegar aqui foi preciso: o
    // Flyway aplicar V1 e o seed num banco vazio, o Hibernate validar as
    // entidades contra o schema resultante, e todos os beans se resolverem.
  }
}
