# Etapa de build
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar apenas o pom.xml para cachear as dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar o restante do código e construir o projeto (pulando testes)
COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa de runtime
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copiar o JAR gerado
COPY --from=build /app/target/*.jar app.jar

# Definir variáveis de ambiente padrão
ENV FIREBASE_SERVICE_ACCOUNT_PATH=/etc/secrets/serviceAccountKey.json \
    PORT=8080

# Expor a porta
EXPOSE 8080

# Comando de inicialização
ENTRYPOINT ["java", "-jar", "app.jar"]
