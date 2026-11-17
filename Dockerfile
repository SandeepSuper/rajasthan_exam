# Container image for hosting the backend on Render (or any Docker host).
# Build stage: compile the Spring Boot jar with the project's Gradle wrapper.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon -q dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon -q bootJar -x test

# Run stage: small JRE image with just the jar.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar

# Render's free plan has 512 MB RAM; keep the heap well under that.
ENV JAVA_OPTS="-Xmx320m -Xss512k -XX:+UseSerialGC -XX:MaxMetaspaceSize=160m"
# Render tells the app which port to listen on through $PORT.
EXPOSE 10000
CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-10000} -jar app.jar"]
