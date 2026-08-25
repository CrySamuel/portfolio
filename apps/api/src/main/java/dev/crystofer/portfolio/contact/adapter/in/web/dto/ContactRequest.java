package dev.crystofer.portfolio.contact.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * O corpo de {@code POST /api/v1/contact}.
 *
 * <p><strong>Primeiro DTO de entrada desta API.</strong> Os outros descrevem o que sai; este
 * descreve o que um desconhecido manda, e por isso cada campo carrega restricao. As anotacoes aqui
 * repetem os invariantes do dominio de proposito: a diferenca esta em <em>quando</em> e
 * <em>como</em> cada camada recusa. Aqui a recusa vira 400 com o nome do campo, antes de qualquer
 * regra de negocio rodar; no dominio ela vira excecao, que o visitante nunca deveria ver.
 *
 * <p><strong>{@code @Email} e deliberadamente frouxo, e a validacao boa vem depois.</strong> O
 * validador padrao aceita coisas que nenhum provedor entrega. Quem recusa de verdade e o {@code
 * EmailAddress} do dominio - o que esta aqui existe para que o erro comum de digitacao volte como
 * 400 com o campo apontado, em vez de como erro interno.
 *
 * <p><strong>O honeypot nao e validado, e nao poderia ser.</strong> Ele e um campo que humano nao
 * ve e nao preenche; robo preenche. Uma restricao {@code @Null} aqui devolveria 400 e ensinaria ao
 * robo exatamente qual campo evitar - por isso o controlador o trata em silencio, com resposta de
 * sucesso, e a mensagem simplesmente nao entra.
 *
 * @param name quem escreveu
 * @param email para onde a resposta vai
 * @param subject assunto declarado
 * @param message o texto
 * @param website o honeypot; preenchido significa robo
 */
@Schema(description = "Mensagem enviada pelo formulario de contato")
public record ContactRequest(
    @Schema(example = "Fulana de Tal")
        @NotBlank(message = "O nome e obrigatorio") @Size(max = 120, message = "O nome deve ter no maximo 120 caracteres") String name,
    @Schema(example = "fulana@exemplo.com")
        @NotBlank(message = "O e-mail e obrigatorio") @Email(message = "O e-mail tem formato invalido") @Size(max = 254, message = "O e-mail deve ter no maximo 254 caracteres") String email,
    @Schema(example = "Vaga de backend")
        @NotBlank(message = "O assunto e obrigatorio") @Size(max = 150, message = "O assunto deve ter no maximo 150 caracteres") String subject,
    @Schema(example = "Vi o portfolio e gostaria de conversar.")
        @NotBlank(message = "A mensagem e obrigatoria") @Size(max = 5000, message = "A mensagem deve ter no maximo 5000 caracteres") String message,
    @Schema(
            description =
                "Campo-armadilha, invisivel no formulario. Deve chegar vazio; preenchido, a"
                    + " mensagem e descartada em silencio.",
            example = "")
        String website) {}
