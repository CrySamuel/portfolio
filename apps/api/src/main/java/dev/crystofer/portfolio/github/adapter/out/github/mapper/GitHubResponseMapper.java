package dev.crystofer.portfolio.github.adapter.out.github.mapper;

import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

import dev.crystofer.portfolio.github.adapter.out.github.dto.GitHubRepositoryResponse;
import dev.crystofer.portfolio.github.domain.model.RepositorySummary;

/**
 * Traduz a resposta do GitHub para o dominio.
 *
 * <p><strong>Escrito a mao, e nao gerado por MapStruct como os outros mappers do projeto.</strong>
 * O MapStruct ganha onde os dois lados tem a mesma forma e o valor esta em o compilador reprovar
 * campo sem origem - e o caso de entidade JPA para dominio. Aqui as formas nao se correspondem: o
 * instante do push vira data em fuso explicito, e metade dos campos da resposta existe so para ser
 * filtrada.
 *
 * <p>As linguagens nao passam por aqui. Combinar os mapas de varios repositorios e regra de negocio
 * - cada projeto pesa igual -, e ela vive em {@code LanguageUsage.averagingByRepository}.
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
}
