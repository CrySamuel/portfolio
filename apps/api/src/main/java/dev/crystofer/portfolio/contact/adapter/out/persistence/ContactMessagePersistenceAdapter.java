package dev.crystofer.portfolio.contact.adapter.out.persistence;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import dev.crystofer.portfolio.contact.adapter.out.persistence.mapper.ContactMessagePersistenceMapper;
import dev.crystofer.portfolio.contact.adapter.out.persistence.repository.ContactMessageJpaRepository;
import dev.crystofer.portfolio.contact.domain.model.ContactMessage;
import dev.crystofer.portfolio.contact.domain.model.EmailStatus;
import dev.crystofer.portfolio.contact.domain.model.StoredContactMessage;
import dev.crystofer.portfolio.contact.domain.port.out.LoadFailedMessagesPort;
import dev.crystofer.portfolio.contact.domain.port.out.SaveContactMessagePort;
import dev.crystofer.portfolio.contact.domain.port.out.UpdateEmailStatusPort;

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
class ContactMessagePersistenceAdapter
    implements SaveContactMessagePort, UpdateEmailStatusPort, LoadFailedMessagesPort {

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

  /**
   * O nome do enum vai como texto, e nao o enum.
   *
   * <p>A consulta e nativa - ver o repositorio -, entao nao ha conversao de {@code @Enumerated} no
   * caminho. Mandar {@code name()} explicitamente e o que casa com o {@code CHECK} da V5, que
   * compara texto.
   */
  @Override
  public void updateStatus(long id, EmailStatus status) {
    repositorio.updateStatus(id, status.name());
  }

  @Override
  public List<StoredContactMessage> loadFailed(int limit) {
    return repositorio
        .findByEmailStatusOrderByCreatedAtAsc(EmailStatus.FAILED, Limit.of(limit))
        .stream()
        .map(entidade -> new StoredContactMessage(entidade.getId(), mapper.toDomain(entidade)))
        .toList();
  }
}
