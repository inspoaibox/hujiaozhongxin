FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

RUN addgroup -S qianniu && adduser -S qianniu -G qianniu

WORKDIR /app

COPY target/ivr-service-*.jar app.jar

RUN mkdir -p /app/data /app/tmp && chown -R qianniu:qianniu /app/data /app/tmp

USER qianniu

EXPOSE 8084

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
