FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy pom first to leverage Docker layer cache for dependencies.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN useradd --system --uid 1001 spring && \
	mkdir -p /app/uploads && \
	chown -R spring:spring /app

COPY --from=builder /app/target/*.jar /app/app.jar

EXPOSE 8080
USER spring

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
