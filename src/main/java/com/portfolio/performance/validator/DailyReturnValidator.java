package com.portfolio.performance.validator;

import com.portfolio.performance.dto.DailyReturnRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DailyReturnValidator {

    public ValidationResult validate(DailyReturnRequest request) {
        List<String> reasons = new ArrayList<>();

        // R1 — negative begin market value
        if (request.getBeginMarketValue() != null && request.getBeginMarketValue() < 0) {
            reasons.add("Begin market value cannot be negative.");
        }

        // R2 — negative end market value
        if (request.getEndMarketValue() != null && request.getEndMarketValue() < 0) {
            reasons.add("End market value cannot be negative.");
        }

        // R3 — currency missing or blank (secondary guard; @NotBlank covers structural absence)
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            reasons.add("Currency is required.");
        }

        // R4 — begin is zero but end is non-zero: return is undefined
        if (request.getBeginMarketValue() != null && request.getEndMarketValue() != null
                && request.getBeginMarketValue() == 0 && request.getEndMarketValue() != 0) {
            reasons.add("Begin market value is 0 but end market value is non-zero: cannot compute return.");
        }

        return reasons.isEmpty() ? ValidationResult.pass() : ValidationResult.fail(reasons);
    }
}
