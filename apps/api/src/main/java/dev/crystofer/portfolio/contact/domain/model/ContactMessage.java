package dev.crystofer.portfolio.contact.domain.model;

import dev.crystofer.portfolio.shared.domain.EmailAddress;

/**
 * Uma mensagem enviada pelo formulario de contato.
 *
 * <p><strong>E o primeiro modelo deste dominio que nasce de entrada externa.</strong> Todos os
 * outros descrevem conteudo que entrou por migracao, escrito por quem conhece o schema; este
 * descreve o que um desconhecido digitou num navegador. A diferenca aparece em cada invariante
 * abaixo - eles nao existem para documentar o formato esperado, e sim para recusar o inesperado.
 *
 * <p><strong>Os limites espelham a {@code V5__create_contact_message_table}, e a duplicacao e
 * proposital.</strong> O banco recusa por ultimo, quando a escrita nao passou por aqui; este tipo
 * recusa primeiro, com uma mensagem que diz qual campo e por que. Nenhum dos dois torna o outro
 * dispensavel: sem o banco, um caminho de escrita futuro passaria por cima; sem o tipo, o visitante
 * receberia um erro de restricao de integridade em vez de uma frase.
 *
 * <p><strong>O e-mail e {@link EmailAddress}, e nao {@code String}.</strong> E o que impede trocar
 * dois parametros de lugar - um bug que compila e que aqui gravaria o assunto no lugar do
 * remetente.
 *
 * @param name quem escreveu
 * @param email para onde a resposta vai; o {@code Reply-To} do e-mail de notificacao
 * @param subject assunto declarado
 * @param message o texto
 * @param ipHash SHA-256 do IP de origem com sal, ou {@code null} quando a origem nao pode ser
 *     determinada. <strong>Nunca o IP.</strong>
 * @param userAgent cabecalho do cliente, ou {@code null}
 * @param status estado da entrega por e-mail
 */
public record ContactMessage(
    String name,
    EmailAddress email,
    String subject,
    String message,
    String ipHash,
    String userAgent,
    EmailStatus status) {

  // Espelham os limites das colunas em V5__create_contact_message_table.sql.
  private static final int MAX_NAME_LENGTH = 120;
  private static final int MAX_SUBJECT_LENGTH = 150;
  private static final int MAX_MESSAGE_LENGTH = 5_000;

  /** SHA-256 em hexadecimal tem tamanho fixo, e a coluna e CHAR(64) por isso. */
  private static final int IP_HASH_LENGTH = 64;

  public ContactMessage {
    name = requireText(name, "Nome", MAX_NAME_LENGTH);
    subject = requireText(subject, "Assunto", MAX_SUBJECT_LENGTH);
    message = requireText(message, "Mensagem", MAX_MESSAGE_LENGTH);

    if (email == null) {
      throw new IllegalArgumentException("E-mail e obrigatorio");
    }
    if (status == null) {
      throw new IllegalArgumentException("Estado de entrega e obrigatorio");
    }

    ipHash = requireHashOrNull(ipHash);

    // Ausente e string em branco viram a mesma coisa, e o nulo e a
    // representacao escolhida: a coluna e nulavel, e guardar "" ali seria
    // afirmar que o cliente mandou um user agent vazio - o que nao aconteceu.
    userAgent = blankToNull(userAgent);
  }

  /**
   * A mensagem como ela chega do formulario: sempre {@link EmailStatus#PENDING}.
   *
   * <p><strong>O estado de nascimento nao e escolha de quem chama.</strong> Deixar o chamador
   * informa-lo abriria a porta para uma mensagem nascer SENT sem nunca ter sido enviada - e o
   * defeito apareceria como silencio, que e o mais caro de perceber. Quem precisa de outro estado e
   * o adaptador que le do banco, e ele usa o construtor canonico.
   */
  public static ContactMessage received(
      String name,
      EmailAddress email,
      String subject,
      String message,
      String ipHash,
      String userAgent) {
    return new ContactMessage(
        name, email, subject, message, ipHash, userAgent, EmailStatus.PENDING);
  }

  /**
   * A mesma mensagem com outro estado de entrega.
   *
   * <p>O record e imutavel, entao "marcar como enviada" produz um valor novo em vez de alterar
   * este. Quem grava a transicao e o adaptador de persistencia; o dominio so descreve qual e o
   * valor resultante.
   */
  public ContactMessage withStatus(EmailStatus novo) {
    if (novo == null) {
      throw new IllegalArgumentException("Estado de entrega e obrigatorio");
    }
    return new ContactMessage(name, email, subject, message, ipHash, userAgent, novo);
  }

  /**
   * O hash e conferido no formato, e nao apenas na presenca.
   *
   * <p>Sessenta e quatro caracteres hexadecimais e o que um SHA-256 produz. A checagem existe
   * porque a coluna e {@code CHAR(64)}: um valor mais curto seria <strong>preenchido com
   * espacos</strong> pelo PostgreSQL e voltaria diferente do que entrou, o que quebraria qualquer
   * comparacao futura entre remetentes - em silencio, e so na auditoria.
   */
  private static String requireHashOrNull(String value) {
    String hash = blankToNull(value);
    if (hash == null) {
      return null;
    }
    if (hash.length() != IP_HASH_LENGTH || !hash.chars().allMatch(ContactMessage::isHexDigit)) {
      throw new IllegalArgumentException(
          "Hash de IP deve ter " + IP_HASH_LENGTH + " caracteres hexadecimais");
    }
    return hash;
  }

  private static boolean isHexDigit(int c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String requireText(String value, String field, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " e obrigatorio");
    }
    String trimmed = value.trim();
    if (trimmed.length() > maxLength) {
      throw new IllegalArgumentException(
          field + " excede " + maxLength + " caracteres: " + trimmed.length());
    }
    return trimmed;
  }
}
