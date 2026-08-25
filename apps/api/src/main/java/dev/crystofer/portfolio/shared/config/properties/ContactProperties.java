package dev.crystofer.portfolio.shared.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * O que o recebimento de mensagens le de configuracao.
 *
 * @param ipHashSalt sal do hash de IP; vazio faz a aplicacao sortear um por boot
 * @param maxPerHour quantas mensagens um mesmo remetente pode enviar por hora
 */
@ConfigurationProperties(prefix = "portfolio.contact")
public record ContactProperties(
    @DefaultValue("") String ipHashSalt, @DefaultValue("5") int maxPerHour) {

  public ContactProperties {
    if (maxPerHour < 1) {
      throw new IllegalArgumentException("O limite por hora precisa ser pelo menos 1");
    }
  }

  /**
   * Ha sal configurado?
   *
   * <p><strong>Sem sal, a aplicacao sorteia um a cada boot em vez de hashear sem ele.</strong> Um
   * hash de IP sem sal e reversivel por forca bruta em minutos - o espaco IPv4 tem 4 bilhoes de
   * itens -, entao "sem sal" seria equivalente a guardar o IP em claro, que e exatamente o que a
   * coluna existe para evitar.
   *
   * <p>O custo do sal sorteado e conhecido e aceitavel: os hashes deixam de ser comparaveis entre
   * reinicios, entao a auditoria so enxerga dentro de uma mesma execucao. E melhor perder
   * correlacao do que perder a protecao - e em producao a variavel esta cadastrada.
   */
  public boolean saltConfigured() {
    return !ipHashSalt.isBlank();
  }
}
