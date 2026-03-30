package com.medhead.poc.acceptance;

import com.medhead.poc.domain.model.BedReservationEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Acceptance-test sink that captures every {@link BedReservationEvent}
 * published by the application inside the current Cucumber scenario. Cleared
 * in {@link Hooks} so each scenario starts with an empty record. Lives in the
 * application context (singleton bean) -- Spring's in-process event bus
 * delivers on the publisher's thread, and REST Assured drives HTTP calls that
 * dispatch through the same context, so a thread-safe list is sufficient.
 */
@Component
public class BedReservationEventRecorder {

    private final List<BedReservationEvent> events = new CopyOnWriteArrayList<>();

    @EventListener
    public void on(BedReservationEvent event) {
        events.add(event);
    }

    public List<BedReservationEvent> events() {
        return List.copyOf(events);
    }

    public void reset() {
        events.clear();
    }
}
