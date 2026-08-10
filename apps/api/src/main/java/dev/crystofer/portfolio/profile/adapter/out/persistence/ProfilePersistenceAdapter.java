package dev.crystofer.portfolio.profile.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.crystofer.portfolio.profile.adapter.out.persistence.mapper.ProfilePersistenceMapper;
import dev.crystofer.portfolio.profile.adapter.out.persistence.repository.ProfileJpaRepository;
import dev.crystofer.portfolio.profile.domain.model.Profile;
import dev.crystofer.portfolio.profile.domain.port.out.LoadProfilePort;

/**
 * Implementa {@link LoadProfilePort} com JPA.
 *
 * <p>A seta aponta para dentro: a interface e do dominio, a implementacao e daqui. Nenhuma classe
 * de dominio menciona esta - e o ArchUnit do commit 20 passa a garantir isso no build.
 *
 * <p>Injecao por construtor, nao por campo. Torna a dependencia explicita na assinatura, permite
 * campo final e deixa a classe instanciavel num teste sem subir contexto. A secao 13.7 transforma
 * isso em regra: nenhum campo pode ser anotado com {@code @Autowired}.
 */
@Component
public class ProfilePersistenceAdapter implements LoadProfilePort {

  private final ProfileJpaRepository repository;
  private final ProfilePersistenceMapper mapper;

  public ProfilePersistenceAdapter(
      ProfileJpaRepository repository, ProfilePersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  /**
   * {@code readOnly = true} nao e enfeite: avisa o Hibernate de que nao ha o que sincronizar no
   * fim, dispensando o dirty checking da entidade e dos links, e permite ao driver marcar a
   * transacao como somente leitura.
   */
  @Override
  @Transactional(readOnly = true)
  public Optional<Profile> loadProfile() {
    return repository.findFirstByOrderByIdAsc().map(mapper::toDomain);
  }
}
