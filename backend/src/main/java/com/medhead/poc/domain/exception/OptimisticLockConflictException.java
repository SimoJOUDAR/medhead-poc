package com.medhead.poc.domain.exception;

/**
 * Signals that a bed-reservation write lost an optimistic-locking race: the
 * hospital_specialties row observed by the service has been updated by another
 * transaction since it was loaded. Translated at the persistence adapter
 * boundary from Spring's {@code ObjectOptimisticLockingFailureException} so the
 * application layer stays framework-agnostic (E1). The recommendation service
 * catches it to retry against the next-nearest candidate; when every retry is
 * exhausted, the exception propagates and PR-S5-08 maps it to HTTP 503.
 */
public class OptimisticLockConflictException extends RuntimeException {

    private final Long hospitalSpecialtyId;

    public OptimisticLockConflictException(Long hospitalSpecialtyId) {
        super("Optimistic lock conflict on hospital_specialty id=" + hospitalSpecialtyId);
        this.hospitalSpecialtyId = hospitalSpecialtyId;
    }

    public OptimisticLockConflictException(Long hospitalSpecialtyId, Throwable cause) {
        super("Optimistic lock conflict on hospital_specialty id=" + hospitalSpecialtyId, cause);
        this.hospitalSpecialtyId = hospitalSpecialtyId;
    }

    public Long hospitalSpecialtyId() {
        return hospitalSpecialtyId;
    }
}
