package dev.crystofer.portfolio.profile.domain.model;

/**
 * Nivel de dominio de uma competencia.
 *
 * <p>Enum, e nunca {@code String}: e o que o commit 30 existe para estabelecer. Com texto solto,
 * "advanced", "Advanced" e "avancado" seriam todos aceitos pelo compilador e so um deles
 * funcionaria - e o erro apareceria na tela, nao no build.
 *
 * <p><strong>Tres niveis, e nao cinco.</strong> A F05 recusa barra de percentual por ser arbitraria
 * e indefensavel numa entrevista; escala longa tem o mesmo defeito, disfarcado de precisao. Com
 * tres, cada rotulo significa alguma coisa e {@link #ADVANCED} continua custando caro.
 *
 * <p>A ordem de declaracao <strong>e</strong> a ordem de dominio, do menor para o maior, e e dela
 * que sai o {@code compareTo} usado para ordenar as competencias dentro de uma categoria. Trocar a
 * ordem das constantes muda o comportamento, e por isso ha teste sobre ela.
 */
public enum Proficiency {
  BASIC("basic"),
  INTERMEDIATE("intermediate"),
  ADVANCED("advanced");

  private final String code;

  Proficiency(String code) {
    this.code = code;
  }

  /**
   * O codigo em minusculo, que e o que a coluna guarda e o que o front recebe.
   *
   * <p>Existe para que o formato publicado nao seja consequencia de como a constante foi escrita em
   * Java. Renomear {@code ADVANCED} para outra coisa e refatoracao; mudar o codigo e quebra de
   * contrato, e a diferenca fica visivel porque sao duas coisas distintas no arquivo.
   */
  public String code() {
    return code;
  }
}
