package dev.crystofer.portfolio.contact.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import dev.crystofer.portfolio.contact.adapter.out.persistence.entity.ContactMessageEntity;
import dev.crystofer.portfolio.contact.domain.model.ContactMessage;
import dev.crystofer.portfolio.shared.domain.EmailAddress;

/**
 * Traduz entre a mensagem do dominio e a linha da tabela.
 *
 * <p><strong>Existe para que a entidade JPA nao saia da persistencia</strong>, que e uma regra
 * verificada pelo ArchUnit. Sem ele, o {@code @Entity} chegaria ao servico e o Hibernate passaria a
 * ditar a forma do dominio - o modelo teria construtor vazio, campos mutaveis e um {@code Long}
 * nulavel no lugar do identificador.
 *
 * <p><strong>A volta reconstroi o {@link EmailAddress} a partir do texto</strong>, e isso revalida
 * o endereco na leitura. Parece redundante, e nao e: uma linha gravada antes de a validacao existir
 * - ou por outro caminho - falha aqui, alto e claro, em vez de circular pelo sistema como um
 * endereco que ninguem consegue responder.
 */
@Component
public class ContactMessagePersistenceMapper {

  public ContactMessageEntity toEntity(ContactMessage message) {
    return new ContactMessageEntity(
        message.name(),
        message.email().value(),
        message.subject(),
        message.message(),
        message.ipHash(),
        message.userAgent(),
        message.status());
  }

  public ContactMessage toDomain(ContactMessageEntity entity) {
    return new ContactMessage(
        entity.getName(),
        new EmailAddress(entity.getEmail()),
        entity.getSubject(),
        entity.getMessage(),
        // CHAR(64) volta preenchido com espacos quando o valor gravado e mais
        // curto - e tambem quando ele e nulo em alguns drivers. O trim aqui e o
        // par do invariante do dominio, que recusa hash de tamanho diferente de
        // 64: sem ele, um valor legitimo voltaria com espacos e seria recusado
        // na leitura da propria linha que o sistema gravou.
        entity.getIpHash() == null ? null : entity.getIpHash().trim(),
        entity.getUserAgent(),
        entity.getEmailStatus());
  }
}
