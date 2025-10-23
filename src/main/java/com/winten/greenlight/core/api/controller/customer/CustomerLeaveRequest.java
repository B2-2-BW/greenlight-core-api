package com.winten.greenlight.core.api.controller.customer;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CustomerLeaveRequest {
    @JsonAlias(value = "g")
    private String greenlightToken;
}