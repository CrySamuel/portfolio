package dev.crystofer.portfolio.integration;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import dev.crystofer.portfolio.support.fixtures.ExperienceFixtures;
import dev.crystofer.portfolio.support.fixtures.ProfileFixtures;
import dev.crystofer.portfolio.support.fixtures.ProjectFixtures;
import dev.crystofer.portfolio.support.fixtures.SkillFixtures;

/**
 * Todo o conteudo publicado tem versao em ingles - a guarda que o ADR-0013 promete.
 *
 * <p><strong>Existe porque a reserva por linha esconde a ausencia.</strong> Um projeto novo sem
 * traducao nao quebra nada: aparece em portugues no meio da pagina em ingles, sem erro, sem log e
 * sem teste vermelho. A API foi desenhada assim de proposito - conteudo nunca some -, e o preco e
 * que a falta so seria vista por quem abrisse a versao em ingles. Aqui ela reprova o build.
 *
 * <p>O que se confere e o seed, e nao uma linha montada pelo teste: os quatro sao reaplicados antes
 * de cada caso, e a pergunta e se cada linha deles tem traducao en-US.
 *
 * <p><strong>A competencia fica de fora, e por decisao.</strong> Java, PostgreSQL e Docker se
 * chamam assim em qualquer idioma, e o seed so traduz o nome que muda. Exigir traducao de todas
 * obrigaria a escrever "Java" duas vezes - e a segunda copia seria o que divergiria.
 *
 * <p>Cada caso tambem exige que a tabela tenha linhas. Sem isso, "nenhuma linha sem traducao" seria
 * verdade numa tabela vazia, e a guarda passaria justamente quando o seed nao rodou.
 */
class SeedTranslationCoverageTest extends AbstractIntegrationTest {

  @Autowired DataSource dataSource;

  @BeforeEach
  void partirDosSeeds() {
    ProfileFixtures.reapplySeed(dataSource);
    ExperienceFixtures.reapplySeed(dataSource);
    SkillFixtures.reapplySeed(dataSource);
    ProjectFixtures.reapplySeed(dataSource);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "profile,        profile_translation,        profile_id",
    "experience,     experience_translation,     experience_id",
    "skill_category, skill_category_translation, category_id",
    "project,        project_translation,        project_id",
    "project_metric, project_metric_translation, metric_id",
  })
  @DisplayName("toda linha do seed deve ter traducao en-US")
  void shouldTranslateEveryRow_ofTheSeed(String tabela, String traducao, String chave) {
    Integer linhas = jdbcTemplate.queryForObject("SELECT count(*) FROM " + tabela, Integer.class);

    var semTraducao =
        jdbcTemplate.queryForList(
            "SELECT o.id FROM "
                + tabela
                + " o WHERE NOT EXISTS (SELECT 1 FROM "
                + traducao
                + " t WHERE t."
                + chave
                + " = o.id AND t.locale = 'en-US')",
            Long.class);

    assertThat(linhas).as("o seed de %s precisa ter linhas", tabela).isPositive();
    assertThat(semTraducao).as("linhas de %s sem traducao en-US no seed", tabela).isEmpty();
  }
}
