FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /app/target/timer-backend-*.jar /app/timer-backend.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/timer-backend.jar"]
