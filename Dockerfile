# Convenience alias: `docker build .` produces the chat image.
# Prefer the module Dockerfiles in CI:
#   docker build -f chat/Dockerfile
#   docker build -f indexer/Dockerfile
#   docker build -f mcp/Dockerfile
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./
COPY core/pom.xml core/
COPY chat/pom.xml chat/
COPY indexer/pom.xml indexer/
COPY mcp/pom.xml mcp/
RUN mvn -q -B -pl chat -am dependency:go-offline -DskipTests

COPY core/src core/src
COPY chat/src chat/src
RUN mvn -q -B -pl chat -am package -DskipTests

RUN cp chat/target/chat-*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre-noble AS runtime

RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --create-home --uid 10001 --shell /usr/sbin/nologin acme

USER 10001
WORKDIR /app

COPY --from=builder --chown=10001:10001 /build/extracted/dependencies/ ./
COPY --from=builder --chown=10001:10001 /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=10001:10001 /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=10001:10001 /build/extracted/application/ ./

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
