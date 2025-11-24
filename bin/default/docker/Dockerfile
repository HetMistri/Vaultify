FROM gradle:8.6-jdk21 AS builder

WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .

RUN gradle clean build -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /home/gradle/project/build/libs/*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

