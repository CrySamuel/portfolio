package dev.crystofer.portfolio.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * A fronteira entre modulos de negocio (secao 3.5).
 *
 * <p>O backend e um monolito modular: {@code profile} hoje, {@code projects}, {@code github} e
 * {@code contact} nos MVPs seguintes. Cada um tem dominio, casos de uso e adaptadores proprios, e
 * nenhum importa o outro - a comunicacao prevista e por evento de aplicacao do Spring.
 *
 * <p>Vale a pena separar esta regra da hexagonal porque as duas erram por motivos diferentes. A
 * hexagonal cuida da direcao das setas dentro de um modulo; esta cuida de quantos modulos uma
 * mudanca alcanca. Um {@code ProjectService} que chame {@code ProfileService} compila, passa nos
 * testes e cumpre a hexagonal - e, ainda assim, transforma dois modulos em um: dai em diante
 * qualquer mudanca em {@code profile} pode quebrar {@code projects}, e "extrair um modulo e mover
 * uma pasta" deixa de ser verdade.
 */
@AnalyzeClasses(
    packages = "dev.crystofer.portfolio",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryTest {

  /**
   * Cada pacote de primeiro nivel abaixo da raiz e um modulo.
   *
   * <p>Escrito como fatia, e nao como lista de nomes, de proposito: {@code projects}, {@code
   * github} e {@code contact} passam a ser vigiados no instante em que a pasta existir, sem ninguem
   * lembrar de voltar aqui. Regra que depende de alguem lembrar e a guarda muda da secao 4.1
   * esperando a vez dela.
   */
  private static final String MODULOS = "dev.crystofer.portfolio.(*)..";

  /**
   * Modulo nao conhece modulo. {@code shared} e a excecao, e e explicita.
   *
   * <p>{@code shared} guarda o que e transversal por natureza - {@code EmailAddress}, o tratador de
   * erro, a configuracao do OpenAPI. Se cada modulo trouxesse a propria copia, o formato de erro do
   * portfolio dependeria do endpoint que o cliente chamou.
   *
   * <p>A excecao vale em um sentido so: qualquer modulo pode depender de {@code shared}, e {@code
   * shared} nao pode depender de modulo nenhum - o {@code ignoreDependency} filtra pelo destino,
   * nao pela origem. O sentido proibido e o que importa: uma unica seta de {@code shared} para
   * {@code profile} tornaria {@code shared} refem do modulo, e a ida-e-volta acabaria carregando o
   * portfolio inteiro para dentro de qualquer teste.
   */
  @ArchTest
  static final ArchRule modulos_nao_se_conhecem =
      slices()
          .matching(MODULOS)
          .should()
          .notDependOnEachOther()
          .ignoreDependency(alwaysTrue(), resideInAPackage("..shared.."))
          .because(
              "modulo que chama modulo vira modulo unico; a comunicacao prevista e por evento"
                  + " (secao 3.5)");
}
