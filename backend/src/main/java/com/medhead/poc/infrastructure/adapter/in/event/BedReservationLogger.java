package com.medhead.poc.infrastructure.adapter.in.event;

import com.medhead.poc.domain.model.BedReservationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Demonstration inbound adapter for {@link BedReservationEvent}: logs every
 * reservation at INFO with a single-line, grep-friendly format. Proves the B6
 * event-driven extension path end-to-end without pulling in an auxiliary
 * downstream system. Payload fields are ids, public names and counts only --
 * no patient data (E2).
 */
@Component
public class BedReservationLogger {

    private static final Logger log = LoggerFactory.getLogger(BedReservationLogger.class);

    @EventListener
    public void on(BedReservationEvent event) {
        log.info("[BED_RESERVATION] hospitalId={} hospital=\"{}\" specialtyId={} specialty=\"{}\" remainingBeds={}",
                event.hospitalId(),
                event.hospitalName(),
                event.specialtyId(),
                event.specialtyName(),
                event.remainingBeds());
    }
}
