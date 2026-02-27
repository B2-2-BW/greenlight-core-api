FROM eclipse-temurin:25.0.2_10-jre-noble

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends tzdata && \
    rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Seoul

WORKDIR /app

COPY ./build/libs/greenlight-core-api-0.0.1-SNAPSHOT.jar /app/greenlight-core-api.jar

EXPOSE 18080 18090

ENTRYPOINT ["java", "-jar", "greenlight-core-api.jar"]