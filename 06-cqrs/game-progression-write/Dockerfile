FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY game-progression-contracts/pom.xml ./game-progression-contracts/pom.xml
COPY game-progression-contracts/src ./game-progression-contracts/src
COPY game-progression-write/pom.xml ./game-progression-write/pom.xml
COPY game-progression-write/src ./game-progression-write/src

RUN mvn -N install -DskipTests
RUN mvn -f game-progression-contracts/pom.xml install -DskipTests
RUN mvn -f game-progression-write/pom.xml package -DskipTests

FROM eclipse-temurin:25-jre

ARG DOCKERIZE_VERSION=v0.13.0
ARG TARGETARCH

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates wget \
    && case "${TARGETARCH}" in \
        amd64) dockerize_arch=amd64 ;; \
        arm64) dockerize_arch=arm64 ;; \
        *) echo "Unsupported architecture: ${TARGETARCH}" && exit 1 ;; \
      esac \
    && wget -qO- "https://github.com/jwilder/dockerize/releases/download/${DOCKERIZE_VERSION}/dockerize-linux-${dockerize_arch}-${DOCKERIZE_VERSION}.tar.gz" \
      | tar -C /usr/local/bin -xz \
    && apt-get purge -y --auto-remove wget \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /workspace/game-progression-write/target/game-progression-write-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["dockerize", "-wait", "tcp://postgres-write:5432", "-wait", "tcp://kafka:9092", "-timeout", "120s", "java", "-jar", "/app/app.jar"]
