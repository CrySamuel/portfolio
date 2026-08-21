package dev.crystofer.portfolio.contact.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.crystofer.portfolio.contact.adapter.out.persistence.entity.ContactMessageEntity;

/**
 * Repositorio de {@code contact_message}.
 *
 * <p>Sem metodo proprio ainda: o commit 46 so precisa inserir, e {@code save} vem do {@link
 * JpaRepository}. As consultas do reprocessamento - buscar o que ficou FAILED - entram no commit
 * 47, que e quem tem o job para usa-las.
 */
public interface ContactMessageJpaRepository extends JpaRepository<ContactMessageEntity, Long> {}
