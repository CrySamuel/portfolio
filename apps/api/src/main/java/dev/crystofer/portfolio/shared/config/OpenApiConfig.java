package dev.crystofer.portfolio.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * Metadados do documento OpenAPI.
 *
 * <p>Os caminhos e schemas o springdoc deduz dos proprios controllers; o que ele nao tem como
 * deduzir e o cabecalho - titulo, versao, licenca, contato. Fica aqui, e nao num {@code
 * openapi.yaml} escrito a mao, porque arquivo paralelo envelhece em silencio enquanto o codigo
 * segue mudando.
 *
 * <p>A versao vem do {@code pom.xml} pela filtragem de recurso do Maven, e nao de uma constante
 * repetida. Numero de versao duplicado diverge no primeiro release em que alguem esquece de um dos
 * dois.
 */
@Configuration
public class OpenApiConfig {

  private final String version;

  public OpenApiConfig(@Value("${info.app.version:0.0.0}") String version) {
    this.version = version;
  }

  @Bean
  OpenAPI portfolioOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Portfolio API")
                .version(version)
                .description(
                    """
                    API que serve o conteudo do portfolio de Crystofer Demetino.

                    O site nao chama estes endpoints do navegador: quem chama e o BFF do \
                    Next.js, em build time e na revalidacao do ISR (ADR-0005). Por isso as \
                    respostas trazem Cache-Control generoso - o visitante recebe HTML da CDN \
                    e nunca espera pela API.""")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
                .contact(new Contact().name("Crystofer Demetino")));
  }
}
