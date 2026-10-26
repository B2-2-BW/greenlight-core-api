FROM amazoncorretto:25.0.5-al2023-headless

RUN dnf install -y tzdata && dnf clean all

ENV TZ=Asia/Seoul

WORKDIR /app

COPY ./build/libs/greenlight-core-api-0.0.1-SNAPSHOT.jar /app/greenlight-core-api.jar

EXPOSE 18080 18090

ENTRYPOINT ["java", "-jar", "greenlight-core-api.jar"]
