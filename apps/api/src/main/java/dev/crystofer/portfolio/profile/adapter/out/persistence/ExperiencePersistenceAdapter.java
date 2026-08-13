package dev.crystofer.portfolio.profile.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.crystofer.portfolio.profile.adapter.out.persistence.mapper.ExperiencePersistenceMapper;
import dev.crystofer.portfolio.profile.adapter.out.persistence.repository.ExperienceJpaRepository;
import dev.crystofer.portfolio.profile.domain.model.Experience;
import dev.crystofer.portfolio.profile.domain.port.out.LoadExperiencePort;

/**
 * Implementa {@link LoadExperiencePort} com JPA.
 *
 * <p>A seta aponta para dentro: a interface e do dominio, a implementacao e daqui. Injecao por
 * construtor, como manda a secao 13.7 - nenhum campo anotado com {@code @Autowired}.
 *
 * <p>Uma consulta so, sem colecao preguicosa a resolver: os destaques vem na propria linha, dentro
 * do {@code jsonb}. E a vantagem de nao ter modelado uma tabela filha para uma lista de texto lida
 * sempre inteira - nao ha N+1 possivel aqui, e nao so evitado.
 */
@Component
public class ExperiencePersistenceAdapter implements LoadExperiencePort {

  private final ExperienceJpaRepository repository;
  private final ExperiencePersistenceMapper mapper;

  public ExperiencePersistenceAdapter(
      ExperienceJpaRepository repository, ExperiencePersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  /**
   * {@code readOnly = true} avisa o Hibernate de que nao ha o que sincronizar no fim, dispensando o
   * dirty checking das entidades e permitindo ao driver marcar a transacao como somente leitura.
   */
  @Override
  @Transactional(readOnly = true)
  public List<Experience> loadExperiences() {
    return repository.findAllByOrderByStartDateDesc().stream().map(mapper::toDomain).toList();
  }
}
