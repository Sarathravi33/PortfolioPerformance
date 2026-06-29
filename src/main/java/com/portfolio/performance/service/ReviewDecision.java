package com.portfolio.performance.service;

import com.portfolio.performance.dto.ReturnStatus;

import java.util.List;

public record ReviewDecision(ReturnStatus status, List<String> reasons) {

    public static ReviewDecision valid() {
        return new ReviewDecision(ReturnStatus.VALID, List.of());
    }

    public static ReviewDecision reviewRequired(List<String> reasons) {
        return new ReviewDecision(ReturnStatus.REVIEW_REQUIRED, List.copyOf(reasons));
    }
}
