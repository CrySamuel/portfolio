package dev.crystofer.portfolio.contact.domain.model;

/**
 * O estado da entrega por e-mail de uma mensagem de contato.
 *
 * <p><strong>Estes tres valores sao a razao de a mensagem nunca se perder.</strong> Sem eles a
 * unica forma de saber se um e-mail saiu seria olhar a caixa de entrada, e uma falha do provedor
 * viraria uma mensagem que nunca existiu para ninguem. Com o estado gravado junto do conteudo, o
 * job de reprocessamento sabe exatamente o que tentar de novo.
 *
 * <p><strong>Nao ha estado para "em envio".</strong> A tentacao existe - marcar SENDING antes de
 * chamar o provedor -, e ela cria um estado do qual nao se sai sozinho: uma instancia que morre no
 * meio do envio deixa a linha travada ali, e nenhum reprocessamento a pega porque ela nao esta
 * FAILED. Com tres estados, o pior caso e um e-mail duplicado; com quatro, o pior caso e um e-mail
 * que nunca sai.
 *
 * <p>O nome de cada constante e gravado como texto na coluna {@code email_status}, e a lista
 * permitida esta repetida num {@code CHECK} da {@code V5__create_contact_message_table}. A
 * duplicacao e deliberada: o banco recusa o valor invalido mesmo quando a escrita nao passa por
 * este enum.
 */
public enum EmailStatus {

  /**
   * Persistida e ainda nao enviada.
   *
   * <p>E o estado de nascimento, aqui e no schema - o {@code DEFAULT 'PENDING'} da coluna diz a
   * mesma coisa. A concordancia e proposital: uma linha inserida por qualquer caminho, inclusive um
   * {@code INSERT} manual, entra na fila de envio em vez de ficar parada num estado que ninguem
   * processa.
   */
  PENDING,

  /** Entregue ao provedor, que aceitou a mensagem. */
  SENT,

  /**
   * O provedor recusou ou nao respondeu.
   *
   * <p>E o unico estado que o job de reprocessamento procura, e por isso e o unico indexado - ver o
   * indice parcial da {@code V5}.
   */
  FAILED
}
