package dev.crystofer.portfolio.profile.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import dev.crystofer.portfolio.profile.adapter.out.persistence.entity.ProfileEntity;

/**
 * Acesso Spring Data a {@code profile}.
 *
 * <p>Esta interface e detalhe de infraestrutura e vive no adaptador, nao no dominio. O caso de uso
 * enxerga {@code LoadProfilePort}; se ele dependesse daqui, dependeria de {@code JpaRepository}, e
 * o nucleo do negocio passaria a conhecer Spring Data - exatamente o acoplamento que a secao 3.4
 * existe para evitar.
 */
public interface ProfileJpaRepository extends JpaRepository<ProfileEntity, Long> {

  /**
   * O perfil, com os links na mesma consulta.
   *
   * <p>Sem o {@code @EntityGraph} seriam duas idas ao banco: uma pelo perfil, outra disparada ao
   * tocar a colecao preguicosa. Com ele, o Hibernate emite um LEFT JOIN e resolve tudo de uma vez.
   *
   * <p>Chamar isso de "prevencao de N+1" seria exagero aqui, e o numero explica por que: a tabela
   * tem uma linha, entao N e 1 e o custo evitado e um round trip. O padrao esta posto agora porque
   * o modulo de projetos do MVP 3 le listas de verdade, e la a mesma omissao custa uma consulta por
   * projeto. Vale estabelecer o habito enquanto o erro ainda e barato.
   */
  @EntityGraph(attributePaths = "socialLinks")
  Optional<ProfileEntity> findFirstByOrderByIdAsc();
}
