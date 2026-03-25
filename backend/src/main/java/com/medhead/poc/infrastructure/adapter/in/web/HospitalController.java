package com.medhead.poc.infrastructure.adapter.in.web;

import com.medhead.poc.application.dto.HospitalDto;
import com.medhead.poc.domain.port.in.ListHospitalsUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the hospital catalogue with each hospital's specialty-bed summary.
 * Authenticated access only -- falls under the default {@code /api/v1/**}
 * rule of the security chain.
 */
@RestController
@RequestMapping("/api/v1/hospitals")
public class HospitalController {

    private final ListHospitalsUseCase listHospitalsUseCase;

    public HospitalController(ListHospitalsUseCase listHospitalsUseCase) {
        this.listHospitalsUseCase = listHospitalsUseCase;
    }

    @GetMapping
    public List<HospitalDto> listAll() {
        return listHospitalsUseCase.listAll();
    }

    @GetMapping("/{id}")
    public HospitalDto findById(@PathVariable Long id) {
        return listHospitalsUseCase.findById(id);
    }
}
