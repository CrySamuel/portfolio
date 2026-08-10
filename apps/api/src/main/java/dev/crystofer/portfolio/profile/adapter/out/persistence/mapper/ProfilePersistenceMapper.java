package dev.crystofer.portfolio.profile.adapter.out.persistence.mapper;

import java.util.Locale;

import org.mapstruct.Mapper;

import dev.crystofer.portfolio.profile.adapter.out.persistence.entity.ProfileEntity;
import dev.crystofer.portfolio.profile.adapter.out.persistence.entity.SocialLinkEntity;
import dev.crystofer.portfolio.profile.domain.model.Profile;
import dev.crystofer.portfolio.profile.domain.model.SocialLink;
import dev.crystofer.portfolio.profile.domain.model.SocialPlatform;

/**
 * Converte entidade em modelo de dominio. Um sentido so.
 *
 * <p>E aqui que a fronteira da secao 3.4 fica visivel: a entidade conhece o dominio, o dominio nao
 * conhece a entidade, e esta classe e a unica que conhece os dois. Trocar Postgres por outra coisa
 * reescreve este pacote e nada alem dele.
 *
 * <p>O MapStruct gera a implementacao na compilacao. Com {@code unmappedTargetPolicy=ERROR} ligado
 * no pom, adicionar um campo em {@link Profile} sem origem correspondente na entidade **reprova o
 * build** - o campo novo nao chega calado como null na tela, que e o modo classico de o mapeamento
 * manual falhar.
 */
@Mapper
public interface ProfilePersistenceMapper {

  Profile toDomain(ProfileEntity entity);

  SocialLink toDomain(SocialLinkEntity entity);

  /**
   * Traduz o valor da coluna para a constante do dominio.
   *
   * <p>O banco guarda minusculo, o enum e maiusculo. O MapStruct usa este metodo automaticamente ao
   * precisar de {@code String -> SocialPlatform}, porque a assinatura casa.
   *
   * <p>Falha alto de proposito. Uma plataforma desconhecida no banco significa migracao que entrou
   * sem o enum correspondente: devolver null ou pular a linha esconderia o problema e o site
   * mostraria um link a menos, sem nada em lugar nenhum dizendo por que.
   */
  default SocialPlatform toPlatform(String platform) {
    if (platform == null || platform.isBlank()) {
      throw new IllegalArgumentException("Plataforma nao pode ser vazia em social_link.platform");
    }
    try {
      // Locale.ROOT: em turco, "i".toUpperCase() nao produz "I", e "linkedin"
      // deixaria de casar com LINKEDIN. Bug que so aparece em uma maquina.
      return SocialPlatform.valueOf(platform.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException cause) {
      throw new IllegalArgumentException(
          "Plataforma desconhecida em social_link.platform: " + platform, cause);
    }
  }
}
