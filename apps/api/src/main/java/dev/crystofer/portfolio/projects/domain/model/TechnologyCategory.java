package dev.crystofer.portfolio.projects.domain.model;

/**
 * Familia a que uma tecnologia pertence.
 *
 * <p>Enum, e nunca {@code String}, pela mesma razao de {@code Proficiency}: com texto solto,
 * "framework", "Framework" e "frameworks" seriam todos aceitos pelo compilador e virariam grupos
 * diferentes no filtro do commit 38 - um defeito que aparece na tela, nao no build.
 *
 * <p><strong>A ordem de declaracao nao tem peso semantico</strong>, e a diferenca em relacao a
 * {@code Proficiency} e o ponto. La a ordem <em>e</em> a escala, e trocar as constantes muda o
 * comportamento; aqui sao rotulos sem hierarquia - linguagem nao vale mais que banco de dados.
 * Reordenar este arquivo nao quebra nada, e nenhuma ordenacao do dominio depende dele.
 *
 * <p>Nao ha relacao com {@code SkillCategory}, e a separacao e arquitetural. A secao 2.8 proibe um
 * modulo conhecer o outro, e as duas taxonomias respondem a perguntas diferentes: uma agrupa
 * competencias para leitura, esta agrupa chips para filtragem.
 */
public enum TechnologyCategory {
  LANGUAGE("language"),
  FRAMEWORK("framework"),
  DATABASE("database"),
  INFRASTRUCTURE("infrastructure"),
  TOOL("tool");

  private final String code;

  TechnologyCategory(String code) {
    this.code = code;
  }

  /**
   * O codigo em minusculo, que e o que a coluna guarda e o que o front recebe.
   *
   * <p>Existe pela mesma razao que em {@code Proficiency}: o formato publicado nao deve ser
   * consequencia de como a constante foi escrita em Java. Renomear a constante e refatoracao; mudar
   * o codigo e quebra de contrato, e as duas coisas ficam visivelmente distintas no arquivo.
   */
  public String code() {
    return code;
  }
}
