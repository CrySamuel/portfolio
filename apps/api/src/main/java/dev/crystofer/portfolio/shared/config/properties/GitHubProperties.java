package dev.crystofer.portfolio.shared.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Tudo o que a integracao com o GitHub le de configuracao.
 *
 * <p>Record com vinculo por construtor, entao os valores sao imutaveis e as regras abaixo rodam
 * <strong>no boot</strong>. E a mesma escolha que {@code SecurityConfig} faz com a chave de
 * servico, e pela mesma razao: configuracao errada precisa aparecer no deploy, com o nome da
 * variavel, e nao numa requisicao qualquer horas depois.
 *
 * <p><strong>O token e opcional, e a diferenca que ele faz e de cota.</strong> Sem ele sao 60
 * requisicoes por hora por IP, e a instancia do Render compartilha IP de saida; com ele, 5.000. Com
 * o cache de 6h o consumo real fica em torno de quatro chamadas por dia, entao o token e margem,
 * nao necessidade diaria - e e o que permite desenvolver sem segredo nenhum na maquina.
 *
 * <p><strong>A excecao e o GraphQL.</strong> A API de contribuicoes so existe la, e o GraphQL do
 * GitHub <em>exige</em> autenticacao: sem token ele responde 401 para qualquer consulta. Por isso
 * as contribuicoes sao o unico numero que some quando nao ha token, em vez de degradar.
 *
 * @param username perfil publico a consultar
 * @param token credencial de leitura publica; vazio significa chamada anonima
 * @param baseUrl raiz da API REST, parametrizada para o WireMock do commit 43
 * @param graphqlUrl endereco do GraphQL, pelo mesmo motivo
 * @param connectTimeout espera pela conexao (secao 3.10)
 * @param readTimeout espera pela resposta (secao 3.10)
 * @param repositoriesToLoad quantos repositorios trazer do perfil
 * @param repositoriesForLanguages de quantos deles somar as linguagens, byte a byte
 */
@ConfigurationProperties(prefix = "portfolio.github")
public record GitHubProperties(
    String username,
    @DefaultValue("") String token,
    @DefaultValue("https://api.github.com") String baseUrl,
    @DefaultValue("https://api.github.com/graphql") String graphqlUrl,
    @DefaultValue("2s") Duration connectTimeout,
    @DefaultValue("3s") Duration readTimeout,
    @DefaultValue("30") int repositoriesToLoad,
    @DefaultValue("20") int repositoriesForLanguages) {

  /** O teto do proprio GitHub por pagina; acima disso seria preciso paginar. */
  private static final int MAX_PER_PAGE = 100;

  public GitHubProperties {
    if (username == null || username.isBlank()) {
      throw new IllegalStateException(
          "GITHUB_USERNAME nao definida. A secao de estatisticas nao tem de quem falar.");
    }
    username = username.trim();
    token = token == null ? "" : token.trim();

    // O limite existe porque cada repositorio custa **uma requisicao a mais**
    // para somar as linguagens dele. Com 30 repositorios e um limite de 20, um
    // reaquecimento gasta 22 chamadas - dentro das 60 por hora que existem sem
    // token, com folga para o keep-alive e para uma segunda tentativa.
    requireRange(repositoriesToLoad, "portfolio.github.repositories-to-load", 1, MAX_PER_PAGE);
    requireRange(
        repositoriesForLanguages,
        "portfolio.github.repositories-for-languages",
        0,
        repositoriesToLoad);
  }

  /** Ha token cadastrado? E o que decide se as contribuicoes sao consultadas. */
  public boolean hasToken() {
    return !token.isBlank();
  }

  private static void requireRange(int value, String property, int min, int max) {
    if (value < min || value > max) {
      throw new IllegalStateException(
          property + " precisa ficar entre " + min + " e " + max + "; veio " + value);
    }
  }
}
