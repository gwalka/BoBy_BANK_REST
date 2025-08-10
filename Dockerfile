# Образ OpenJDK 17
FROM openjdk:17-jdk-slim

# Копируем jar зцвнутрь контейнера
COPY build/libs/*.jar app.jar

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "/app.jar"]