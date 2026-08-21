package dev.crystofer.portfolio.contact.domain.port.out;

import dev.crystofer.portfolio.contact.domain.model.ContactMessage;

/**
 * Porta de saida: gravar a mensagem recebida.
 *
 * <p><strong>Esta porta pode lancar, e a diferenca em relacao a do GitHub e de
 * significado.</strong> La, indisponibilidade da origem e um estado previsto e o contrato promete
 * sempre devolver estatisticas. Aqui, nao conseguir gravar significa que a mensagem se perdeu - e o
 * visitante precisa saber disso para tentar de novo, em vez de receber uma confirmacao falsa.
 *
 * <p>E por isso que nao ha fallback nem retrato vazio: o unico desfecho honesto de uma escrita que
 * falhou e o erro subir.
 */
public interface SaveContactMessagePort {

  /**
   * @param message a mensagem a persistir
   * @return o identificador atribuido pelo banco
   */
  long save(ContactMessage message);
}
