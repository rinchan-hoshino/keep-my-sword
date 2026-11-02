FROM eclipse-temurin:21-jdk-jammy

WORKDIR /opt/mod

RUN apt-get update \
  && apt-get install -y --no-install-recommends git bash coreutils findutils ca-certificates \
  && rm -rf /var/lib/apt/lists/*

COPY .ci/local-ci/run-checks.sh /usr/local/bin/minecraft-mod-local-ci-runner
RUN chmod +x /usr/local/bin/minecraft-mod-local-ci-runner

ENTRYPOINT ["/usr/local/bin/minecraft-mod-local-ci-runner"]
