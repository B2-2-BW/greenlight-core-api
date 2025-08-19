FROM alpine/java:17.0.12

RUN apk --no-cache add tzdata
ENV TZ=Asia/Seoul

WORKDIR /app

COPY ./build/libs/greenlight-prototype-core-api-0.0.1-SNAPSHOT.jar /app/greenlight-prototype-core-api.jar

EXPOSE 18080 18090

ENTRYPOINT ["java", "-jar", "greenlight-prototype-core-api.jar"]