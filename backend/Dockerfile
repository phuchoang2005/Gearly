# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy file cấu hình Maven
COPY pom.xml .

# Tải dependencies trước để cache layer
RUN mvn dependency:go-offline

# Copy toàn bộ source code
COPY src ./src

# Build ứng dụng
RUN mvn clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy file JAR từ stage build
COPY --from=builder /app/target/*.jar app.jar

# Copy thư mục uploads (ảnh, dữ liệu tĩnh)
COPY uploads /app/uploads

# Expose port 8080 (thay đổi nếu app bạn dùng port khác)
EXPOSE 8080

# Non-secret runtime defaults. Secrets (JWT_SECRET, MAIL_*, MOMO_*,
# GOOGLE_CLIENT_ID, GITHUB_MODELS_*) are NOT baked into the image — they are
# injected at run time via docker-compose `environment:` (from backend/.env).
ENV SPRING_DATA_MONGODB_URI=mongodb://mongodb:27017/bookify
ENV SPRING_PROFILES_ACTIVE=docker

# Chạy app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
