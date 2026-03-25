FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /workspace/target/*.jar app.jar

ENV APP_NAME=${APP_NAME:-app}
EXPOSE 8080
USER appuser

ENTRYPOINT ["java","-jar","app.jar"]
