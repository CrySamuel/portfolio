package dev.crystofer.portfolio.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Liga o agendador da aplicacao.
 *
 * <p>Sem isto, {@code @Scheduled} e um comentario: o metodo existe, a anotacao esta la, e nada
 * nunca o chama. Nao ha erro, nao ha aviso - e o modo de falha que a secao 4.1 do estado do projeto
 * cataloga, a guarda muda. O unico sintoma seria o cache do GitHub expirando sem ninguem
 * reaquece-lo, seis horas depois, na tela de um visitante.
 *
 * <p>Fica em classe propria e nao na {@code PortfolioApplication} porque a classe de bootstrap e o
 * lugar onde ninguem procura configuracao - e porque assim um teste pode excluir esta configuracao
 * sem excluir a aplicacao inteira.
 *
 * <p>As tarefas rodam em thread virtual, como o resto: {@code spring.threads.virtual.enabled} vale
 * tambem para o agendador, entao uma tarefa que bloqueia esperando o GitHub nao prende thread de
 * plataforma.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
