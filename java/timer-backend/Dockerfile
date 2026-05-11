FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl wget \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/timer-backend-*.jar /app/timer-backend.jar
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=20 \
  CMD curl -sf http://127.0.0.1:8080/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java","-jar","/app/timer-backend.jar"]
