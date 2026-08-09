package dev.crystofer.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PortfolioApplicationTests {

  @Test
  @DisplayName("deve subir o contexto da aplicacao")
  void shouldLoadApplicationContext() {
    // O proprio carregamento do contexto e a assercao: se algum bean estiver mal
    // configurado, o teste falha antes de chegar aqui.
  }
}
