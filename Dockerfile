#Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

RUN --mount=type=secret,id=corporate-ca \
    if [ -f /run/secrets/corporate-ca ]; then \
        apk add --no-cache ca-certificates && \
        cp /run/secrets/corporate-ca /usr/local/share/ca-certificates/corporate-ca.crt && \
        update-ca-certificates && \
        keytool -importcert -noprompt -trustcacerts \
            -alias corporate-ca \
            -file /run/secrets/corporate-ca \
            -cacerts -storepass changeit; \
    fi

# Copy only what's needed to resolve dependencies first, so this layer
# is cached and skipped on rebuilds where only source code changed.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Now copy the actual source and build the real jar.
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

#Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Create a dedicated, unprivileged user instead of running as root.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
