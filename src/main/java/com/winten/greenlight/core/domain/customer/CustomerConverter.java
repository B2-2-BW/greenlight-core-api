package com.winten.greenlight.core.domain.customer;

import com.winten.greenlight.core.api.controller.queue.CustomerSessionResponse;
import com.winten.greenlight.core.api.controller.queue.QueueConfigResponse;
import com.winten.greenlight.core.domain.queue.ActionConfig;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerConverter {
    CustomerSessionResponse toResponse(final CustomerSession customerSession);
    QueueConfigResponse toResponse(final ActionConfig config);
}