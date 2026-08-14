package dev.crystofer.portfolio.projects.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.crystofer.portfolio.projects.adapter.out.persistence.mapper.ProjectPersistenceMapper;
import dev.crystofer.portfolio.projects.adapter.out.persistence.repository.ProjectJpaRepository;
import dev.crystofer.portfolio.projects.domain.model.Project;
import dev.crystofer.portfolio.projects.domain.port.out.LoadProjectPort;
import dev.crystofer.portfolio.shared.domain.Slug;

/**
 * Implementa {@link LoadProjectPort} com JPA.
 *
 * <p>A seta aponta para dentro: a interface e do dominio, a implementacao e daqui. Injecao por
 * construtor, como manda a seccao 13.7.
 */
@Component
public class ProjectPersistenceAdapter implements LoadProjectPort {

  private final ProjectJpaRepository repository;
  private final ProjectPersistenceMapper mapper;

  public ProjectPersistenceAdapter(
      ProjectJpaRepository repository, ProjectPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  /**
   * {@code readOnly = true} dispensa o dirty checking das entidades e das colecoes, e permite ao
   * driver marcar a transacao como somente leitura.
   *
   * <p>A transacao precisa envolver o mapeamento, e nao so a consulta: e ao converter que as
   * metricas sao tocadas pela primeira vez, e e esse toque que dispara o lote do
   * {@code @BatchSize}. Fora da transacao, a colecao preguicosa estouraria em {@code
   * LazyInitializationException}.
   */
  @Override
  @Transactional(readOnly = true)
  public List<Project> loadProjects() {
    return repository.findAllByOrderByDisplayOrderAsc().stream().map(mapper::toDomain).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Project> loadProjectBySlug(Slug slug) {
    return repository.findBySlug(slug.value()).map(mapper::toDomain);
  }
}
