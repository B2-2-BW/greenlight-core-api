package com.winten.greenlight.prototype.core.domain.customer;

import com.winten.greenlight.prototype.core.db.repository.redis.customer.CustomerRepository;
import com.winten.greenlight.prototype.core.db.repository.redis.customer.CustomerZSetEntity;
import com.winten.greenlight.prototype.core.domain.event.Event;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.annotation.ContinueSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final Tracer tracer;

//    @Observed(
//            name = "service.createCustomer",
//            contextualName = "processing in createCustomer",
//            lowCardinalityKeyValues = {"service", "CustomerService"}
//    )
    public Mono<Customer> createCustomer(Customer customer, Event event) {
        customer.setCustomerId(event.getEventName()); // 이벤트 이름으로 Customer ID 설정
        return customerRepository.createCustomer(customer);

//                .doOnSuccess(result -> log.info("Customer created: {}", result))
//                .doOnError(error -> log.error("Error creating customer", error));
    }

    public Mono<CustomerQueueInfo> getCustomerQueueInfo(Customer customer) {
        Span span = tracer.nextSpan().name("getCustomerQueueInfo");
        return Mono.deferContextual(ctx -> {
            try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
                span.tag("service.info", "additional details");
                span.event("entering doSomething");
                return customerRepository.getCustomerStatus(customer)
                        .map(info -> {
                            if (info.getPosition() != null && info.getQueueSize() != null) {
                                info.setEstimatedWaitTime(info.getPosition() * 60); // 예시 계산
                            }
                            return info;
                        })
                        .filter(info -> info.getCustomerId() != null && info.getWaitingPhase() != null)
                        .switchIfEmpty(Mono.error(new CoreException(ErrorType.DEFAULT_ERROR, "User가 없습니다.")))
                        .doOnError(e -> {
                            CoreException ce = (CoreException) e;
                            span.error(e);
                            span.tag("error.type", ce.getErrorType().name());
                            span.tag("error.detail", ce.getDetail().toString());
                        })
                        .doOnTerminate(span::end);
            }
        });

    }

    public Mono<Customer> deleteCustomer(Customer customer) {
        return customerRepository.deleteCustomer(customer);
    }

}