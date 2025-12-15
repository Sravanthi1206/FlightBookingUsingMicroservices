# ---------- BUILD STAGE ----------
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY flightappcommon flightappcommon
COPY configserver configserver
COPY eureka-server eureka-server
COPY authservice authservice
COPY flightservice flightservice
COPY bookingservice bookingservice
COPY emailservice emailservice
COPY apigateway apigateway

RUN mvn -B -DskipTests clean install

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

ARG SERVICE_NAME
COPY --from=build /workspace/${SERVICE_NAME}/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
