# syntax=docker/dockerfile:1

# ---------- Etapa de build ----------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Se copia primero el wrapper y el pom para cachear las dependencias en una
# capa separada: mientras no cambien, "docker build" no vuelve a bajarlas.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ---------- Etapa de runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

# Tuning para el free tier de Render (512MB de RAM total, compartida con el
# resto del contenedor): SerialGC gasta menos memoria nativa que el G1
# default en instancias chicas de un solo core, y se cappea el heap y el
# metaspace para no dejar que el JVM reclame de más y el proceso muera por
# OOM del host en vez de por un OutOfMemoryError manejable.
ENV JAVA_OPTS="-XX:+UseSerialGC -XX:MaxRAMPercentage=70 -XX:MaxMetaspaceSize=180m -Xss512k -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# Sin HEALTHCHECK propio: Render no lo usa para enrutar/reiniciar (tiene su
# propio chequeo HTTP configurable desde el dashboard, apuntando a
# /actuator/health). Se probó acá y se descartó porque ese endpoint agrega
# TODOS los indicators —incluido el SMTP externo (mail)— así que un hipo
# de Brevo tira el status a DOWN aunque el resto de la app funcione bien;
# como Docker HEALTHCHECK no aporta nada real en este despliegue, mejor no
# generar falsos "unhealthy".

# Render inyecta PORT en runtime (free tier no siempre es 8080); si no está
# seteada (ej. build/run local), cae a 8080 por consistencia con el resto
# del proyecto (docker-compose, README, application.properties).
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
