# ONE Dockerfile for all services. The build argument MODULE chooses the service:
#   docker build --build-arg MODULE=movie-service -t cinema/movie-service .
# (run it from the microservices/ folder)

# ---- stage 1: build the jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /build
COPY . .
# -pl = only this module, -am = and the modules it needs (platform-starter)
RUN mvn -B -q -pl ${MODULE} -am package -DskipTests

# ---- stage 2: a small image that only runs the jar ----
FROM eclipse-temurin:21-jre-alpine
ARG MODULE
# run as a normal user, not as root (a production habit)
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /build/${MODULE}/target/${MODULE}-0.0.1-SNAPSHOT.jar app.jar
USER app
# use at most 75% of the container memory for the Java heap
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
