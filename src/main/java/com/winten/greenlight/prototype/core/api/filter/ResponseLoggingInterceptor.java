package com.winten.greenlight.prototype.core.api.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.nio.channels.Channels;
import java.util.Objects;

@Component
@Slf4j
@Lazy
public class ResponseLoggingInterceptor extends ServerHttpResponseDecorator {
    private final long startTime;

    public ResponseLoggingInterceptor(ServerHttpResponse delegate, long startTime) {
        super(delegate);
        this.startTime = startTime;
    }
    public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
        Flux<DataBuffer> bufferFlux = Flux.from(body);
        return super.writeWith(bufferFlux.doOnNext(dataBuffer -> {
            try(ByteArrayOutputStream boas = new ByteArrayOutputStream()) {
                Channels.newChannel(boas).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
                log.info("Response |  status={} |Time Taken: {}ms", Objects.requireNonNull(getStatusCode()).value(),System.currentTimeMillis() - startTime);
            }
            catch (Exception e) {
                log.error("Error during logging request {}", e.getMessage());
            }
        }));
    }
}