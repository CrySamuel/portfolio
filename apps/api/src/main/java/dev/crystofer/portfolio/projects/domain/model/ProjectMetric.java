package dev.crystofer.portfolio.projects.domain.model;

/**
 * Um numero que sustenta o resultado declarado por um projeto.
 *
 * <p>Existe porque a secao 16 do plano nomeia o risco do MVP 3 - a tentacao de escrever case
 * generico - e escolhe uma mitigacao estrutural: se nao ha numero, o case ainda nao esta pronto.
 *
 * <p>O valor e texto, e nao um tipo numerico. Ele carrega unidade, e as unidades nao sao
 * comensuraveis entre si: "80ms", "40%", "R$ 800+", "4h para 2h". Guardar numero obrigaria uma
 * coluna de unidade ao lado e ainda assim nao caberia o ultimo caso - o tipo seguiria mentindo, so
 * que com mais campos. Como nenhuma conta e feita sobre esses valores, texto e a representacao
 * honesta.
 *
 * @param label o que esta sendo medido
 * @param value o valor com a unidade, como sera exibido
 * @param displayOrder posicao entre as metricas do projeto; a mais forte primeiro
 */
public record ProjectMetric(String label, String value, int displayOrder) {

  // Espelham os limites das colunas em V4__create_project_tables.sql.
  private static final int MAX_LABEL_LENGTH = 60;
  private static final int MAX_VALUE_LENGTH = 40;

  public ProjectMetric {
    label = requireText(label, "Rotulo da metrica", MAX_LABEL_LENGTH);
    value = requireText(value, "Valor da metrica", MAX_VALUE_LENGTH);
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
