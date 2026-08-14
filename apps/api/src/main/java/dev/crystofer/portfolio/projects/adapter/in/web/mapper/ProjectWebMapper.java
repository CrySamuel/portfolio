package dev.crystofer.portfolio.projects.adapter.in.web.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import dev.crystofer.portfolio.projects.adapter.in.web.dto.ProjectDetailResponse;
import dev.crystofer.portfolio.projects.adapter.in.web.dto.ProjectMetricResponse;
import dev.crystofer.portfolio.projects.adapter.in.web.dto.ProjectSummaryResponse;
import dev.crystofer.portfolio.projects.adapter.in.web.dto.TechnologyResponse;
import dev.crystofer.portfolio.projects.domain.model.Project;
import dev.crystofer.portfolio.projects.domain.model.ProjectMetric;
import dev.crystofer.portfolio.projects.domain.model.Technology;
import dev.crystofer.portfolio.projects.domain.model.TechnologyCategory;
import dev.crystofer.portfolio.shared.domain.Slug;

/**
 * Converte modelo de dominio em DTO de resposta. Um sentido so.
 *
 * <p>Com {@code unmappedTargetPolicy=ERROR}, campo novo no DTO sem origem no dominio reprova a
 * compilacao. O caminho inverso - campo do dominio que ninguem publica - <strong>nao</strong> e
 * verificado por aqui, e e proposital: {@code ProjectSummaryResponse} omite a narrativa e os
 * enderecos de propriedade, e o compilador nao tem como distinguir omissao deliberada de
 * esquecimento. Quem guarda isso e o {@code OpenApiContractTest}.
 */
@Mapper
public interface ProjectWebMapper {

  ProjectSummaryResponse toSummary(Project project);

  List<ProjectSummaryResponse> toSummary(List<Project> projects);

  ProjectDetailResponse toDetail(Project project);

  TechnologyResponse toResponse(Technology technology);

  ProjectMetricResponse toResponse(ProjectMetric metric);

  /**
   * O value object vira o texto que o JSON publica.
   *
   * <p>Publicar {@code {"value": "finai"}} obrigaria todo cliente a desembrulhar um nivel para
   * chegar ao texto que ele receberia direto - e o slug e exatamente o valor que o front usa para
   * montar a URL.
   */
  default String toValue(Slug slug) {
    return slug == null ? null : slug.value();
  }

  /**
   * A constante vira o codigo em minusculo, e nao o nome escrito em Java.
   *
   * <p>E o que impede o formato publicado de ser consequencia de como a constante foi nomeada:
   * renomear {@code FRAMEWORK} passa a ser refatoracao, enquanto mudar o codigo continua sendo
   * quebra de contrato.
   */
  default String toCode(TechnologyCategory category) {
    return category == null ? null : category.code();
  }
}
