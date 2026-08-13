package dev.crystofer.portfolio.profile.domain.model;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A timeline profissional, sempre ordenada.
 *
 * <p><strong>Este tipo existe para que a ordem seja verdade por construcao</strong>, e nao uma
 * promessa que cada consulta SQL e cada template precisam lembrar de cumprir. E a mesma escolha que
 * {@link Profile} faz com os links sociais, e ela ja se provou uma vez neste projeto: descobriu-se,
 * medindo, que quem ordenava os links era o dominio, e que o {@code ORDER BY} da consulta era
 * redundancia. O indice {@code experience_start_date_desc_idx} da migracao V2 tem exatamente esse
 * papel - ele evita o sort, nao garante a ordem.
 *
 * <p>A consequencia pratica e o que torna a escolha util: mudar a consulta do adaptador, trocar o
 * banco ou remover o indice nao pode alterar a ordem de exibicao, porque nenhuma dessas coisas
 * atravessa este construtor.
 *
 * @param experiences as passagens, da mais recente para a mais antiga
 */
public record Timeline(List<Experience> experiences) {

  /**
   * Cronologica decrescente, com desempate total.
   *
   * <p>O criterio principal e a data de inicio, que e o que a Definition of Done do MVP 2 pede. Os
   * desempates nao sao preciosismo: sem eles, duas passagens iniciadas no mesmo mes sairiam em
   * ordem indefinida, e a pagina mudaria de aparencia entre dois deploys sem que nada tivesse
   * mudado.
   *
   * <p>Cargo atual antes de cargo encerrado quando as datas de inicio empatam - quem esta na
   * posicao hoje e a informacao mais relevante das duas. Depois disso, empresa e cargo em ordem
   * alfabetica fecham a ordem, e a tripla e a mesma chave natural que a migracao declara unica,
   * entao a ordenacao e total: nao existem duas passagens indistinguiveis.
   */
  private static final Comparator<Experience> CRONOLOGICA_DECRESCENTE =
      Comparator.comparing(Experience::startDate)
          .reversed()
          .thenComparing(Comparator.comparing(Experience::isCurrent).reversed())
          .thenComparing(Experience::company)
          .thenComparing(Experience::role);

  public Timeline {
    experiences = order(experiences);
  }

  /** Timeline sem nenhuma passagem, que e um estado legitimo e nao uma falha. */
  public static Timeline empty() {
    return new Timeline(List.of());
  }

  /**
   * A posicao atual, quando existe.
   *
   * <p>Devolve a primeira encontrada na ordem ja estabelecida, e nao "a unica": acumular dois
   * cargos ao mesmo tempo e possivel, e a migracao permite - a chave natural so proibe a mesma
   * empresa, no mesmo cargo, comecando no mesmo dia.
   */
  public Optional<Experience> findCurrent() {
    return experiences.stream().filter(Experience::isCurrent).findFirst();
  }

  /** Se nao ha nenhuma passagem registrada. */
  public boolean isEmpty() {
    return experiences.isEmpty();
  }

  /**
   * Copia defensiva, unicidade e ordenacao.
   *
   * <p>A unicidade repete a constraint {@code experience_company_role_start_uk} pela razao de
   * sempre: o dominio nao pode depender de o banco estar correto para estar correto.
   */
  private static List<Experience> order(List<Experience> experiences) {
    if (experiences == null) {
      throw new IllegalArgumentException(
          "Lista de experiencias e obrigatoria; use Timeline.empty() se vazia");
    }
    Set<List<Object>> seen = new HashSet<>();
    for (Experience experience : experiences) {
      List<Object> key = List.of(experience.company(), experience.role(), experience.startDate());
      if (!seen.add(key)) {
        throw new IllegalArgumentException(
            "Passagem repetida: "
                + experience.company()
                + " / "
                + experience.role()
                + " / "
                + experience.startDate());
      }
    }
    return experiences.stream().sorted(CRONOLOGICA_DECRESCENTE).toList();
  }
}
