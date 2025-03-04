package com.winten.greenlight.prototype.core.api.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class RequestMonitorWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerWebExchangeDecorator decorator = new ServerWebExchangeDecorator(exchange){
            @Override
            public ServerHttpResponse getResponse() {
                return new ResponseLoggingInterceptor(super.getResponse(), startTime);
            }
        };
        return chain.filter(decorator);
    }
}