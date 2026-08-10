package dev.crystofer.portfolio.profile.domain.port.in;

import dev.crystofer.portfolio.profile.domain.model.Profile;

/**
 * Porta de entrada: obter o perfil publico.
 *
 * <p>E o contrato que o adaptador REST enxerga. O controlador do commit 19 depende desta interface,
 * nunca da classe que a implementa - e o que permite testar o controlador sem banco e trocar a
 * implementacao sem tocar no controlador (secao 13.2, Dependency Inversion).
 *
 * <p>Devolve {@link Profile}, e nao {@code Optional<Profile>}, ao contrario da porta de saida. A
 * assimetria e intencional: para quem consome o caso de uso, portfolio sem perfil nao e um estado
 * possivel do sistema, e sim uma falha - o seed garante a linha, e a tabela e travada em uma so.
 * Devolver Optional aqui empurraria para cada chamador a decisao de o que fazer com um sistema
 * quebrado, e cada um decidiria diferente. Quem traduz a ausencia em erro e a camada de aplicacao,
 * uma vez.
 */
public interface GetProfileUseCase {

  Profile getProfile();
}
