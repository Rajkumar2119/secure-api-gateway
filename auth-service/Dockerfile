# Build from the repository root:  docker build -f auth-service/Dockerfile -t auth-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY auth-service/pom.xml auth-service/pom.xml
COPY catalog-service/pom.xml catalog-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/src auth-service/src
RUN mvn -B -q -pl auth-service -am package -DskipTests \
 && cp auth-service/target/auth-service-1.0.0.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
