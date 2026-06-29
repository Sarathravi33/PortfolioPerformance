package com.portfolio.performance.service;

import com.portfolio.performance.dto.DailyReturnRequest;
import com.portfolio.performance.dto.DailyReturnResponse;
import com.portfolio.performance.dto.ReturnStatus;
import com.portfolio.performance.model.ValuationAudit;
import com.portfolio.performance.repository.ValuationAuditRepository;
import com.portfolio.performance.validator.DailyReturnValidator;
import com.portfolio.performance.validator.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyReturnServiceTest {

    @Mock
    private DailyReturnValidator validator;

    @Mock
    private CalculationReviewer reviewer;

    @Mock
    private ValuationAuditRepository auditRepository;

    @InjectMocks
    private DailyReturnService service;

    // -----------------------------------------------------------------------
    // Happy path — spec example
    // -----------------------------------------------------------------------

    @Test
    void specExample_returnsValid_with2_5PctReturn() {
        DailyReturnRequest request = buildRequest(1_000_000.0, 1_035_000.0, 10_000.0, 1.8);

        when(validator.validate(request)).thenReturn(ValidationResult.pass());
        when(reviewer.review(eq(request), eq(2.5))).thenReturn(ReviewDecision.valid());

        DailyReturnResponse response = service.calculate(request);

        assertThat(response.getStatus()).isEqualTo(ReturnStatus.VALID);
        assertThat(response.getPortfolioReturnPct()).isEqualTo(2.5);
        assertThat(response.getExcessReturnPct()).isEqualTo(0.7);
        assertThat(response.getBenchmarkReturnPct()).isEqualTo(1.8);
        assertThat(response.getReasons()).isEmpty();
        assertThat(response.getProcessedAt()).isNotBlank();
        verify(auditRepository, times(1)).save(any(ValuationAudit.class));
    }

    // -----------------------------------------------------------------------
    // REVIEW_REQUIRED — excess return deviation > 5%
    // -----------------------------------------------------------------------

    @Test
    void excessReturnExceeds5Pct_returnsReviewRequired() {
        // portfolioReturn = ((1090000 - 1000000 - 0) / 1000000) * 100 = 9.0%
        // deviation from benchmark 1.8% = 7.2% > 5%
        DailyReturnRequest request = buildRequest(1_000_000.0, 1_090_000.0, 0.0, 1.8);
        String reason = "Portfolio return (9.0000%) deviates from benchmark (1.8000%) by 7.2000%, exceeding the 5% threshold.";

        when(validator.validate(request)).thenReturn(ValidationResult.pass());
        when(reviewer.review(eq(request), eq(9.0)))
                .thenReturn(ReviewDecision.reviewRequired(List.of(reason)));

        DailyReturnResponse response = service.calculate(request);

        assertThat(response.getStatus()).isEqualTo(ReturnStatus.REVIEW_REQUIRED);
        assertThat(response.getPortfolioReturnPct()).isEqualTo(9.0);
        assertThat(response.getExcessReturnPct()).isEqualTo(7.2);
        assertThat(response.getReasons()).containsExactly(reason);
        verify(auditRepository, times(1)).save(any(ValuationAudit.class));
    }

    // -----------------------------------------------------------------------
    // REVIEW_REQUIRED — cash flow exceeds 20% of BMV
    // -----------------------------------------------------------------------

    @Test
    void cashFlowExceeds20PctOfBmv_returnsReviewRequired() {
        // |cashFlow| = 250000 > 20% of 1000000 (= 200000)
        // portfolioReturn = ((1025000 - 1000000 - 250000) / 1000000) * 100 = -22.5%
        DailyReturnRequest request = buildRequest(1_000_000.0, 1_025_000.0, 250_000.0, 1.8);
        String reason = "Absolute net cash flow (250000.00) exceeds 20% of begin market value (200000.00).";

        when(validator.validate(request)).thenReturn(ValidationResult.pass());
        when(reviewer.review(eq(request), eq(-22.5)))
                .thenReturn(ReviewDecision.reviewRequired(List.of(reason)));

        DailyReturnResponse response = service.calculate(request);

        assertThat(response.getStatus()).isEqualTo(ReturnStatus.REVIEW_REQUIRED);
        assertThat(response.getPortfolioReturnPct()).isEqualTo(-22.5);
        assertThat(response.getReasons()).containsExactly(reason);
        verify(auditRepository, times(1)).save(any(ValuationAudit.class));
    }

    // -----------------------------------------------------------------------
    // REVIEW_REQUIRED — both conditions fire simultaneously
    // -----------------------------------------------------------------------

    @Test
    void bothReviewConditions_returnsTwoReasons() {
        // portfolioReturn = ((1090000 - 1000000 - 250000) / 1000000) * 100 = -16.0%
        // deviation from benchmark = |-16.0 - 1.8| = 17.8% > 5%
        // |cashFlow| = 250000 > 20% of 1000000
        DailyReturnRequest request = buildRequest(1_000_000.0, 1_090_000.0, 250_000.0, 1.8);
        List<String> reasons = List.of(
                "Portfolio return (-16.0000%) deviates from benchmark (1.8000%) by 17.8000%, exceeding the 5% threshold.",
                "Absolute net cash flow (250000.00) exceeds 20% of begin market value (200000.00)."
        );

        when(validator.validate(request)).thenReturn(ValidationResult.pass());
        when(reviewer.review(eq(request), eq(-16.0)))
                .thenReturn(ReviewDecision.reviewRequired(reasons));

        DailyReturnResponse response = service.calculate(request);

        assertThat(response.getStatus()).isEqualTo(ReturnStatus.REVIEW_REQUIRED);
        assertThat(response.getReasons()).hasSize(2);
        assertThat(response.getReasons()).isEqualTo(reasons);
        verify(auditRepository, times(1)).save(any(ValuationAudit.class));
    }

    // -----------------------------------------------------------------------
    // INVALID_INPUT — all four validation paths
    // -----------------------------------------------------------------------

    @Test
    void r1_negativeBeginMarketValue_returnsInvalidInput() {
        DailyReturnRequest request = buildRequest(-500.0, 1_000.0, 0.0, 1.8);
        when(validator.validate(request)).thenReturn(
                ValidationResult.fail(List.of("Begin market value cannot be negative.")));

        DailyReturnResponse response = service.calculate(request);

        assertThat(response.getStatus()).isEqualTo(ReturnStatus.INVALID_INPUT);
        assertThat(response.getPortfolioReturnPct()).isNull();
        assertThat(response.getExcessReturnPct()).isNull();
        assertThat(response.getReasons()).containsExactly("Begin market value cannot be negative.");
        verify(reviewer, never()).review(any(), anyDouble());
        verify(auditRepository, times(1)).save(any(ValuationAudit.class));
    }

    @Test
    void r2_negativeEndMarketValue_returnsInvalidInput() {
        DailyReturnRequest request = buildRequest(1_000_000.0, -1.0, 0.0, 1.8);
        when(validator.validate(request)).thenReturn(
                ValidationResult.fail(List.of("End market value cannot be negative.")));

        DailyReturnResponse response = service.calculate(request);

        assertThat(response.getStatus()).isEqualTo(ReturnStatus.INVALID_INPUT);
        assertThat(response.getPortfolioReturnPct()).isNull();
        assertThat(response.getExcessReturnPct()).isNull();
        assertThat(response.getReasons()).containsExactly("End market value cannot be negative.");
        verify(reviewer, never()).review(any(), anyDouble());
    }

    @Test
    void r3_missingCurrency_returnsInvalidInput() {
        DailyReturnRequest request = buildRequest(1_000_000.0, 1_035_000.0, 10_000.0, 1.8);
        request.setCurrency(null);
        when(validator.validate(request)).thenReturn(
                ValidationResult.fail(List.of("Currency is required.")));

        DailyReturnResponse response = service.calculate(request);

        assertThat(response.getStatus()).isEqualTo(ReturnStatus.INVALID_INPUT);
        assertThat(response.getPortfolioReturnPct()).isNull();
        assertThat(response.getExcessReturnPct()).isNull();
        assertThat(response.getReasons()).containsExactly("Currency is required.");
        verify(reviewer, never()).review(any(), anyDouble());
    }

    @Test
    void r4_beginZeroEndNonZero_returnsInvalidInput() {
        DailyReturnRequest request = buildRequest(0.0, 50_000.0, 0.0, 1.8);
        when(validator.validate(request)).thenReturn(
                ValidationResult.fail(List.of(
                        "Begin market value is 0 but end market value is non-zero: cannot compute return.")));

        DailyReturnResponse response = service.calculate(request);

        assertThat(response.getStatus()).isEqualTo(ReturnStatus.INVALID_INPUT);
        assertThat(response.getPortfolioReturnPct()).isNull();
        assertThat(response.getExcessReturnPct()).isNull();
        assertThat(response.getReasons()).containsExactly(
                "Begin market value is 0 but end market value is non-zero: cannot compute return.");
        verify(reviewer, never()).review(any(), anyDouble());
    }

    // -----------------------------------------------------------------------
    // Edge case — begin=0, end=0 → portfolioReturnPct=0
    // -----------------------------------------------------------------------

    @Test
    void bothMarketValuesZero_returnsZeroPortfolioReturn() {
        DailyReturnRequest request = buildRequest(0.0, 0.0, 0.0, 1.8);

        when(validator.validate(request)).thenReturn(ValidationResult.pass());

        ArgumentCaptor<Double> returnPctCaptor = ArgumentCaptor.forClass(Double.class);
        when(reviewer.review(eq(request), returnPctCaptor.capture()))
                .thenReturn(ReviewDecision.valid());

        DailyReturnResponse response = service.calculate(request);

        assertThat(returnPctCaptor.getValue()).isEqualTo(0.0);
        assertThat(response.getPortfolioReturnPct()).isEqualTo(0.0);
        assertThat(response.getExcessReturnPct()).isEqualTo(-1.8);
        assertThat(response.getStatus()).isEqualTo(ReturnStatus.VALID);
        verify(auditRepository, times(1)).save(any(ValuationAudit.class));
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private DailyReturnRequest buildRequest(double bmv, double emv, double cashFlow, double bench) {
        DailyReturnRequest request = new DailyReturnRequest();
        request.setPortfolioId("PF-1001");
        request.setValuationDate("2026-06-14");
        request.setBeginMarketValue(bmv);
        request.setEndMarketValue(emv);
        request.setNetCashFlow(cashFlow);
        request.setBenchmarkReturnPct(bench);
        request.setCurrency("USD");
        request.setRequestedBy("advisor01");
        return request;
    }
}
