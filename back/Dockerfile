FROM eclipse-temurin:25-jre-noble
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring \
    && mkdir -p /app/uploads && chown -R spring:spring /app/uploads

USER spring

COPY app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
