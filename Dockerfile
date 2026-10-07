# ---------- Etapa 1: build ----------
# Imagem com Maven + JDK 21 para compilar o projeto e gerar o app.jar
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /opt/app

COPY pom.xml .
COPY src ./src

# Os testes ja rodam no pipeline de CI; aqui so empacotamos
RUN mvn -B clean package -DskipTests

# ---------- Etapa 2: runtime ----------
# Imagem leve, apenas com o JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /opt/app

# Usuario sem privilegios (a aplicacao nao precisa rodar como root)
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /opt/app/target/app.jar app.jar
USER spring

# Porta padrao (pode ser alterada pela variavel SERVER_PORT)
ENV SERVER_PORT=8080
# A JVM respeita o limite de memoria do container
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080

# Container "saudavel" = /actuator/health respondendo 200
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO /dev/null http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
