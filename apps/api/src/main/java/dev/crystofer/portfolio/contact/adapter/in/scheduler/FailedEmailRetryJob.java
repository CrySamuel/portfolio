package dev.crystofer.portfolio.contact.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.crystofer.portfolio.contact.domain.port.in.RetryFailedEmailsUseCase;

/**
 * Tenta de novo o que ficou por entregar.
 *
 * <p><strong>E a metade que transforma "a mensagem esta gravada" em "a mensagem chega".</strong>
 * Sem ele, uma queda do provedor deixaria a linha em {@code FAILED} para sempre: tecnicamente nao
 * perdida, praticamente invisivel - e "nenhuma mensagem se perde" viraria uma promessa sobre o
 * banco, e nao sobre a caixa de entrada.
 *
 * <p><strong>Intervalo fixo, e nao expressao cron.</strong> {@code fixedDelay} conta a partir do
 * fim da execucao anterior, entao duas passagens nunca se sobrepoem - com {@code cron}, uma
 * passagem lenta por causa de vinte tentativas contra um provedor fora do ar encontraria a seguinte
 * ja comecando, e as duas reenviariam as mesmas mensagens.
 *
 * <p>Cinco minutos e o intervalo, e ele e um compromisso explicito: uma queda curta do provedor se
 * resolve sozinha em minutos, sem ninguem perceber; uma longa drena vinte mensagens por passagem, o
 * que da 240 por hora - mais do que este portfolio recebe num ano.
 *
 * <p>O primeiro reprocessamento espera dois minutos depois do boot, pela mesma razao do
 * reaquecimento do GitHub: o servico do plano gratuito hiberna e volta com frequencia, e disputar a
 * subida com o resto da inicializacao atrasa o que a plataforma cronometra.
 */
@Component
class FailedEmailRetryJob {

  private final RetryFailedEmailsUseCase reprocessamento;

  FailedEmailRetryJob(RetryFailedEmailsUseCase reprocessamento) {
    this.reprocessamento = reprocessamento;
  }

  @Scheduled(initialDelay = 120_000, fixedDelay = 5 * 60 * 1_000)
  void reprocessar() {
    reprocessamento.retryFailed();
  }
}
