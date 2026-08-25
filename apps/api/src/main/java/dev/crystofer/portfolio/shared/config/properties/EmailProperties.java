package dev.crystofer.portfolio.shared.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Tudo o que o envio de e-mail le de configuracao.
 *
 * <p>Record com vinculo por construtor, como {@link GitHubProperties} e pela mesma razao:
 * configuracao errada precisa aparecer no deploy, com o nome da variavel, e nao numa requisicao
 * qualquer horas depois.
 *
 * <p><strong>A chave e opcional, e a ausencia dela troca o adaptador.</strong> Sem chave, o envio e
 * escrito no log e a aplicacao sobe normalmente - e o que permite desenvolver e rodar a suite sem
 * segredo nenhum na maquina. E a mesma decisao do token do GitHub, com uma diferenca importante: la
 * a ausencia degrada um numero, aqui ela <em>nao entrega e-mail nenhum</em>. Por isso o adaptador
 * de log grita em nivel de aviso.
 *
 * <p><strong>O destinatario tem default porque e conteudo, e nao segredo.</strong> O e-mail do dono
 * ja consta do {@code package.json} e da autoria de todos os commits deste repositorio - o mesmo
 * criterio que deu default ao {@code GITHUB_USERNAME}. O que nunca teria default e a chave.
 *
 * @param apiKey credencial do provedor; vazio significa "escreva no log em vez de enviar"
 * @param recipient para quem a notificacao vai
 * @param sender remetente declarado; precisa pertencer a um dominio verificado no provedor
 * @param baseUrl raiz da API do provedor, parametrizada para o dublê dos testes
 * @param connectTimeout espera pela conexao
 * @param readTimeout espera pela resposta
 */
@ConfigurationProperties(prefix = "portfolio.email")
public record EmailProperties(
    @DefaultValue("") String apiKey,
    @DefaultValue("crystoferdemetino@gmail.com") String recipient,
    @DefaultValue("onboarding@resend.dev") String sender,
    @DefaultValue("https://api.resend.com") String baseUrl,
    @DefaultValue("2s") Duration connectTimeout,
    @DefaultValue("10s") Duration readTimeout) {

  /** Ha credencial para falar com o provedor? */
  public boolean configured() {
    return !apiKey.isBlank();
  }
}
