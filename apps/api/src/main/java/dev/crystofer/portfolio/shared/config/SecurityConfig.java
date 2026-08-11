package dev.crystofer.portfolio.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.crystofer.portfolio.shared.web.ServiceKeyAuthFilter;

/**
 * Onde a chave de servico e exigida - e onde ela deixa de ser exigida.
 *
 * <p>O filtro cobre {@code /api/*} e mais nada. Fora dali ficam, de proposito:
 *
 * <ul>
 *   <li>{@code /actuator/health}, que o Render consulta para saber se o servico subiu. Protege-lo
 *       faria a plataforma concluir que a aplicacao esta morta e reinicia-la em loop.
 *   <li>{@code /v3/api-docs} e {@code /swagger-ui}, publicos porque a Definition of Done do MVP 1
 *       pede Swagger acessivel. Documentacao aberta com dados fechados e a combinacao certa: quem
 *       avalia o repositorio ve o contrato sem precisar de credencial, e ninguem le o conteudo sem
 *       a chave.
 * </ul>
 *
 * <p>O filtro e registrado aqui em vez de anotado com {@code @Component}, e a diferenca importa: um
 * {@code @Component} do tipo {@code Filter} entra automaticamente nas fatias {@code @WebMvcTest},
 * que passariam a exigir cabecalho em testes cujo assunto e o contrato do controlador. Registrado
 * por {@link FilterRegistrationBean}, ele existe onde a aplicacao inteira sobe e em lugar nenhum
 * mais.
 */
@Configuration
public class SecurityConfig {

  /**
   * Chave curta e chave ausente sao rejeitadas no boot, e nao na primeira requisicao.
   *
   * <p>Este e o ponto do arranjo. Sem a validacao, `SERVICE_API_KEY` esquecida no painel do Render
   * produziria uma aplicacao que sobe, responde e **nao protege nada** - a falha silenciosa da
   * secao 4.1, agora valendo o conteudo inteiro da API. Falhando no boot, o deploy fica vermelho e
   * a versao anterior continua no ar.
   *
   * <p>O minimo de 32 caracteres existe porque `openssl rand -base64 32` e o que o `.env.example`
   * manda usar. Sem piso, `SERVICE_API_KEY=teste` passaria pela mesma verificacao e daria a
   * sensacao de estar protegido.
   */
  private static final int TAMANHO_MINIMO_DA_CHAVE = 32;

  private final String chaveDeServico;

  public SecurityConfig(@Value("${portfolio.security.service-key:}") String chaveDeServico) {
    if (chaveDeServico.isBlank()) {
      throw new IllegalStateException(
          "SERVICE_API_KEY nao definida. A API nao sobe sem ela - do contrario ficaria aberta na"
              + " internet sem que nada avisasse. Gere com: openssl rand -base64 32");
    }
    if (chaveDeServico.length() < TAMANHO_MINIMO_DA_CHAVE) {
      throw new IllegalStateException(
          "SERVICE_API_KEY tem "
              + chaveDeServico.length()
              + " caracteres; o minimo e "
              + TAMANHO_MINIMO_DA_CHAVE
              + ". Gere com: openssl rand -base64 32");
    }
    this.chaveDeServico = chaveDeServico;
  }

  @Bean
  FilterRegistrationBean<ServiceKeyAuthFilter> serviceKeyAuthFilter(ObjectMapper objectMapper) {
    var registro =
        new FilterRegistrationBean<>(new ServiceKeyAuthFilter(chaveDeServico, objectMapper));
    registro.addUrlPatterns("/api/*");
    return registro;
  }
}
