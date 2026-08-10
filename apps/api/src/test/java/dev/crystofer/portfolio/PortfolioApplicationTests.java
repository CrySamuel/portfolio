package dev.crystofer.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * A exclusao do DataSource e temporaria e tem prazo: sai no commit 21, quando o Testcontainers
 * subir um Postgres 16 de verdade para os testes.
 *
 * <p>O motivo: a partir do commit 16 o classpath tem driver, JDBC e Flyway, e o Spring passa a
 * tentar abrir conexao ao carregar o contexto. Sem banco alcancavel, este teste falharia no CI - e
 * falharia por ausencia de infraestrutura, nao por defeito no codigo, que e o tipo de vermelho que
 * ensina a equipe a ignorar o CI.
 *
 * <p>O que se perde ate la esta dito de forma direta: este teste deixou de cobrir a camada de
 * dados. A migracao V1 e o seed foram verificados contra um Postgres real localmente, e essa
 * verificacao esta no corpo do commit 16 - mas verificacao manual nao e guarda automatizada, e nao
 * ha guarda automatizada sobre o schema entre os commits 16 e 21.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude="
            + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
class PortfolioApplicationTests {

  @Test
  @DisplayName("deve subir o contexto da aplicacao")
  void shouldLoadApplicationContext() {
    // O proprio carregamento do contexto e a assercao: se algum bean estiver mal
    // configurado, o teste falha antes de chegar aqui.
  }
}
