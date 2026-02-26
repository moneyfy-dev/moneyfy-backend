FROM maven:3.9.9-eclipse-temurin-21

WORKDIR /app

# Copiamos solo el pom.xml para cachear dependencias
COPY pom.xml .
RUN mvn dependency:go-offline

# Montaremos el código en tiempo de ejecución, no lo copiamos aquí
CMD ["mvn", "spring-boot:run"]