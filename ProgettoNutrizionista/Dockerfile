# syntax=docker/dockerfile:1

# ---- Stage 1: build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Stage 2: runtime (Chromium + tutte le dipendenze di sistema già preinstallate) ----
# Il tag DEVE combaciare esattamente con la versione di com.microsoft.playwright:playwright nel
# pom.xml: un mismatch fa scaricare/usare al driver Node un Chromium diverso da quello nell'immagine.
FROM mcr.microsoft.com/playwright/java:v1.61.0-noble
WORKDIR /app

# Font brand (Inter): installati a livello di sistema operativo, non più embeddati per-render
# via PDFBox come col vecchio motore OpenHTMLToPDF (vedi ChromiumPdfRenderer).
COPY --from=build /build/src/main/resources/fonts /usr/share/fonts/truetype/inter
RUN fc-cache -f

COPY --from=build /build/target/*.jar app.jar

# Utente non-root dedicato + directory upload (montate come volumi persistenti, vedi docker-compose.yml).
# uploads/fascicoli: FascicoloService usa un path relativo hardcoded, non configurabile via env var
# — funziona SOLO perché WORKDIR coincide col working directory della JVM (java -jar da /app).
RUN useradd -m -u 10001 appuser \
 && mkdir -p /app/uploads/loghi /app/uploads/fascicoli \
 && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
