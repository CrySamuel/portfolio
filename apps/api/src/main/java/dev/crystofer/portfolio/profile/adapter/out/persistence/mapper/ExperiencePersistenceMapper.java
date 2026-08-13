package dev.crystofer.portfolio.profile.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;

import dev.crystofer.portfolio.profile.adapter.out.persistence.entity.ExperienceEntity;
import dev.crystofer.portfolio.profile.domain.model.Experience;

/**
 * Converte entidade em modelo de dominio. Um sentido so.
 *
 * <p>A entidade conhece o dominio, o dominio nao conhece a entidade, e esta classe e a unica que
 * conhece os dois - a fronteira da secao 3.4 em forma de codigo.
 *
 * <p>Nao ha conversao escrita a mao aqui, ao contrario do {@link ProfilePersistenceMapper}, onde a
 * plataforma precisa virar enum. Os seis componentes de {@link Experience} tem tipo identico ao da
 * entidade, e o {@code jsonb} ja chega como lista pelo mapeamento do Hibernate. O {@code
 * unmappedTargetPolicy=ERROR} do pom continua sendo a guarda: componente novo no record sem origem
 * na entidade reprova a compilacao, em vez de chegar nulo na tela.
 */
@Mapper
public interface ExperiencePersistenceMapper {

  Experience toDomain(ExperienceEntity entity);
}
