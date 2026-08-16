package dev.crystofer.portfolio.github.adapter.out.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A resposta do GraphQL com o calendario de contribuicoes.
 *
 * <p>O aninhamento e feio e e do GitHub, nao nosso: o total de contribuicoes do ultimo ano vive
 * quatro niveis abaixo de {@code data}. Ele <strong>so existe no GraphQL</strong> - nenhum endpoint
 * REST publica esse numero -, e o GraphQL do GitHub exige autenticacao para qualquer consulta.
 *
 * <p>Cada nivel pode vir nulo quando a consulta falha parcialmente, e o GraphQL responde 200 mesmo
 * assim - por isso a leitura desce pelo caminho com {@link #totalContributions()} em vez de
 * encadear chamadas que estourariam em {@code NullPointerException}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubContributionsResponse(Data data) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Data(User user) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record User(ContributionsCollection contributionsCollection) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ContributionsCollection(ContributionCalendar contributionCalendar) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ContributionCalendar(int totalContributions) {}

  /**
   * O total, ou zero quando qualquer nivel do caminho veio nulo.
   *
   * <p>Zero e a resposta honesta aqui: o dominio trata ausencia de contribuicao e ausencia do
   * numero do mesmo jeito - a secao nao mostra a linha.
   */
  public int totalContributions() {
    if (data == null || data.user() == null) {
      return 0;
    }
    var collection = data.user().contributionsCollection();
    if (collection == null || collection.contributionCalendar() == null) {
      return 0;
    }
    return collection.contributionCalendar().totalContributions();
  }
}
