package com.medhead.poc.infrastructure.adapter.in.web;

import com.medhead.poc.application.dto.SpecialtyDto;
import com.medhead.poc.domain.port.in.ListSpecialtiesUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the seeded NHS specialty catalogue. Authenticated access only --
 * falls under the default {@code /api/v1/**} rule of the security chain.
 */
@RestController
@RequestMapping("/api/v1/specialties")
public class SpecialtyController {

    private final ListSpecialtiesUseCase listSpecialtiesUseCase;

    public SpecialtyController(ListSpecialtiesUseCase listSpecialtiesUseCase) {
        this.listSpecialtiesUseCase = listSpecialtiesUseCase;
    }

    @GetMapping
    public List<SpecialtyDto> listAll() {
        return listSpecialtiesUseCase.listAll();
    }
}
