package dev.crystofer.portfolio.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base dos testes que sobem a aplicacao inteira contra um Postgres de verdade.
 *
 * <p>A aplicacao roda em porta aleatoria, com Tomcat de verdade, e as chamadas saem pela rede local
 * - nao por {@code MockMvc}. A diferenca nao e purismo: {@code MockMvc} simula o container servlet,
 * entao negociacao de conteudo, codificacao dos bytes na resposta e compressao ficam de fora do que
 * o teste consegue observar. Este e o unico nivel da piramide (secao 13.6) onde essas coisas sao
 * verificaveis.
 *
 * <p><strong>Um container para a suite inteira.</strong> O campo e estatico e mora aqui, na base:
 * todas as subclasses compartilham a mesma instancia, e o Postgres sobe uma vez por JVM. Fosse um
 * container por classe, cada teste de integracao novo acrescentaria dezenas de segundos ao CI - o
 * tipo de custo que faz alguem propor "usa H2 nos testes" seis meses depois.
 *
 * <p>Pelo mesmo motivo as anotacoes de contexto ficam nesta classe e nao nas subclasses: a chave de
 * cache de contexto do Spring passa a ser identica entre elas, entao o contexto tambem e montado
 * uma vez so.
 *
 * <p>A imagem e {@code postgres:16-alpine}, a mesma do docker-compose e a de producao (secao 4.3 do
 * plano). Fixar a versao aqui, num lugar so, e o que impede o teste de rodar contra outro Postgres
 * que nao o combinado - erro que ja aconteceu neste projeto, quando um Postgres 18 instalado na
 * maquina atendeu no lugar do container.
 *
 * <p>O {@code @ServiceConnection} aponta o datasource da aplicacao para o container sozinho. Sem
 * ele, seria preciso um {@code @DynamicPropertySource} repetindo url, usuario e senha - tres
 * chances de a aplicacao conectar num banco diferente daquele que o teste prepara.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  /** Cliente HTTP ja apontado para a porta sorteada. */
  @Autowired protected TestRestTemplate restTemplate;

  /**
   * Acesso direto ao banco, para preparar e conferir estado.
   *
   * <p>SQL cru de proposito, e nao os repositorios da aplicacao. Montar o cenario com o mesmo
   * codigo que esta sob teste torna o teste circular: um mapeamento errado gravaria errado, leria
   * errado e passaria.
   */
  @Autowired protected JdbcTemplate jdbcTemplate;
}
