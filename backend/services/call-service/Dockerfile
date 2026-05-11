# 千牛云呼叫中心系统 - call-service
# 多阶段构建：构建阶段已在 GitHub Actions 中完成，这里直接使用 JAR

FROM eclipse-temurin:17-jre-alpine

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 创建非 root 用户运行应用（安全最佳实践）
RUN addgroup -S qianniu && adduser -S qianniu -G qianniu

WORKDIR /app

# 复制 JAR 包（GitHub Actions 构建后的产物）
COPY target/call-service-*.jar app.jar

# H2 演示库和录音临时文件目录
RUN mkdir -p /app/data /recordings && chown -R qianniu:qianniu /app/data /recordings

USER qianniu

EXPOSE 8080

# JVM 参数：容器内存感知 + GC 优化
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
