package dev.crystofer.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * O contexto sobe contra um Postgres 16 de verdade.
 *
 * <p>Ate o commit 17 este teste excluia o DataSource, porque nao havia banco alcancavel no CI. Essa
 * saida acabou aqui: o commit 18 introduz um adaptador de persistencia que e {@code @Component} e
 * depende de repositorio JPA, entao o contexto simplesmente nao carrega sem banco. Continuar
 * excluindo exigiria excluir tambem o adaptador, depois o proximo, ate sobrar um teste que nao
 * testa nada - o padrao de guarda muda da secao 4.1.
 *
 * <p>O que se ganha vai alem de consertar o teste. Com um banco real no CI, cada push passa a
 * verificar automaticamente aquilo que ate agora so tinha verificacao manual: as migracoes Flyway
 * aplicam do zero, e o {@code ddl-auto: validate} confere que as entidades batem com o schema que
 * elas criaram. Era a lacuna registrada no corpo do commit 16.
 *
 * <p>A imagem e fixada em {@code postgres:16-alpine}, a mesma do docker-compose e a mesma de
 * producao. Testar contra outra versao e o erro que o Testcontainers existe para evitar (secao 4.3)
 * - e foi o erro que quase aconteceu de verdade neste projeto, quando um Postgres 18 instalado na
 * maquina atendeu no lugar do container.
 *
 * <p><strong>Requisito novo:</strong> {@code ./mvnw verify} e o hook de pre-push passam a precisar
 * de Docker rodando.
 */
@SpringBootTest
@Testcontainers
class PortfolioApplicationTests {

  /**
   * Estatico de proposito: um container por classe, e nao por metodo. O JUnit inicia antes do
   * contexto do Spring e o encerra no fim, e subir Postgres por teste custaria segundos cada.
   */
  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @Test
  @DisplayName("deve subir o contexto da aplicacao com o schema migrado")
  void shouldLoadApplicationContext() {
    // O proprio carregamento e a assercao. Para chegar aqui foi preciso: o
    // Flyway aplicar V1 e o seed num banco vazio, o Hibernate validar as
    // entidades contra o schema resultante, e todos os beans se resolverem.
  }
}
