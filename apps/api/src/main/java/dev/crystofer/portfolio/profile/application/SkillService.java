package dev.crystofer.portfolio.profile.application;

import org.springframework.stereotype.Service;

import dev.crystofer.portfolio.profile.domain.model.SkillCatalog;
import dev.crystofer.portfolio.profile.domain.port.in.ListSkillsUseCase;
import dev.crystofer.portfolio.profile.domain.port.out.LoadSkillPort;

/**
 * Orquestra a leitura das competencias.
 *
 * <p>Faz uma coisa so: transforma o que a origem entregou no tipo que carrega agrupamento e ordem.
 * Nem o agrupamento nem a ordenacao estao aqui - sao do {@link SkillCatalog} e do {@code
 * SkillCategory}, no dominio. Este servico apenas atravessa a fronteira, como o {@link
 * ExperienceService}.
 *
 * <p>Origem vazia produz catalogo vazio, e nao erro.
 */
@Service
public class SkillService implements ListSkillsUseCase {

  private final LoadSkillPort loadSkillPort;

  public SkillService(LoadSkillPort loadSkillPort) {
    this.loadSkillPort = loadSkillPort;
  }

  @Override
  public SkillCatalog listSkills() {
    return new SkillCatalog(loadSkillPort.loadSkillCategories());
  }
}
