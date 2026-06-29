package com.portfolio.performance.service;

import com.portfolio.performance.dto.DailyReturnRequest;
import com.portfolio.performance.dto.DailyReturnResponse;
import com.portfolio.performance.dto.ReturnStatus;
import com.portfolio.performance.model.ValuationAudit;
import com.portfolio.performance.repository.ValuationAuditRepository;
import com.portfolio.performance.validator.DailyReturnValidator;
import com.portfolio.performance.validator.ValidationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class DailyReturnService {

    private final DailyReturnValidator validator;
    private final CalculationReviewer reviewer;
    private final ValuationAuditRepository auditRepository;

    public DailyReturnService(DailyReturnValidator validator,
                               CalculationReviewer reviewer,
                               ValuationAuditRepository auditRepository) {
        this.validator = validator;
        this.reviewer = reviewer;
        this.auditRepository = auditRepository;
    }

    public DailyReturnResponse calculate(DailyReturnRequest request) {
        String processedAt = Instant.now().toString();

        // Step 1 — validate inputs
        ValidationResult validation = validator.validate(request);
        if (!validation.isValid()) {
            persistAudit(request, null, null, ReturnStatus.INVALID_INPUT, processedAt);
            return buildResponse(request, null, null,
                    ReturnStatus.INVALID_INPUT, validation.reasons(), processedAt);
        }

        // Step 2 — compute portfolio return
        double rawPortfolioReturn = 0.0;
        if (request.getBeginMarketValue() > 0) {
            rawPortfolioReturn = ((request.getEndMarketValue()
                    - request.getBeginMarketValue()
                    - request.getNetCashFlow())
                    / request.getBeginMarketValue()) * 100.0;
        }
        // beginMarketValue == 0 && endMarketValue == 0: return stays 0.0

        // Step 3 — compute excess return
        double rawExcessReturn = rawPortfolioReturn - request.getBenchmarkReturnPct();

        // Step 4 — round to 4 decimal places
        double portfolioReturnPct = round4(rawPortfolioReturn);
        double excessReturnPct    = round4(rawExcessReturn);

        // Step 5 — review against tolerance thresholds
        ReviewDecision decision = reviewer.review(request, portfolioReturnPct);

        // Step 6 — persist audit record
        persistAudit(request, portfolioReturnPct, excessReturnPct, decision.status(), processedAt);

        // Step 7 — return response
        return buildResponse(request, portfolioReturnPct, excessReturnPct,
                decision.status(), decision.reasons(), processedAt);
    }

    private double round4(double value) {
        return BigDecimal.valueOf(value)
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void persistAudit(DailyReturnRequest request,
                               Double portfolioReturnPct,
                               Double excessReturnPct,
                               ReturnStatus status,
                               String processedAt) {
        ValuationAudit audit = new ValuationAudit();
        audit.setPortfolioId(request.getPortfolioId());
        audit.setValuationDate(request.getValuationDate());
        audit.setBeginMarketValue(request.getBeginMarketValue());
        audit.setEndMarketValue(request.getEndMarketValue());
        audit.setNetCashFlow(request.getNetCashFlow());
        audit.setBenchmarkReturnPct(request.getBenchmarkReturnPct());
        audit.setCurrency(request.getCurrency());
        audit.setRequestedBy(request.getRequestedBy());
        audit.setPortfolioReturnPct(portfolioReturnPct);
        audit.setExcessReturnPct(excessReturnPct);
        audit.setStatus(status);
        audit.setProcessedAt(processedAt);
        auditRepository.save(audit);
    }

    private DailyReturnResponse buildResponse(DailyReturnRequest request,
                                               Double portfolioReturnPct,
                                               Double excessReturnPct,
                                               ReturnStatus status,
                                               List<String> reasons,
                                               String processedAt) {
        return DailyReturnResponse.builder()
                .portfolioId(request.getPortfolioId())
                .valuationDate(request.getValuationDate())
                .portfolioReturnPct(portfolioReturnPct)
                .benchmarkReturnPct(request.getBenchmarkReturnPct())
                .excessReturnPct(excessReturnPct)
                .status(status)
                .reasons(reasons)
                .processedAt(processedAt)
                .build();
    }
}
