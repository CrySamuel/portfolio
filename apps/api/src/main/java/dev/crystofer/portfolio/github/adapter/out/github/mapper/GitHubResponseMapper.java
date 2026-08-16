package dev.crystofer.portfolio.github.adapter.out.github.mapper;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import dev.crystofer.portfolio.github.adapter.out.github.dto.GitHubRepositoryResponse;
import dev.crystofer.portfolio.github.domain.model.LanguageUsage;
import dev.crystofer.portfolio.github.domain.model.RepositorySummary;

/**
 * Traduz a resposta do GitHub para o dominio.
 *
 * <p><strong>Escrito a mao, e nao gerado por MapStruct como os outros mappers do projeto.</strong>
 * O MapStruct ganha onde os dois lados tem a mesma forma e o valor esta em o compilador reprovar
 * campo sem origem - e o caso de entidade JPA para dominio. Aqui as formas nao se correspondem: as
 * linguagens chegam como um mapa de nome para bytes que vira uma lista, o instante do push vira
 * data em fuso explicito, e metade dos campos da resposta existe so para ser filtrada. Um mapper
 * gerado precisaria de tantas expressoes escritas a mao que sobraria a anotacao, sem o ganho.
 */
@Component
public class GitHubResponseMapper {

  /**
   * Um repositorio da resposta, no formato do dominio.
   *
   * <p><strong>O fuso e explicito, e a razao ja custou um mes uma vez.</strong> O {@code pushed_at}
   * chega como instante em UTC; converte-lo com o fuso da maquina que roda a aplicacao faria um
   * push das 21h de Sao Paulo virar o dia seguinte - e o desempate por data de push passaria a
   * depender de onde o servidor esta. O mesmo cuidado que a timeline tem ao formatar mes.
   */
  public RepositorySummary toSummary(GitHubRepositoryResponse response) {
    return new RepositorySummary(
        response.name(),
        response.description(),
        response.htmlUrl(),
        response.language(),
        response.stargazersCount(),
        response.pushedAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate());
  }

  /**
   * O mapa de linguagens somado de varios repositorios, no formato do dominio.
   *
   * <p>Entradas com zero bytes sao descartadas em vez de recusadas. O dominio exige bytes positivos
   * - zero nao e pouco uso, e ausencia -, e a soma de varios repositorios pode produzir o zero por
   * um caminho que o GitHub sozinho nao produz. Deixar a excecao subir faria uma linguagem
   * irrelevante derrubar o retrato inteiro.
   *
   * <p>A ordem nao e definida aqui. Quem ordena e {@code GitHubStats}, e repetir a decisao no
   * mapper criaria um segundo lugar decidindo a mesma coisa.
   */
  public List<LanguageUsage> toLanguages(Map<String, Long> bytesPorLinguagem) {
    return bytesPorLinguagem.entrySet().stream()
        .filter(entrada -> entrada.getValue() != null && entrada.getValue() > 0)
        .map(entrada -> new LanguageUsage(entrada.getKey(), entrada.getValue()))
        .toList();
  }
}
