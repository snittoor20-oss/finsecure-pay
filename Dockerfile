FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S finsecure && adduser -S finsecure -G finsecure
COPY --chown=finsecure:finsecure target/pay-0.0.1-SNAPSHOT.jar app.jar
USER finsecure
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]