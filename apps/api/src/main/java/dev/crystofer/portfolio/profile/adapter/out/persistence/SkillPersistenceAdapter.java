package dev.crystofer.portfolio.profile.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.crystofer.portfolio.profile.adapter.out.persistence.mapper.SkillPersistenceMapper;
import dev.crystofer.portfolio.profile.adapter.out.persistence.repository.SkillCategoryJpaRepository;
import dev.crystofer.portfolio.profile.domain.model.SkillCategory;
import dev.crystofer.portfolio.profile.domain.port.out.LoadSkillPort;

/**
 * Implementa {@link LoadSkillPort} com JPA.
 *
 * <p>A seta aponta para dentro: a interface e do dominio, a implementacao e daqui. Injecao por
 * construtor, como manda a secao 13.7.
 */
@Component
public class SkillPersistenceAdapter implements LoadSkillPort {

  private final SkillCategoryJpaRepository repository;
  private final SkillPersistenceMapper mapper;

  public SkillPersistenceAdapter(
      SkillCategoryJpaRepository repository, SkillPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  /**
   * {@code readOnly = true} dispensa o dirty checking das entidades e das colecoes, e permite ao
   * driver marcar a transacao como somente leitura.
   */
  @Override
  @Transactional(readOnly = true)
  public List<SkillCategory> loadSkillCategories() {
    return repository.findAllByOrderByDisplayOrderAsc().stream().map(mapper::toDomain).toList();
  }
}
