# Runtime Dockerfile for Spring Boot (expects a pre-built jar in target/)
######### Build stage #########
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy only the files needed for a Maven build, then run a package to produce the jar
COPY pom.xml ./
COPY src ./src
# skip compiling/running tests inside the Docker build (reduces network/test dependency issues)
RUN mvn -B -Dmaven.test.skip=true package

######### Runtime stage #########
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the fat jar produced by the build stage
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
