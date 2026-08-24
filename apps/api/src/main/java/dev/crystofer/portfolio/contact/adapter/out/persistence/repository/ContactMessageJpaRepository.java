package dev.crystofer.portfolio.contact.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.crystofer.portfolio.contact.adapter.out.persistence.entity.ContactMessageEntity;
import dev.crystofer.portfolio.contact.domain.model.EmailStatus;

/** Repositorio de {@code contact_message}. */
public interface ContactMessageJpaRepository extends JpaRepository<ContactMessageEntity, Long> {

  /**
   * Grava o desfecho da entrega, e a data com ele.
   *
   * <p><strong>Consulta nativa por causa do {@code now()}.</strong> A coluna {@code updated_at} tem
   * {@code DEFAULT now()}, e default so vale no {@code INSERT} - sem escrever a data aqui, ela
   * ficaria congelada no instante da criacao para sempre, e a coluna documentaria uma informacao
   * que nunca teve. A alternativa seria a aplicacao mandar o proprio relogio, que e justamente o
   * que a V5 recusa: com duas instancias, ou com uma que acorda de hibernacao dessincronizada, a
   * ordem das tentativas deixaria de ser confiavel.
   *
   * <p>Um {@code UPDATE} direto, e nao carregar a entidade para alterar um campo: a leitura seria
   * uma ida a mais ao banco para buscar dados que ninguem usa.
   */
  @Modifying
  @Query(
      value =
          "UPDATE contact_message SET email_status = :status, updated_at = now() WHERE id = :id",
      nativeQuery = true)
  int updateStatus(@Param("id") long id, @Param("status") String status);

  /**
   * As entregas que falharam, mais antigas primeiro.
   *
   * <p>E a consulta que o indice parcial da {@code V5} atende - {@code WHERE email_status =
   * 'FAILED'} ali, {@code ORDER BY created_at} aqui, que e a coluna indexada.
   */
  List<ContactMessageEntity> findByEmailStatusOrderByCreatedAtAsc(EmailStatus status, Limit limit);
}
