package dev.crystofer.portfolio.github.adapter.out.github.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.crystofer.portfolio.github.adapter.out.github.dto.GitHubRepositoryResponse;
import dev.crystofer.portfolio.github.domain.model.RepositorySummary;

class GitHubResponseMapperTest {

  private final GitHubResponseMapper mapper = new GitHubResponseMapper();

  @Test
  @DisplayName("deve mapear os campos que o dominio usa")
  void shouldMap_domainFields() {
    RepositorySummary resumo =
        mapper.toSummary(repositorio(OffsetDateTime.parse("2026-08-15T12:00:00Z")));

    assertThat(resumo.name()).isEqualTo("portfolio");
    assertThat(resumo.description()).isEqualTo("Monorepo full-stack");
    assertThat(resumo.url()).isEqualTo("https://github.com/CrySamuel/portfolio");
    assertThat(resumo.primaryLanguage()).isEqualTo("Java");
    assertThat(resumo.stars()).isEqualTo(3);
  }

  /**
   * O fuso e explicito, e a razao ja custou um mes uma vez.
   *
   * <p>Um push as 21h de Sao Paulo e 00h do dia seguinte em UTC. Convertendo com o fuso da maquina,
   * a data mudaria conforme onde a aplicacao roda - e o desempate por push passaria a depender do
   * servidor. Este teste fixa o instante numa borda de dia para que a diferenca apareca.
   */
  @Test
  @DisplayName("deve converter o push para data em UTC, e nao no fuso da maquina")
  void shouldConvertPushDate_inUtc() {
    OffsetDateTime pushEmSaoPaulo = OffsetDateTime.parse("2026-08-15T21:30:00-03:00");

    RepositorySummary resumo = mapper.toSummary(repositorio(pushEmSaoPaulo));

    assertThat(pushEmSaoPaulo.atZoneSameInstant(ZoneOffset.UTC).toLocalDate())
        .isEqualTo(LocalDate.of(2026, 8, 16));
    assertThat(resumo.lastPushedAt()).isEqualTo(LocalDate.of(2026, 8, 16));
  }

  private static GitHubRepositoryResponse repositorio(OffsetDateTime pushedAt) {
    return new GitHubRepositoryResponse(
        "portfolio",
        "Monorepo full-stack",
        "https://github.com/CrySamuel/portfolio",
        "Java",
        3,
        pushedAt,
        false,
        false);
  }
}
