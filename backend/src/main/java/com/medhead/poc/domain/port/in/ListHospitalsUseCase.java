package com.medhead.poc.domain.port.in;

import com.medhead.poc.application.dto.HospitalDto;
import java.util.List;

/**
 * Driving port exposing the hospital catalogue with each entry's specialties
 * and bed availability, so clients can inspect the network before routing an
 * emergency.
 */
public interface ListHospitalsUseCase {

    List<HospitalDto> listAll();

    HospitalDto findById(Long id);
}
