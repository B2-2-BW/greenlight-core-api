# Base image로 OpenJDK 17 선택
FROM openjdk:17-jdk-alpine

RUN apk --no-cache add tzdata
ENV TZ=Asia/Seoul

# Set the working directory
WORKDIR /app

# Copy the Spring Boot application WAR file to the container
COPY ./build/libs/greenlight-prototype-core-api-0.0.1-SNAPSHOT.jar /app/greenlight-prototype-core-api.jar

# 애플리케이션 포트 열기 (필요에 따라 수정 가능)
EXPOSE 8080

# JAR 파일을 실행하도록 ENTRYPOINT 설정
ENTRYPOINT ["java", "-jar", "greenlight-prototype-core-api.jar"]