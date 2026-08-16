package dev.crystofer.portfolio.github.adapter.out.github.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.crystofer.portfolio.github.adapter.out.github.dto.GitHubRepositoryResponse;
import dev.crystofer.portfolio.github.domain.model.LanguageUsage;
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

  @Test
  @DisplayName("deve transformar o mapa de bytes em linguagens")
  void shouldMap_languages() {
    Map<String, Long> bytes = new LinkedHashMap<>();
    bytes.put("Java", 90_000L);
    bytes.put("TypeScript", 10_000L);

    assertThat(mapper.toLanguages(bytes))
        .extracting(LanguageUsage::name, LanguageUsage::bytes)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("Java", 90_000L),
            org.assertj.core.groups.Tuple.tuple("TypeScript", 10_000L));
  }

  /**
   * Zero e descartado, e nao recusado.
   *
   * <p>O dominio exige bytes positivos - zero nao e pouco uso, e ausencia. Deixar a excecao subir
   * faria uma linguagem irrelevante derrubar o retrato inteiro do perfil.
   */
  @Test
  @DisplayName("deve descartar linguagem com zero bytes em vez de derrubar o retrato")
  void shouldDiscard_zeroBytes() {
    Map<String, Long> bytes = new LinkedHashMap<>();
    bytes.put("Java", 100L);
    bytes.put("Batchfile", 0L);

    assertThat(mapper.toLanguages(bytes)).extracting(LanguageUsage::name).containsExactly("Java");
  }

  @Test
  @DisplayName("deve descartar valor nulo vindo da resposta")
  void shouldDiscard_nullBytes() {
    Map<String, Long> bytes = new LinkedHashMap<>();
    bytes.put("Java", 100L);
    bytes.put("Nix", null);

    assertThat(mapper.toLanguages(bytes)).hasSize(1);
  }

  @Test
  @DisplayName("deve devolver lista vazia quando nao ha linguagem")
  void shouldReturnEmpty_whenThereIsNoLanguage() {
    assertThat(mapper.toLanguages(Map.of())).isEmpty();
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
