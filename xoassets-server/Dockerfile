FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /app/target/xoassets-server-0.1.0.jar /app/xoassets-server.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/xoassets-server.jar"]
