package com.portfolio.performance.validator;

import java.util.List;

public record ValidationResult(boolean isValid, List<String> reasons) {

    public static ValidationResult pass() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult fail(List<String> reasons) {
        return new ValidationResult(false, List.copyOf(reasons));
    }
}
