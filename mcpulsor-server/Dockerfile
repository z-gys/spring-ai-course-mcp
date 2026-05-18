# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build

ARG MODULE_DIR=mcpulsor-server

WORKDIR /src
COPY . /src

# Build fat jar via shade plugin
RUN --mount=type=cache,target=/root/.m2 \
    set -eux; \
    mvn -q -DskipTests -f "${MODULE_DIR}/pom.xml" package; \
    mkdir -p /app; \
    cp "${MODULE_DIR}"/target/*-fat.jar /app/app.jar

# ---------- Runtime: STDIO + Inspector (one container) ----------
FROM eclipse-temurin:21-jre-alpine AS stdio
WORKDIR /app
RUN apk add --no-cache nodejs npm
COPY --from=build /app/app.jar /app/app.jar


EXPOSE 6274 6277
# Inspector launches your server (fat jar has Main-Class set)
CMD sh -c 'npx @modelcontextprotocol/inspector -e MCP_TRANSPORT=$MCP_TRANSPORT -- java -jar /app/app.jar'

# ---------- Runtime: HTTP server only (inspector separate) ----------
FROM eclipse-temurin:21-jre-alpine AS http
WORKDIR /app
COPY --from=build /app/app.jar /app/app.jar

EXPOSE 8090
CMD ["sh", "-c", "java -jar /app/app.jar"]
