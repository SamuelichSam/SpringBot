FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Копируем исходники
COPY pom.xml .
COPY src ./src

# Устанавливаем Maven, собираем проект и ОСТАВЛЯЕМ JDK
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests && \
    apk del maven

# Теперь JDK остался (eclipse-temurin:21-jdk-alpine включает JDK)
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/SpringBot-0.0.1-SNAPSHOT.jar"]