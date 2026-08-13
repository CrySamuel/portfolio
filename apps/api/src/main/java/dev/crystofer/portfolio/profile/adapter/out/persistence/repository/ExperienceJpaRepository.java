package dev.crystofer.portfolio.profile.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.crystofer.portfolio.profile.adapter.out.persistence.entity.ExperienceEntity;

/**
 * Acesso Spring Data a {@code experience}.
 *
 * <p>Detalhe de infraestrutura, e por isso vive no adaptador. O caso de uso enxerga {@code
 * LoadExperiencePort}; se dependesse daqui, o nucleo do negocio passaria a conhecer Spring Data.
 */
public interface ExperienceJpaRepository extends JpaRepository<ExperienceEntity, Long> {

  /**
   * Todas as passagens, da mais recente para a mais antiga.
   *
   * <p><strong>O {@code ORDER BY} nao e o que garante a ordem exibida</strong> - quem garante e o
   * {@code Timeline} do dominio, que reordena o que receber. Esta clausula existe para que o
   * planejador use o {@code experience_start_date_desc_idx} e devolva as linhas ja ordenadas, sem
   * passo de sort.
   *
   * <p>A distincao importa e ja custou uma suposicao errada neste projeto: no modulo de perfil
   * acreditou-se que a ordenacao vinha da consulta, e a medida mostrou que vinha do dominio.
   * Corolario que segue valendo aqui: <em>mudanca nesta consulta nao pode ser usada como argumento
   * de que a ordem esta garantida</em>, e remover esta clausula nao deve alterar nenhuma resposta.
   */
  List<ExperienceEntity> findAllByOrderByStartDateDesc();
}
