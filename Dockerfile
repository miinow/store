# ------------ Build stage ------------
FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY gradlew /workspace/gradlew
COPY gradle /workspace/gradle
COPY build.gradle /workspace/build.gradle
COPY settings.gradle /workspace/settings.gradle
COPY gradle.properties /workspace/gradle.properties

RUN chmod +x ./gradlew || true

RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src /workspace/src

RUN ./gradlew --no-daemon clean bootJar

# ------------ Runtime stage ------------
FROM eclipse-temurin:17-jre

RUN useradd -ms /bin/bash appuser
USER appuser

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=25 -XX:+UseContainerSupport"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
