package dev.crystofer.portfolio.profile.adapter.in.web.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import dev.crystofer.portfolio.profile.adapter.in.web.dto.ExperienceResponse;
import dev.crystofer.portfolio.profile.domain.model.Experience;

/**
 * Converte passagens do dominio no corpo da resposta.
 *
 * <p>Simetrico ao mapper de persistencia: la a fronteira e com o banco, aqui com a web.
 *
 * <p>A conversao da lista preserva a ordem recebida, e e assim que a garantia do {@code Timeline}
 * chega ate o JSON. O mapper nao reordena nada - se reordenasse, seria mais um lugar decidindo a
 * ordem, e a garantia deixaria de ter dono unico.
 *
 * <p>{@code isCurrent} nao aparece no destino, e nao precisa: o {@code unmappedTargetPolicy=ERROR}
 * vigia campos do <em>alvo</em> sem origem, que e onde mora o defeito caro - campo publicado sempre
 * nulo. Propriedade de origem que ninguem le e apenas dado que este contrato escolheu nao expor.
 */
@Mapper
public interface ExperienceWebMapper {

  ExperienceResponse toResponse(Experience experience);

  List<ExperienceResponse> toResponse(List<Experience> experiences);
}
