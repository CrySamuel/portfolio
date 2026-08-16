package dev.crystofer.portfolio.github.adapter.in.web.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import dev.crystofer.portfolio.github.adapter.in.web.dto.GitHubStatsResponse;
import dev.crystofer.portfolio.github.adapter.in.web.dto.LanguageShareResponse;
import dev.crystofer.portfolio.github.adapter.in.web.dto.RepositoryResponse;
import dev.crystofer.portfolio.github.domain.model.GitHubStats;
import dev.crystofer.portfolio.github.domain.model.RepositorySummary;

/**
 * Converte o retrato do dominio em resposta HTTP. Um sentido so.
 *
 * <p>O repositorio e mapeado campo a campo pelo MapStruct, como nos outros modulos - com {@code
 * unmappedTargetPolicy=ERROR}, campo novo no DTO sem origem no dominio reprova a compilacao.
 *
 * <p><strong>As linguagens e o corte precisam de conta, e por isso sao escritos.</strong> A fatia
 * de cada linguagem depende do total das outras, e o total so existe no agregado; o corte da
 * vitrine depende de um numero de configuracao. Nenhuma das duas coisas cabe num mapeamento
 * declarativo, e simular isso com expressoes deixaria a conta escondida dentro de uma anotacao.
 */
@Mapper
public interface GitHubWebMapper {

  RepositoryResponse toResponse(RepositorySummary repository);

  /**
   * O retrato inteiro, com o corte da vitrine aplicado.
   *
   * <p>O total dos pesos e calculado <strong>uma vez</strong> e reaproveitado por todas as fatias.
   * Chamar {@code totalLanguageWeight()} dentro do laco recomeçaria a soma a cada linguagem - com
   * quinze delas, quinze varreduras da mesma lista para chegar sempre ao mesmo numero.
   *
   * @param limite quantos repositorios publicar; vem da configuracao, porque quantos cards cabem e
   *     decisao de tela e nao de dominio
   */
  default GitHubStatsResponse toResponse(GitHubStats stats, int limite) {
    long total = stats.totalLanguageWeight();

    List<LanguageShareResponse> languages =
        stats.languages().stream()
            .map(uso -> new LanguageShareResponse(uso.name(), uso.shareOf(total)))
            .toList();

    return new GitHubStatsResponse(
        stats.username(),
        stats.publicRepositories(),
        stats.contributionsLastYear(),
        languages,
        stats.highlights(limite).stream().map(this::toResponse).toList());
  }
}
