package dev.crystofer.portfolio.projects.adapter.out.persistence.mapper;

import java.util.Locale;

import org.mapstruct.Mapper;

import dev.crystofer.portfolio.projects.adapter.out.persistence.entity.ProjectEntity;
import dev.crystofer.portfolio.projects.adapter.out.persistence.entity.ProjectMetricEntity;
import dev.crystofer.portfolio.projects.adapter.out.persistence.entity.TechnologyEntity;
import dev.crystofer.portfolio.projects.domain.model.Project;
import dev.crystofer.portfolio.projects.domain.model.ProjectMetric;
import dev.crystofer.portfolio.projects.domain.model.Technology;
import dev.crystofer.portfolio.projects.domain.model.TechnologyCategory;
import dev.crystofer.portfolio.shared.domain.Slug;

/**
 * Converte entidade em modelo de dominio. Um sentido so.
 *
 * <p>Com {@code unmappedTargetPolicy=ERROR}, componente novo no record sem origem na entidade
 * reprova a compilacao.
 */
@Mapper
public interface ProjectPersistenceMapper {

  Project toDomain(ProjectEntity entity);

  Technology toDomain(TechnologyEntity entity);

  ProjectMetric toDomain(ProjectMetricEntity entity);

  /**
   * Envolve o texto da coluna no value object.
   *
   * <p>E aqui que a validacao de formato acontece do lado de dentro: um slug malformado que tenha
   * entrado no banco por um caminho que nao passou pelo {@code CHECK} morre na leitura, com a
   * mensagem dizendo qual valor era, em vez de virar URL quebrada na tela.
   */
  default Slug toSlug(String value) {
    return value == null ? null : Slug.of(value);
  }

  /**
   * Traduz o codigo da coluna para a constante do dominio.
   *
   * <p>Falha alto de proposito, como os mappers de plataforma e de nivel ja fazem. Uma categoria
   * desconhecida no banco significa migracao que entrou sem a constante correspondente: devolver
   * null ou pular a linha esconderia o problema, e a tela mostraria um chip a menos sem nada em
   * lugar nenhum dizendo por que.
   *
   * <p>O {@code CHECK} da V4 ja recusa valor fora da lista, entao este caminho so dispara se alguem
   * acrescentar valor a coluna sem acrescentar ao enum - que e exatamente quando se quer o erro.
   *
   * <p>{@code Locale.ROOT} pelo mesmo motivo dos outros mappers: em turco, {@code
   * "i".toUpperCase()} nao produz {@code "I"}.
   */
  default TechnologyCategory toCategory(String category) {
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException("Categoria nao pode ser vazia em technology.category");
    }
    try {
      return TechnologyCategory.valueOf(category.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException cause) {
      throw new IllegalArgumentException(
          "Categoria desconhecida em technology.category: " + category, cause);
    }
  }
}
