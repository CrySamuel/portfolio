package dev.crystofer.portfolio.shared.web;

import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import dev.crystofer.portfolio.shared.domain.ContentLanguage;

/**
 * O idioma do conteudo pedido pelo {@code Accept-Language}, e a resposta que declara o entregue.
 *
 * <p>As duas metades moram juntas porque sao as duas pontas do mesmo contrato HTTP (ADR-0013): o
 * cliente diz o que prefere, o servidor diz o que mandou. Os controladores de conteudo usam as
 * duas, e espalha-las pelos cinco tornaria facil esquecer o {@code Vary} num deles.
 */
public final class LanguageNegotiation {

  /**
   * Como o cabecalho aparece no contrato publicado. Constante porque anotacao so aceita valor
   * conhecido em compilacao - e os cinco endpoints de conteudo publicam o mesmo texto.
   */
  public static final String DESCRICAO_DO_PEDIDO =
      "Idioma preferido do conteudo, no formato do Accept-Language (RFC 4647). Existem pt-BR e"
          + " en-US; en casa com en-US. Ausente, malformado ou sem idioma suportado: pt-BR.";

  /** Como o cabecalho de resposta aparece no contrato publicado. */
  public static final String DESCRICAO_DO_IDIOMA_ENTREGUE =
      "Idioma do conteudo entregue: pt-BR ou en-US. A resposta varia pelo Accept-Language.";

  private LanguageNegotiation() {}

  /**
   * O idioma mais preferido que existe, ou o padrao.
   *
   * <p>A leitura do cabecalho e a da RFC 4647, pela propria JDK: preferencias ordenadas por peso, e
   * {@code en} casando com {@code en-US}. Em cima dela, uma segunda passada so com o idioma de cada
   * preferencia - quem pede {@code en-GB} recebe {@code en-US}, e nao portugues. E o que alguem com
   * o navegador em ingles britanico espera, e a filtragem pura nao faz: para ela, {@code en-GB} e
   * {@code en-US} sao tags diferentes.
   *
   * <p><strong>Nunca recusa.</strong> Cabecalho ausente, malformado ou sem nenhum idioma suportado
   * devolve o padrao, e nao 406. O conteudo existe em portugues para qualquer um, e responder erro
   * a quem pediu frances seria pior do que responder em portugues - o que o {@code
   * Content-Language} deixa claro.
   */
  public static ContentLanguage negociar(String acceptLanguage) {
    if (acceptLanguage == null || acceptLanguage.isBlank()) {
      return ContentLanguage.padrao();
    }

    List<LanguageRange> preferencias;
    try {
      preferencias = LanguageRange.parse(acceptLanguage);
    } catch (IllegalArgumentException malformado) {
      return ContentLanguage.padrao();
    }

    var exato = Locale.filterTags(preferencias, ContentLanguage.tags());
    if (!exato.isEmpty()) {
      return idiomaDa(exato.get(0));
    }

    var soOIdioma =
        preferencias.stream()
            .map(
                preferencia ->
                    new LanguageRange(semRegiao(preferencia.getRange()), preferencia.getWeight()))
            .toList();
    var porIdioma = Locale.filterTags(soOIdioma, ContentLanguage.tags());
    return porIdioma.isEmpty() ? ContentLanguage.padrao() : idiomaDa(porIdioma.get(0));
  }

  /**
   * Uma resposta {@code 200} que declara o idioma do conteudo.
   *
   * <p><strong>O {@code Vary} e obrigatorio, e nao cortesia.</strong> As respostas de conteudo saem
   * com {@code Cache-Control: public}: sem ele, um cache compartilhado guardaria a primeira versao
   * que visse e a serviria a quem pedisse a outra. O {@code Content-Language} e o idioma negociado,
   * mesmo quando alguma linha caiu para o portugues por falta de traducao - o cabecalho descreve a
   * resposta, e nao cada campo dela.
   */
  public static <T> ResponseEntity<T> responder(
      ContentLanguage idioma, CacheControl cache, T corpo) {
    return ResponseEntity.ok()
        .cacheControl(cache)
        .header(HttpHeaders.CONTENT_LANGUAGE, idioma.tag())
        .varyBy(HttpHeaders.ACCEPT_LANGUAGE)
        .body(corpo);
  }

  private static String semRegiao(String faixa) {
    int hifen = faixa.indexOf('-');
    return hifen < 0 ? faixa : faixa.substring(0, hifen);
  }

  private static ContentLanguage idiomaDa(String tag) {
    return ContentLanguage.daTag(tag)
        .orElseThrow(() -> new IllegalStateException("Tag fora da lista suportada: " + tag));
  }
}
