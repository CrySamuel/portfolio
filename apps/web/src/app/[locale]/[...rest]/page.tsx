import { notFound } from 'next/navigation';

/**
 * Todo endereco sem rota, levado para dentro do layout do idioma.
 *
 * <p><strong>Sem este arquivo a 404 sai crua, e isso foi medido.</strong> Desde que o layout raiz e
 * o de {@code app/[locale]}, um endereco que nenhuma rota atende cai na 404 global do Next, que fica
 * fora do segmento - e portanto fora do layout: {@code /rota-inexistente} passou a responder sem
 * navbar, sem rodape e sem o link de pular para o conteudo. So uma rota dentro do segmento leva a
 * 404 para dentro dele, e o catch-all e essa rota: as rotas de verdade tem precedencia sobre ele, e
 * o que sobra chega aqui.
 *
 * <p>O endereco com ponto ({@code /foto.png}) fica de fora: o middleware nao passa por ele, entao
 * ele nao ganha o prefixo do idioma e casa com {@code [locale]} como se {@code foto.png} fosse um
 * idioma - e o {@code idiomaDaRota} do layout responde 404 antes de qualquer chamada a API.
 */
export default function RotaInexistente(): never {
  notFound();
}
