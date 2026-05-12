FROM eclipse-temurin:21-jdk-jammy

ARG RINLIB_REF=main
WORKDIR /opt/mod

RUN apt-get update \
  && apt-get install -y --no-install-recommends git bash coreutils findutils ca-certificates \
  && rm -rf /var/lib/apt/lists/*

RUN git clone --depth 1 --branch "$RINLIB_REF" https://github.com/rinchanai/rinlib.git /opt/rinlib \
  || git clone --depth 1 --branch main https://github.com/rinchanai/rinlib.git /opt/rinlib
RUN cd /opt/rinlib && ./gradlew --no-daemon publishToMavenLocal

COPY .ci/local-ci/run-checks.sh /usr/local/bin/minecraft-mod-local-ci-runner
RUN chmod +x /usr/local/bin/minecraft-mod-local-ci-runner

ENTRYPOINT ["/usr/local/bin/minecraft-mod-local-ci-runner"]
