package com.medhead.poc.infrastructure.adapter.out.event;

import com.medhead.poc.domain.model.BedReservationEvent;
import com.medhead.poc.domain.port.out.BedReservationEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * In-process adapter delegating to Spring's {@link ApplicationEventPublisher}
 * (D7: no message broker in the PoC). The domain port stays free of any Spring
 * types; the bridge happens here.
 */
@Component
public class SpringBedReservationEventPublisher implements BedReservationEventPublisher {

    private final ApplicationEventPublisher delegate;

    public SpringBedReservationEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(BedReservationEvent event) {
        delegate.publishEvent(event);
    }
}
