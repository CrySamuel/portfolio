package dev.crystofer.portfolio.profile.adapter.out.persistence.mapper;

import java.util.Locale;

import org.mapstruct.Mapper;

import dev.crystofer.portfolio.profile.adapter.out.persistence.entity.SkillCategoryEntity;
import dev.crystofer.portfolio.profile.adapter.out.persistence.entity.SkillEntity;
import dev.crystofer.portfolio.profile.domain.model.Proficiency;
import dev.crystofer.portfolio.profile.domain.model.Skill;
import dev.crystofer.portfolio.profile.domain.model.SkillCategory;

/**
 * Converte entidade em modelo de dominio. Um sentido so.
 *
 * <p>Com {@code unmappedTargetPolicy=ERROR}, componente novo no record sem origem na entidade
 * reprova a compilacao.
 */
@Mapper
public interface SkillPersistenceMapper {

  SkillCategory toDomain(SkillCategoryEntity entity);

  Skill toDomain(SkillEntity entity);

  /**
   * Traduz o codigo da coluna para a constante do dominio.
   *
   * <p>Falha alto de proposito, como o mapper de plataforma ja fazia. Um nivel desconhecido no
   * banco significa migracao que entrou sem o enum correspondente: devolver null ou pular a linha
   * esconderia o problema e a tela mostraria uma competencia a menos, sem nada em lugar nenhum
   * dizendo por que.
   *
   * <p>O {@code CHECK} da V3 ja recusa valor fora da escala, entao este caminho so dispara se
   * alguem acrescentar valor a coluna sem acrescentar ao enum - que e exatamente quando se quer o
   * erro.
   *
   * <p>{@code Locale.ROOT} pelo mesmo motivo dos outros mappers: em turco, {@code
   * "i".toUpperCase()} nao produz {@code "I"}.
   */
  default Proficiency toProficiency(String proficiency) {
    if (proficiency == null || proficiency.isBlank()) {
      throw new IllegalArgumentException("Nivel nao pode ser vazio em skill.proficiency");
    }
    try {
      return Proficiency.valueOf(proficiency.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException cause) {
      throw new IllegalArgumentException(
          "Nivel desconhecido em skill.proficiency: " + proficiency, cause);
    }
  }
}
