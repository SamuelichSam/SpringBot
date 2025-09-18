FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Копируем исходники
COPY pom.xml .
COPY src ./src

# Устанавливаем Maven и собираем проект
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests && \
    apk del maven

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/SpringBot-0.0.1-SNAPSHOT.jar"]