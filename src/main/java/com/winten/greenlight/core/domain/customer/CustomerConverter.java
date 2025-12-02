package com.winten.greenlight.core.domain.customer;

import com.winten.greenlight.core.api.controller.queue.CustomerSessionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerConverter {
    CustomerSessionResponse toResponse(final CustomerSession customerSession);
}