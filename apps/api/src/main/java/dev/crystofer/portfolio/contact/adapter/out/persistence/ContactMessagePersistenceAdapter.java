package dev.crystofer.portfolio.contact.adapter.out.persistence;

import org.springframework.stereotype.Component;

import dev.crystofer.portfolio.contact.adapter.out.persistence.mapper.ContactMessagePersistenceMapper;
import dev.crystofer.portfolio.contact.adapter.out.persistence.repository.ContactMessageJpaRepository;
import dev.crystofer.portfolio.contact.domain.model.ContactMessage;
import dev.crystofer.portfolio.contact.domain.port.out.SaveContactMessagePort;

/**
 * Grava a mensagem recebida.
 *
 * <p><strong>Sem {@code @Transactional} aqui, e a ausencia e deliberada.</strong> Quem abre a
 * transacao e o {@code ContactService}, porque e ele que precisa que a gravacao e a publicacao do
 * evento sejam a mesma unidade. Uma transacao propria neste adaptador confirmaria a escrita antes
 * de o servico terminar - e o ouvinte {@code AFTER_COMMIT} do commit 47 dispararia sobre um commit
 * que nao e o do fluxo.
 *
 * <p>Devolve o identificador em vez da entidade salva pelo mesmo motivo que existe o mapper: a
 * entidade nao sai daqui.
 */
@Component
class ContactMessagePersistenceAdapter implements SaveContactMessagePort {

  private final ContactMessageJpaRepository repositorio;
  private final ContactMessagePersistenceMapper mapper;

  ContactMessagePersistenceAdapter(
      ContactMessageJpaRepository repositorio, ContactMessagePersistenceMapper mapper) {
    this.repositorio = repositorio;
    this.mapper = mapper;
  }

  @Override
  public long save(ContactMessage message) {
    return repositorio.save(mapper.toEntity(message)).getId();
  }
}
