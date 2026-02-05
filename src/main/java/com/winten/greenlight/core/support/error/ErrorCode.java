package com.winten.greenlight.core.support.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    ACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Action not found.", LogLevel.INFO),
    ACTION_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "Action Group not found.", LogLevel.INFO),
    CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "Customer not found.", LogLevel.INFO),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Room not found.", LogLevel.INFO),
    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "Ticket not found.", LogLevel.INFO),

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad Request.", LogLevel.INFO),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token.", LogLevel.INFO),
    INVALID_TICKET_STATE(HttpStatus.BAD_REQUEST, "Invalid Ticket state.", LogLevel.WARN),

    REDIS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error has occurred while accessing data." , LogLevel.ERROR),
    INVALID_DATA(HttpStatus.INTERNAL_SERVER_ERROR, "Data is not valid." , LogLevel.WARN),
    JSON_CONVERT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Json conversion error", LogLevel.WARN),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error has occurred.", LogLevel.ERROR),

    NOT_MODIFIED(HttpStatus.NOT_MODIFIED, "Not Modified", LogLevel.INFO),

    ;

    private final HttpStatus status; //HTTP 응답 코드
    private final String message; // 노출 메시지
    private final LogLevel logLevel;

}