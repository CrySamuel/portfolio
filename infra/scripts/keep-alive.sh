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
# Por que um laco, e nao um ping por execucao: **o GitHub nao honra o cron**. O
# arquivo do workflow pede dez minutos e a entrega medida fica entre 42 e 57 -
# uma ordem de grandeza acima da hibernacao de quinze. Com um ping por execucao,
# o servico dorme entre uma passagem e a outra e o keep-alive nao cumpre o que
# promete. Repositorio publico tem minutos de Actions ilimitados, entao a saida
# barata e uma execucao que fica de pe pingando, em vez de muitas execucoes que
# chegam quando querem.
#
# A janela continua sendo respeitada dentro do laco: ele para sozinho ao sair
# dela, para nao transformar o conserto de cadencia num estouro de cota.
#
# Uso:
#   ./keep-alive.sh <url-base>              # um ping so
#   ./keep-alive.sh <url-base> 55           # pinga por 55 minutos

set -euo pipefail

BASE_URL="${1:-}"

if [ -z "$BASE_URL" ]; then
  echo "erro: informe a URL base. Uso: $0 <url-base>" >&2
  exit 2
fi

# Liveness, e nao /actuator/health. O health completo consulta o banco, e este
# script pinga a cada cinco minutos durante quase uma hora - usar o caminho
# errado manteria o Neon acordado junto e gastaria as horas de compute dele,
# trocando um problema de cota por outro. Com o laco, o caminho certo importa
# mais do que importava com um ping por execucao.
ENDPOINT="${BASE_URL%/}/actuator/health/liveness"

# Duracao do laco em minutos. Zero - o padrao - mantem o comportamento antigo de
# um ping so, que e o que `workflow_dispatch` manual costuma querer.
DURACAO_MIN="${2:-0}"

# Cinco minutos contra os quinze da hibernacao: tres chances de acertar antes de
# o servico dormir, o que absorve uma falha de rede sem abrir janela.
#
# Sobrescrever pela variavel de ambiente serve para **exercitar o laco**: com o
# padrao, conferir que ele repete custa cinco minutos de espera, e verificacao
# que custa isso e verificacao que ninguem faz. O workflow nunca a define.
INTERVALO_S="${KEEP_ALIVE_INTERVALO_S:-300}"

# A janela e de 10:00 as 01:59 UTC, a mesma do cron do workflow. Repetida aqui
# porque o laco sobrevive ao instante em que foi disparado: sem esta checagem,
# uma execucao iniciada as 01:55 pingaria ate as 02:50, fora da faixa que a conta
# de cota assume.
dentro_da_janela() {
  local hora
  hora=$((10#$(date -u +%H)))
  [ "$hora" -ge 10 ] || [ "$hora" -le 1 ]
}

# --max-time generoso porque a primeira chamada depois da hibernacao e
# justamente a que espera o servico subir. Falhar aqui por impaciencia
# transformaria o keep-alive numa fonte de alarme falso.
pingar() {
  local RESPOSTA CORPO STATUS

  RESPOSTA=$(curl --silent --show-error --location     --retry 3 --retry-delay 5 --retry-connrefused     --max-time 120     --write-out '
%{http_code}'     "$ENDPOINT") || return 1

  CORPO=$(echo "$RESPOSTA" | sed '$d')
  STATUS=$(echo "$RESPOSTA" | tail -n1)

  if [ "$STATUS" != "200" ]; then
    echo "erro: $ENDPOINT respondeu $STATUS" >&2
    echo "$CORPO" >&2
    return 1
  fi

  # O corpo e conferido, e nao so o status: um proxy ou pagina de erro da
  # plataforma tambem devolve 200, e um keep-alive que aceita qualquer 200 pode
  # ficar meses batendo numa pagina de manutencao sem ninguem perceber.
  case "$CORPO" in
    *'"status":"UP"'*)
      echo "ok: API acordada em $(date -u +%H:%M:%S) UTC"
      ;;
    *)
      echo "erro: resposta inesperada de $ENDPOINT" >&2
      echo "$CORPO" >&2
      return 1
      ;;
  esac
}

# O primeiro ping e o unico que pode reprovar a execucao. Se a API nao sobe nem
# na primeira tentativa, isso e sinal de problema e merece ficar vermelho no
# historico; falha de um ping no meio do laco, nao - ver abaixo.
pingar

# Um ping so, que e o comportamento historico e o que o disparo manual espera.
if [ "$DURACAO_MIN" -eq 0 ]; then
  exit 0
fi

FIM=$(( $(date +%s) + DURACAO_MIN * 60 ))

while :; do
  # O tempo restante e conferido **antes** de dormir, e nao depois. Na ordem
  # inversa, uma duracao menor que o intervalo ainda dormiria o intervalo
  # inteiro - o laco terminaria depois da hora combinada, e exercita-lo custaria
  # cinco minutos.
  RESTANTE=$(( FIM - $(date +%s) ))

  if [ "$RESTANTE" -lt "$INTERVALO_S" ]; then
    echo "fim: laco de $DURACAO_MIN minutos concluido"
    exit 0
  fi

  if ! dentro_da_janela; then
    echo "fim: fora da janela de 10:00-01:59 UTC, encerrando o laco"
    exit 0
  fi

  sleep "$INTERVALO_S"

  # Falha de um ping no meio do laco nao derruba a execucao. Uma instabilidade
  # de trinta segundos na plataforma nao e motivo para deixar o servico dormir os
  # proximos quarenta minutos - que e exatamente o que acontecia quando cada
  # execucao tinha uma tentativa unica.
  pingar || echo "aviso: ping falhou, seguindo o laco" >&2
done
