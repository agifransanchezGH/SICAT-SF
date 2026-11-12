# Imagen de una API. Uso: docker build --build-arg MODULE=sicat-admin-api -t sicat-admin-api backend/
ARG MODULE=sicat-admin-api

FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /src
# Primero los POM para cachear dependencias
COPY pom.xml .
COPY sicat-core/pom.xml sicat-core/
COPY sicat-admin-api/pom.xml sicat-admin-api/
COPY sicat-public-api/pom.xml sicat-public-api/
RUN mvn -B -q -pl ${MODULE} -am dependency:go-offline || true
COPY . .
RUN mvn -B -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
RUN useradd --system --uid 10001 sicat
COPY --from=build /src/${MODULE}/target/${MODULE}.jar app.jar
USER sicat
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
