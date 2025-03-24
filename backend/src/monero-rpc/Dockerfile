FROM debian:bullseye-slim

RUN \
  apt update && \
  apt upgrade -y && \
  apt install curl bzip2 -y

RUN \
  cd /tmp && \
  curl -L -O https://downloads.getmonero.org/cli/linux64 && \
  curl -L -O https://www.getmonero.org/downloads/hashes.txt

RUN \
  grep -qe $(sha256sum /tmp/linux64) /tmp/hashes.txt

RUN \
  cd /tmp && \
  tar -xvf linux64 && \
  mv /tmp/monero-x86_64-linux-gnu-*/monero* /usr/local/bin && \
  rm -rf /tmp/*

RUN \
  adduser --system --group --disabled-password monero && \
  mkdir -p /home/monero/wallet && \
  chown -R monero:monero /usr/local/bin/monero* && \
  chown -R monero:monero /home/monero/wallet

WORKDIR /home/monero/wallet

COPY run.sh /run.sh
RUN chown monero:monero /run.sh && \
  chmod a+x /run.sh

ENTRYPOINT ["/run.sh"]
