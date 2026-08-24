package dev.crystofer.portfolio.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Liga a execucao assincrona da aplicacao.
 *
 * <p>Sem isto, {@code @Async} e um comentario: o metodo existe, a anotacao esta la, e ele roda na
 * thread de quem chamou. Nao ha erro nem aviso - o mesmo modo de falha que o {@code @Scheduled} sem
 * {@code @EnableScheduling} teria, e que a {@link SchedulingConfig} existe para evitar.
 *
 * <p><strong>Aqui o {@code @Async} nao e otimizacao de latencia - e correcao, e isso foi
 * medido.</strong> Removido {@code @EnableAsync}, tres dos quatro testes de entrega falham com
 * {@code TransactionRequiredException: No active transaction for update or delete query}.
 *
 * <p>O motivo e a fase em que o ouvinte roda. Em {@code AFTER_COMMIT}, na thread da requisicao, a
 * sincronizacao de transacao ainda esta ativa mas a transacao esta <em>encerrando</em>: o
 * {@code @Transactional} do caso de uso nao abre uma nova, tenta juntar-se a que esta acabando, e o
 * {@code UPDATE} do desfecho nao encontra transacao nenhuma. Com {@code @Async}, a entrega vai para
 * outra thread, onde nao ha transacao alguma a que se juntar - e ai ela ganha a propria.
 *
 * <p>A previsao escrita antes do experimento era que a suite continuaria verde e o unico efeito
 * seria o visitante esperar o provedor responder. <strong>Estava errada</strong>, e o registro fica
 * porque a intuicao "assincrono e so desempenho" e comum e custa caro.
 *
 * <p>As tarefas rodam em thread virtual, como o resto - {@code spring.threads.virtual.enabled} vale
 * tambem para o executor do {@code @Async}, entao uma chamada bloqueada esperando o provedor nao
 * prende thread de plataforma.
 */
@Configuration
@EnableAsync
public class AsyncConfig {}
