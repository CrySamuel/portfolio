package dev.crystofer.portfolio.profile.application;

import org.springframework.stereotype.Service;

import dev.crystofer.portfolio.profile.domain.model.Profile;
import dev.crystofer.portfolio.profile.domain.port.in.GetProfileUseCase;
import dev.crystofer.portfolio.profile.domain.port.out.LoadProfilePort;
import dev.crystofer.portfolio.shared.error.ResourceNotFoundException;

/**
 * Orquestra a leitura do perfil.
 *
 * <p>E aqui que a assimetria entre as portas se resolve, e este e o unico lugar onde ela se
 * resolve. {@link LoadProfilePort} devolve {@code Optional} porque origem sem linha e um estado
 * legitimo do dado; {@link GetProfileUseCase} devolve {@code Profile} porque portfolio sem perfil e
 * falha do sistema, nao estado previsto. A conversao de um no outro acontece nesta classe, uma vez.
 *
 * <p>Se o {@code Optional} vazasse para o controlador, cada chamador futuro decidiria por conta o
 * que fazer com um sistema quebrado - e decidiriam diferente. Um devolveria 404, outro um corpo
 * vazio, outro 200 com campos nulos.
 *
 * <p>A classe nao tem mais nada porque nao ha mais nada: nao formata resposta, nao conhece HTTP,
 * nao decide cache. Um servico de aplicacao que crescesse para alem disso estaria acumulando
 * responsabilidade de adaptador (secao 13.2).
 */
@Service
public class ProfileService implements GetProfileUseCase {

  private final LoadProfilePort loadProfilePort;

  public ProfileService(LoadProfilePort loadProfilePort) {
    this.loadProfilePort = loadProfilePort;
  }

  /**
   * A mensagem e a que vai para o cliente, entao ela nao cita arquivo de migracao nem tabela. Se
   * isto disparar, a causa quase certa e o seed de {@code R__seed_profile.sql} nao aplicado - mas
   * esse diagnostico e nosso, e o lugar dele e o log do {@code GlobalExceptionHandler}, nao o corpo
   * de uma resposta publica (secao 2.4).
   */
  @Override
  public Profile getProfile() {
    return loadProfilePort
        .loadProfile()
        .orElseThrow(() -> new ResourceNotFoundException("profile", "Perfil nao encontrado"));
  }
}
