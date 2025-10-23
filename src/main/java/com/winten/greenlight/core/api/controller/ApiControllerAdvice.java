package com.winten.greenlight.core.api.controller;

import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestControllerAdvice
public class ApiControllerAdvice {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(CoreException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCoreException(CoreException ex) {
        return Mono.just(new ErrorResponse(ex))
                    .doOnNext(response -> {
                        switch (ex.getErrorType().getLogLevel()) {
                            case INFO:
                                log.info(response.toString());
                                break;
                            case WARN:
                                log.warn(response.toString());
                                break;
                            case ERROR:
                                log.error(response.toString());
                                break;
                            default:
                                log.debug(response.toString());
                        }
                    })
                    .map(response -> ResponseEntity.status(ex.getErrorType().getStatus()).body(response));
    }

    // TODO Map이 아닌 ApiResponse를 리턴하도록 개선하고, 에러메시지 표출방식 통일
    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<Map<String, String>>> handleServerWebInputException(
            ServerWebInputException ex) {
        String parameterName = ex.getMethodParameter().getParameterName();
        String parameterType = ex.getMethodParameter().getParameterType().getSimpleName();

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Missing required request parameter");
        errorResponse.put("parameterName", parameterName);
        errorResponse.put("parameterType", parameterType);

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse));
    }

}