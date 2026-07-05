package dev.coreystevens.titlerate.controller;

import dev.coreystevens.titlerate.dto.PremiumRequest;
import dev.coreystevens.titlerate.dto.PremiumResponse;
import dev.coreystevens.titlerate.service.PremiumCalculationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calculate")
public class PremiumController {

    private final PremiumCalculationService calculator;

    public PremiumController(PremiumCalculationService calculator) {
        this.calculator = calculator;
    }

    @PostMapping
    public ResponseEntity<PremiumResponse> calculate(@Valid @RequestBody PremiumRequest req) {
        return ResponseEntity.ok(calculator.calculate(req));
    }
}
