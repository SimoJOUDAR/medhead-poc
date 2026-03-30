package com.medhead.poc.domain.port.out;

import com.medhead.poc.domain.model.BedReservationEvent;

/**
 * Driven port through which the recommendation service announces a successful
 * bed reservation. The adapter is free to fan it out synchronously in-process
 * (Spring {@code ApplicationEventPublisher}, per D7) or -- in a production
 * evolution -- onto a message broker without changing the application layer.
 */
public interface BedReservationEventPublisher {

    void publish(BedReservationEvent event);
}
