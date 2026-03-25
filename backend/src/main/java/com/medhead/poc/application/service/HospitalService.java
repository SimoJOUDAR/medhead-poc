package com.medhead.poc.application.service;

import com.medhead.poc.application.dto.HospitalDto;
import com.medhead.poc.application.dto.HospitalDto.HospitalSpecialtySummaryDto;
import com.medhead.poc.domain.exception.HospitalNotFoundException;
import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.domain.model.HospitalSpecialty;
import com.medhead.poc.domain.port.in.ListHospitalsUseCase;
import com.medhead.poc.domain.port.out.HospitalRepository;
import com.medhead.poc.domain.port.out.HospitalSpecialtyRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Serves the hospital catalogue enriched with each hospital's specialty-bed
 * summary. Composes the two read ports in the application layer rather than
 * leaking a persistence-level join into the domain.
 */
@Service
public class HospitalService implements ListHospitalsUseCase {

    private final HospitalRepository hospitalRepository;
    private final HospitalSpecialtyRepository hospitalSpecialtyRepository;

    public HospitalService(HospitalRepository hospitalRepository,
                           HospitalSpecialtyRepository hospitalSpecialtyRepository) {
        this.hospitalRepository = hospitalRepository;
        this.hospitalSpecialtyRepository = hospitalSpecialtyRepository;
    }

    @Override
    public List<HospitalDto> listAll() {
        Map<Long, List<HospitalSpecialty>> specialtiesByHospital = groupSpecialtiesByHospital();
        return hospitalRepository.findAll().stream()
                .map(hospital -> toDto(hospital, specialtiesByHospital.getOrDefault(hospital.id(), List.of())))
                .toList();
    }

    @Override
    public HospitalDto findById(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new HospitalNotFoundException(id));
        List<HospitalSpecialty> specialties = groupSpecialtiesByHospital()
                .getOrDefault(hospital.id(), List.of());
        return toDto(hospital, specialties);
    }

    private Map<Long, List<HospitalSpecialty>> groupSpecialtiesByHospital() {
        return hospitalSpecialtyRepository.findAll().stream()
                .collect(Collectors.groupingBy(HospitalSpecialty::hospitalId));
    }

    private static HospitalDto toDto(Hospital hospital, List<HospitalSpecialty> specialties) {
        List<HospitalSpecialtySummaryDto> summaries = specialties.stream()
                .map(HospitalService::toSummary)
                .toList();
        return new HospitalDto(
                hospital.id(),
                hospital.name(),
                hospital.coordinates().latitude(),
                hospital.coordinates().longitude(),
                hospital.address(),
                summaries
        );
    }

    private static HospitalSpecialtySummaryDto toSummary(HospitalSpecialty hospitalSpecialty) {
        return new HospitalSpecialtySummaryDto(
                hospitalSpecialty.specialty().id(),
                hospitalSpecialty.specialty().name(),
                hospitalSpecialty.availableBeds()
        );
    }
}
