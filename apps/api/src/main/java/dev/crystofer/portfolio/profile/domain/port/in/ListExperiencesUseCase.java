package dev.crystofer.portfolio.profile.domain.port.in;

import dev.crystofer.portfolio.profile.domain.model.Timeline;

/**
 * Porta de entrada: listar a timeline profissional.
 *
 * <p>Devolve {@link Timeline}, e nao {@code List<Experience>}. A diferenca e o ponto do commit: uma
 * lista solta obrigaria cada chamador a ordenar, e dois chamadores ordenariam diferente. O tipo
 * carrega a garantia junto com o dado.
 *
 * <p>Nao devolve {@code Optional} nem lanca quando nao ha nenhuma passagem, e aqui esta a diferenca
 * em relacao a {@link GetProfileUseCase}. Portfolio sem perfil e sistema quebrado, porque o seed
 * garante a linha e a tabela e travada em uma so; portfolio sem experiencia cadastrada e apenas um
 * portfolio cujo dono ainda nao preencheu a timeline - estado legitimo, que o site representa como
 * secao vazia e nao como erro. Tratar os dois casos igual seria transformar conteudo ausente em
 * falha de infraestrutura.
 */
public interface ListExperiencesUseCase {

  Timeline listExperiences();
}
