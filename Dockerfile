FROM eclipse-temurin:25.0.1_8-jre-alpine

RUN apk --no-cache add tzdata
ENV TZ=Asia/Seoul

WORKDIR /app

COPY ./build/libs/greenlight-core-api-0.0.1-SNAPSHOT.jar /app/greenlight-core-api.jar

EXPOSE 18080 18090

ENTRYPOINT ["java", "-jar", "greenlight-core-api.jar"]