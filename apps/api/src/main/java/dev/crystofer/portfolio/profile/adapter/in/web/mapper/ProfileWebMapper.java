package dev.crystofer.portfolio.profile.adapter.in.web.mapper;

import java.util.Locale;

import org.mapstruct.Mapper;

import dev.crystofer.portfolio.profile.adapter.in.web.dto.ProfileResponse;
import dev.crystofer.portfolio.profile.domain.model.Profile;
import dev.crystofer.portfolio.profile.domain.model.SocialLink;
import dev.crystofer.portfolio.profile.domain.model.SocialPlatform;

/**
 * Converte o modelo de dominio no corpo da resposta.
 *
 * <p>Simetrico ao mapper de persistencia: la a fronteira e com o banco, aqui com a web. O dominio
 * nao conhece nenhum dos dois, e cada adaptador traduz para o formato do seu lado.
 *
 * <p>Com {@code unmappedTargetPolicy=ERROR}, um campo novo em {@link ProfileResponse} sem origem no
 * dominio reprova o build. Vale mais aqui do que na persistencia: e este contrato que sai para o
 * mundo, e campo publicado sempre nulo e o defeito que ninguem reporta e todo mundo contorna.
 */
@Mapper
public interface ProfileWebMapper {

  ProfileResponse toResponse(Profile profile);

  ProfileResponse.SocialLinkResponse toResponse(SocialLink link);

  /**
   * O enum vira o codigo minusculo que o front ja usa.
   *
   * <p>Publicar {@code "GITHUB"} obrigaria o cliente a normalizar - e a decidir sozinho como. O
   * formato do JSON e decisao do adaptador, nao consequencia de como o enum foi escrito em Java.
   *
   * <p>Locale.ROOT pelo mesmo motivo do outro mapper: em turco, {@code "I".toLowerCase()} nao
   * produz {@code "i"}.
   */
  default String toPlatformCode(SocialPlatform platform) {
    return platform.name().toLowerCase(Locale.ROOT);
  }
}
