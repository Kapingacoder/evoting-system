FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY pom.xml .
COPY mvnw* .
COPY .mvn .mvn
COPY src ./src

RUN ./mvnw clean package -DskipTests

COPY target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]