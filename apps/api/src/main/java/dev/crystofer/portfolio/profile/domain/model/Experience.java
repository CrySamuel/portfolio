package dev.crystofer.portfolio.profile.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Uma passagem profissional da timeline.
 *
 * <p>Record pelo mesmo motivo de {@link Profile}: e um valor lido, nunca alterado em memoria. O
 * conteudo entra por migracao (ADR-0004).
 *
 * <p>As validacoes repetem o que a {@code V2__create_experience_table} ja restringe, e a repeticao
 * e deliberada - o dominio nao pode depender de o banco estar correto para estar correto. O banco
 * recusaria com erro de driver, ja dentro da transacao; aqui a recusa vem antes, dizendo qual campo
 * estourou.
 *
 * @param company empresa ou cliente
 * @param role cargo exercido
 * @param startDate mes de entrada
 * @param endDate mes de saida; {@code null} significa <strong>cargo atual</strong>
 * @param description o que foi feito, em texto corrido
 * @param highlights destaques da posicao, sem entradas em branco
 */
public record Experience(
    String company,
    String role,
    LocalDate startDate,
    LocalDate endDate,
    String description,
    List<String> highlights) {

  // Espelham os limites das colunas em V2__create_experience_table.sql.
  private static final int MAX_COMPANY_LENGTH = 120;
  private static final int MAX_ROLE_LENGTH = 120;

  public Experience {
    company = requireText(company, "Empresa", MAX_COMPANY_LENGTH);
    role = requireText(role, "Cargo", MAX_ROLE_LENGTH);
    description = requireText(description, "Descricao", Integer.MAX_VALUE);
    startDate = requireStartDate(startDate);
    requirePeriod(startDate, endDate);
    highlights = normalizeHighlights(highlights);
  }

  /**
   * Se a pessoa ainda ocupa a posicao.
   *
   * <p>A ausencia de data de saida <em>e</em> a resposta, e por isso nao ha campo booleano
   * guardando a mesma informacao: dois lugares dizendo a mesma coisa e um lugar onde eles podem
   * divergir. O badge "Atual" da interface le daqui.
   */
  public boolean isCurrent() {
    return endDate == null;
  }

  /** Data de saida, quando a posicao ja terminou. */
  public Optional<LocalDate> findEndDate() {
    return Optional.ofNullable(endDate);
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

  private static LocalDate requireStartDate(LocalDate startDate) {
    if (startDate == null) {
      throw new IllegalArgumentException("Data de inicio e obrigatoria");
    }
    return startDate;
  }

  /**
   * O mesmo limite do {@code experience_period_ck}, e igual tambem no limite.
   *
   * <p>Entrar e sair no mesmo dia e valido - acontece em contrato que nao vingou, e recusar isso
   * seria inventar uma regra que o negocio nao tem.
   */
  private static void requirePeriod(LocalDate startDate, LocalDate endDate) {
    if (endDate != null && endDate.isBefore(startDate)) {
      throw new IllegalArgumentException(
          "Data de saida (" + endDate + ") e anterior a de inicio (" + startDate + ")");
    }
  }

  /**
   * Copia defensiva, e recusa de entrada vazia.
   *
   * <p>A copia importa pela mesma razao que em {@link Profile}: sem ela o record guardaria a
   * referencia recebida, e quem a passou poderia seguir alterando a lista depois.
   *
   * <p>Destaque em branco e recusado em vez de descartado em silencio. Uma lista com string vazia
   * chega da origem por erro de seed, e o efeito visivel seria um marcador sem texto na tela -
   * defeito que ninguem associa a um espaco perdido num INSERT.
   */
  private static List<String> normalizeHighlights(List<String> highlights) {
    if (highlights == null) {
      throw new IllegalArgumentException(
          "Lista de destaques e obrigatoria; use List.of() se vazia");
    }
    return highlights.stream()
        .map(
            highlight -> {
              if (highlight == null || highlight.isBlank()) {
                throw new IllegalArgumentException("Destaque em branco na lista");
              }
              return highlight.trim();
            })
        .toList();
  }
}
