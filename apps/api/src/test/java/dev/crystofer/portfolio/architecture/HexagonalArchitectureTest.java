package dev.crystofer.portfolio.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * A arquitetura da secao 3.4, escrita como teste.
 *
 * <p>Ate o commit anterior a pureza do dominio era propriedade verificada a mao: alguem varria os
 * imports e confirmava que so havia {@code java.*}. Varredura manual protege o codigo do dia em que
 * foi feita. A partir daqui a regra vive no build, e quem violar descobre na compilacao - nao numa
 * revisao que talvez aconteca.
 *
 * <p>O ArchUnit le o bytecode de {@code target/classes}, e nao o texto dos arquivos. A diferenca
 * importa: ele ve a dependencia que nenhum {@code import} declara - tipo de campo, parametro,
 * generico, anotacao, excecao lancada e classe usada por nome completo no meio de uma expressao. Um
 * {@code org.springframework.util.StringUtils.hasText(x)} escrito inteiro dentro do dominio nao
 * aparece na lista de imports; aparece aqui.
 *
 * <p>{@code DoNotIncludeTests} exclui {@code target/test-classes} da analise de proposito. As
 * regras descrevem a dependencia da producao: um teste de dominio depende legitimamente de JUnit e
 * AssertJ, e reprovar isso seria confundir "o dominio nao usa framework" com "ninguem pode testar o
 * dominio".
 */
@AnalyzeClasses(
    packages = "dev.crystofer.portfolio",
    importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

  /**
   * A regra da dependencia: as setas apontam para dentro.
   *
   * <p>A lista e maior do que a do plano porque nomeia tudo o que a varredura manual do commit 17
   * conferiu: Spring, jakarta, Hibernate, Jackson, Swagger e os dois aneis de fora. Cada item barra
   * um vazamento diferente:
   *
   * <ul>
   *   <li>{@code jakarta..} - e nao apenas {@code jakarta.persistence..} - porque {@code
   *       jakarta.validation} anotaria o dominio com a mesma facilidade, e a secao 3.4 escolheu
   *       validacao no construtor, que nao depende de ninguem executar um validador.
   *   <li>{@code tools.jackson..} <strong>e</strong> {@code com.fasterxml.jackson..} porque anotar
   *       o modelo com {@code @JsonProperty} amarraria o formato publicado a um nome de campo
   *       interno - a razao de existir do DTO do commit 19. Os dois pacotes estao na lista porque o
   *       Jackson 3, que o Boot 4 adotou, mudou o databind para {@code tools.jackson} e deixou as
   *       anotacoes em {@code com.fasterxml.jackson.annotation}. Barrar so o pacote novo deixaria a
   *       anotacao passar, que e justamente o vazamento mais provavel aqui.
   *   <li>{@code ..application..} porque a camada de aplicacao esta fora do dominio: ela conhece as
   *       portas, as portas nao a conhecem.
   * </ul>
   */
  @ArchTest
  static final ArchRule dominio_nao_conhece_framework =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta..",
              "org.hibernate..",
              "tools.jackson..",
              "com.fasterxml.jackson..",
              "io.swagger..",
              "..adapter..",
              "..application..")
          .because(
              "o dominio e o centro: trocar Postgres, HTTP ou Spring nao pode exigir tocar em regra"
                  + " de negocio (secao 3.4)");

  /**
   * A mesma fronteira pelo lado de dentro - e este e o par que pega o caso novo.
   *
   * <p>A regra acima e uma lista de proibidos, e uma lista de proibidos so protege contra o que
   * alguem se lembrou de escrever nela. A biblioteca de amanha - Lombok, um cliente de fila, um
   * validador - nao esta la, entra no dominio sem disparar nada e a fronteira se perde em silencio,
   * que e exatamente o modo de falha da secao 4.1 do estado do projeto.
   *
   * <p>Invertido em lista de permitidos, o padrao de falha inverte tambem: dependencia nova e
   * reprovada por omissao, e adicionar uma exige editar este arquivo. A conversa deixa de ser "isso
   * e proibido?" e passa a ser "isso e mesmo parte do dominio?", que e a pergunta certa e aparece
   * na revisao do commit.
   */
  @ArchTest
  static final ArchRule dominio_so_depende_de_si_mesmo_e_do_java =
      classes()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage("..domain..", "java..")
          .because(
              "lista de proibidos protege contra o framework que alguem previu; lista de permitidos"
                  + " protege contra o proximo");

  /**
   * A aplicacao nao conhece adaptador nenhum, e nao apenas o da web.
   *
   * <p>O plano fecha o caminho {@code application -> adapter.in.web}. Fechar {@code ..adapter..}
   * inteiro custa a mesma linha e pega o vazamento mais provavel na pratica: o caso de uso que
   * injeta o {@code ProfileJpaRepository} direto, "porque e mais rapido do que passar pela porta".
   * A porta deixaria de ser fronteira e viraria decoracao, e o servico so voltaria a ser testavel
   * com banco.
   */
  @ArchTest
  static final ArchRule aplicacao_nao_conhece_adaptador =
      noClasses()
          .that()
          .resideInAPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..adapter..")
          .because(
              "o caso de uso fala com portas; qual tecnologia as implementa e decisao do adaptador");

  /**
   * Entidade JPA nao atravessa a fronteira do adaptador que a criou.
   *
   * <p>O {@code ProfilePersistenceMapper} existe para isso, mas nada obrigava ninguem a usa-lo: um
   * controlador poderia devolver {@code ProfileEntity} e o codigo compilaria. O preco viria depois
   * - o JSON publico passaria a ter a forma do schema, renomear coluna viraria breaking change e o
   * cliente receberia proxy de lazy loading em vez de dado.
   *
   * <p>A regra anterior nao cobre este caso: entidade vazando para a web nao passa pelo dominio, e
   * por isso nenhuma das duas regras de {@code ..domain..} dispararia.
   */
  @ArchTest
  static final ArchRule entidade_jpa_nao_sai_da_persistencia =
      noClasses()
          .that()
          .resideOutsideOfPackage("..adapter.out.persistence..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..adapter.out.persistence.entity..")
          .because("o mapeamento para o dominio e a unica saida da entidade (secao 3.4)");

  /**
   * Modulos sem ciclo.
   *
   * <p>Cada primeiro nivel abaixo da raiz e uma fatia: hoje {@code profile} e {@code shared}. Ciclo
   * entre fatias e o comeco do monolito emaranhado que a secao 3.5 quer evitar: com um so caminho
   * de ida, extrair um modulo e mover uma pasta; com ida e volta, e um projeto.
   */
  @ArchTest
  static final ArchRule modulos_sem_ciclo =
      slices().matching("dev.crystofer.portfolio.(*)..").should().beFreeOfCycles();

  /**
   * Controlador tem nome de controlador.
   *
   * <p>Convencao de nome parece detalhe cosmetico, e nao e: e o que permite a este arquivo - e ao
   * proximo teste, e ao filtro de log, e a configuracao de seguranca - falar de "todos os
   * controladores" sem manter lista a mao.
   */
  @ArchTest
  static final ArchRule controlador_termina_com_controller =
      classes()
          .that()
          .areAnnotatedWith(RestController.class)
          .should()
          .haveSimpleNameEndingWith("Controller");

  /**
   * Nenhuma injecao por campo.
   *
   * <p>Construtor deixa a dependencia visivel na assinatura, permite campo {@code final} e mantem a
   * classe instanciavel num teste sem subir contexto - e o que faz o {@code ProfileServiceTest}
   * rodar em milissegundos. Com {@code @Autowired} no campo, a unica forma de montar o objeto e
   * reflection, e a lista de dependencias vira algo que se descobre lendo a classe inteira.
   */
  @ArchTest
  static final ArchRule sem_injecao_por_campo =
      noFields()
          .should()
          .beAnnotatedWith(Autowired.class)
          .because("injecao por construtor torna a dependencia explicita e testavel (secao 13.7)");
}
