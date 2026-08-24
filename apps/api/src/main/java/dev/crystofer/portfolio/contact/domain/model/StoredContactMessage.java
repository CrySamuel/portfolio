package dev.crystofer.portfolio.contact.domain.model;

/**
 * Uma mensagem que ja esta no banco, com o identificador dela.
 *
 * <p><strong>Existe porque entregar exige saber qual linha atualizar.</strong> {@link
 * ContactMessage} descreve o que o visitante escreveu e nao carrega identidade - e nao deveria
 * carregar, porque ela nasce antes de existir linha nenhuma. Depois de gravada, as duas coisas
 * andam juntas: o conteudo monta o e-mail, o identificador diz onde gravar o desfecho.
 *
 * <p>A alternativa seria pendurar um {@code Long id} nulavel no proprio {@code ContactMessage}, e
 * ela e pior: todo consumidor passaria a lidar com "mensagem que talvez tenha id", e o unico jeito
 * de saber qual caso e o seu seria testar o nulo. Dois tipos dizem a mesma coisa sem pergunta.
 *
 * @param id identificador da linha
 * @param message o conteudo gravado
 */
public record StoredContactMessage(long id, ContactMessage message) {

  public StoredContactMessage {
    if (message == null) {
      throw new IllegalArgumentException("Mensagem e obrigatoria");
    }
    if (id <= 0) {
      throw new IllegalArgumentException("Identificador e obrigatorio");
    }
  }
}
