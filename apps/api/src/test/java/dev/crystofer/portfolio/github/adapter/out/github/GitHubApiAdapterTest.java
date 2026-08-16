package dev.crystofer.portfolio.github.adapter.out.github;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.crystofer.portfolio.github.adapter.out.github.dto.GitHubRepositoryResponse;

/**
 * As regras de quais repositorios entram na vitrine.
 *
 * <p>So isso, e sem Spring: os cenarios de HTTP - 403 de cota, 500, timeout e resposta malformada,
 * mais a prova de que o disjuntor abre e fecha - sao do commit 43, com WireMock.
 */
class GitHubApiAdapterTest {

  private static final String USUARIO = "CrySamuel";

  @Test
  @DisplayName("deve aceitar repositorio proprio, vivo e que nao e o do perfil")
  void shouldAccept_ordinaryRepository() {
    assertThat(GitHubApiAdapter.contaComoProjeto(repositorio("portfolio", false, false), USUARIO))
        .isTrue();
  }

  /**
   * Fork nao e codigo escrito pela pessoa, e somaria a distribuicao de linguagens de outra gente.
   */
  @Test
  @DisplayName("deve recusar fork")
  void shouldReject_fork() {
    assertThat(GitHubApiAdapter.contaComoProjeto(repositorio("spring-boot", true, false), USUARIO))
        .isFalse();
  }

  /** Arquivado e dela, mas nao representa o que ela mantem hoje. */
  @Test
  @DisplayName("deve recusar arquivado")
  void shouldReject_archived() {
    assertThat(
            GitHubApiAdapter.contaComoProjeto(repositorio("estudo-antigo", false, true), USUARIO))
        .isFalse();
  }

  /**
   * O repositorio de perfil nao e projeto - e o README do topo do perfil.
   *
   * <p>Medido antes de existir esta regra: ele chegava em <strong>primeiro</strong> na ordem de
   * destaque, porque tem uma estrela e o criterio comeca por estrelas.
   */
  @Test
  @DisplayName("deve recusar o repositorio de perfil, que tem o nome do usuario")
  void shouldReject_profileRepository() {
    assertThat(GitHubApiAdapter.contaComoProjeto(repositorio("CrySamuel", false, false), USUARIO))
        .isFalse();
  }

  /**
   * A comparacao ignora a caixa porque as duas pontas sao escritas por gente diferente: o nome do
   * repositorio vem do GitHub e o nome de usuario vem da configuracao. {@code GITHUB_USERNAME}
   * digitada em minusculas nao pode fazer o repositorio de perfil voltar para a vitrine.
   */
  @Test
  @DisplayName("deve recusar o repositorio de perfil mesmo com caixa diferente")
  void shouldReject_profileRepository_regardlessOfCase() {
    assertThat(GitHubApiAdapter.contaComoProjeto(repositorio("crysamuel", false, false), USUARIO))
        .isFalse();
    assertThat(
            GitHubApiAdapter.contaComoProjeto(repositorio("CrySamuel", false, false), "crysamuel"))
        .isFalse();
  }

  private static GitHubRepositoryResponse repositorio(String name, boolean fork, boolean archived) {
    return new GitHubRepositoryResponse(
        name,
        null,
        "https://github.com/CrySamuel/" + name,
        "Java",
        1,
        OffsetDateTime.parse("2026-08-15T12:00:00Z"),
        fork,
        archived);
  }
}
