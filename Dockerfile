FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app
COPY --from=builder --chown=10001:10001 /app/target/shrek-back-0.0.1-SNAPSHOT.jar ./shrek-back.jar
COPY --chown=10001:10001 ShippingCountries.csv EMS.csv EMS1.csv ECO.csv ./
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "shrek-back.jar"]
