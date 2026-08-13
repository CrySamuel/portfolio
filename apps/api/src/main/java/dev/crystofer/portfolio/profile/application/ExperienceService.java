package dev.crystofer.portfolio.profile.application;

import org.springframework.stereotype.Service;

import dev.crystofer.portfolio.profile.domain.model.Timeline;
import dev.crystofer.portfolio.profile.domain.port.in.ListExperiencesUseCase;
import dev.crystofer.portfolio.profile.domain.port.out.LoadExperiencePort;

/**
 * Orquestra a leitura da timeline.
 *
 * <p>A classe faz uma coisa, e e a unica que a faz: transformar o que a origem entregou no tipo que
 * carrega a garantia de ordem. A ordenacao em si nao esta aqui - ela e do {@link Timeline}, no
 * dominio. Este servico apenas atravessa a fronteira.
 *
 * <p>Distincao que vale registrar, porque o vizinho faz diferente: o {@link ProfileService} traduz
 * ausencia em erro, e este nao traduz nada. Origem vazia produz timeline vazia, que e um valor
 * legitimo do dominio e o estado real do projeto enquanto o dono nao informa a propria historia.
 * Lancar aqui deixaria a secao Sobre fora do ar por falta de conteudo.
 */
@Service
public class ExperienceService implements ListExperiencesUseCase {

  private final LoadExperiencePort loadExperiencePort;

  public ExperienceService(LoadExperiencePort loadExperiencePort) {
    this.loadExperiencePort = loadExperiencePort;
  }

  @Override
  public Timeline listExperiences() {
    return new Timeline(loadExperiencePort.loadExperiences());
  }
}
