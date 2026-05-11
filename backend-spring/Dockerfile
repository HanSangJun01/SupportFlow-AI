FROM eclipse-temurin:21-jdk

WORKDIR /workspace
COPY . .
RUN ./mvnw -q -DskipTests package

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/backend-spring-0.0.1-SNAPSHOT.jar"]
