package com.portfolio.performance.service;

import com.portfolio.performance.dto.DailyReturnRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CalculationReviewer {

    private static final double EXCESS_RETURN_THRESHOLD = 5.0;
    private static final double CASH_FLOW_THRESHOLD_PCT = 0.20;

    public ReviewDecision review(DailyReturnRequest request, double portfolioReturnPct) {
        List<String> reasons = new ArrayList<>();

        // R5 — excess return deviation exceeds 5%
        double deviation = Math.abs(portfolioReturnPct - request.getBenchmarkReturnPct());
        if (deviation > EXCESS_RETURN_THRESHOLD) {
            reasons.add(String.format(
                "Portfolio return (%.4f%%) deviates from benchmark (%.4f%%) by %.4f%%, exceeding the 5%% threshold.",
                portfolioReturnPct, request.getBenchmarkReturnPct(), deviation
            ));
        }

        // R6 — absolute net cash flow exceeds 20% of begin market value
        //       skipped when beginMarketValue is 0 to avoid trivially-true comparison
        if (request.getBeginMarketValue() != 0) {
            double cashFlowLimit = CASH_FLOW_THRESHOLD_PCT * request.getBeginMarketValue();
            double absCashFlow = Math.abs(request.getNetCashFlow());
            if (absCashFlow > cashFlowLimit) {
                reasons.add(String.format(
                    "Absolute net cash flow (%.2f) exceeds 20%% of begin market value (%.2f).",
                    absCashFlow, cashFlowLimit
                ));
            }
        }

        return reasons.isEmpty() ? ReviewDecision.valid() : ReviewDecision.reviewRequired(reasons);
    }
}
