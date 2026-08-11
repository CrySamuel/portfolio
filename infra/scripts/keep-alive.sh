#!/usr/bin/env bash
#
# Mantem a API acordada durante a janela em que alguem realmente abriria o site.
#
# O plano gratuito do Render hiberna o servico depois de 15 minutos sem trafego,
# e traz de volta em cerca de um minuto. Desde que a home passou a ser
# pre-renderizada (ISR), o visitante nunca espera por isso - a CDN entrega o
# HTML pronto. Quem ainda sente o cold start e quem abre o Swagger, que a
# Definition of Done do MVP 1 exige publico: um avaliador clicando em
# /swagger-ui esperaria o minuto inteiro.
#
# Por que uma janela, e nao o dia todo: o plano da 750 horas de instancia por
# mes e o mes tem ~730. Manter a API acordada 24/7 consumiria 97% da cota, e
# qualquer segundo servico no workspace estouraria o limite - com suspensao, nao
# com cobranca. Sedezesseis horas por dia dao ~487 horas, 65% da cota, e cobrem
# o horario comercial brasileiro com folga.
#
# Uso:
#   ./keep-alive.sh https://portfolio-api.onrender.com

set -euo pipefail

BASE_URL="${1:-}"

if [ -z "$BASE_URL" ]; then
  echo "erro: informe a URL base. Uso: $0 <url-base>" >&2
  exit 2
fi

# Liveness, e nao /actuator/health. O health completo consulta o banco, e este
# script roda a cada dez minutos: usar o caminho errado manteria o Neon acordado
# junto e gastaria as horas de compute dele - trocando um problema de cota por
# outro.
ENDPOINT="${BASE_URL%/}/actuator/health/liveness"

# --max-time generoso porque a primeira chamada depois da hibernacao e
# justamente a que espera o servico subir. Falhar aqui por impaciencia
# transformaria o keep-alive numa fonte de alarme falso.
RESPOSTA=$(curl --silent --show-error --location \
  --retry 3 --retry-delay 5 --retry-connrefused \
  --max-time 120 \
  --write-out '\n%{http_code}' \
  "$ENDPOINT")

CORPO=$(echo "$RESPOSTA" | sed '$d')
STATUS=$(echo "$RESPOSTA" | tail -n1)

if [ "$STATUS" != "200" ]; then
  echo "erro: $ENDPOINT respondeu $STATUS" >&2
  echo "$CORPO" >&2
  exit 1
fi

# O corpo e conferido, e nao so o status: um proxy ou pagina de erro da
# plataforma tambem devolve 200, e um keep-alive que aceita qualquer 200 pode
# ficar meses batendo numa pagina de manutencao sem ninguem perceber.
case "$CORPO" in
  *'"status":"UP"'*)
    echo "ok: API acordada ($ENDPOINT)"
    ;;
  *)
    echo "erro: resposta inesperada de $ENDPOINT" >&2
    echo "$CORPO" >&2
    exit 1
    ;;
esac
