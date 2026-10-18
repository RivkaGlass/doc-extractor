# מכיל את בניית הקונטיינר של היישום. 
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src ./src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8081
USER spring

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
