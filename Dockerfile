# Stage 1: Build ứng dụng bằng Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy file cấu hình pom.xml và tải dependencies (tận dụng cache của Docker)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy toàn bộ mã nguồn và build ra file jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Chạy ứng dụng bằng JRE nhẹ
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy file jar từ Stage 1 sang Stage 2
# Lưu ý: Đảm bảo tên file .jar trong thư mục target khớp với tên project của bạn
COPY --from=build /app/target/*.jar app.jar

# Render sẽ cung cấp cổng qua biến môi trường PORT, mặc định là 10000
EXPOSE 8080

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]