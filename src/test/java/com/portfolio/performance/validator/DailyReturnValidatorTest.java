package com.portfolio.performance.validator;

import com.portfolio.performance.dto.DailyReturnRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DailyReturnValidatorTest {

    private DailyReturnValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DailyReturnValidator();
    }

    // -----------------------------------------------------------------------
    // Valid baseline
    // -----------------------------------------------------------------------

    @Test
    void validRequest_passesAllRules() {
        ValidationResult result = validator.validate(buildValidRequest());

        assertThat(result.isValid()).isTrue();
        assertThat(result.reasons()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // R1 — negative begin market value
    // -----------------------------------------------------------------------

    @Test
    void r1_negativeBeginMarketValue_returnsInvalid() {
        DailyReturnRequest request = buildValidRequest();
        request.setBeginMarketValue(-1.0);

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.reasons()).hasSize(1);
        assertThat(result.reasons().get(0)).isEqualTo("Begin market value cannot be negative.");
    }

    // -----------------------------------------------------------------------
    // R2 — negative end market value
    // -----------------------------------------------------------------------

    @Test
    void r2_negativeEndMarketValue_returnsInvalid() {
        DailyReturnRequest request = buildValidRequest();
        request.setEndMarketValue(-1.0);

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.reasons()).hasSize(1);
        assertThat(result.reasons().get(0)).isEqualTo("End market value cannot be negative.");
    }

    // -----------------------------------------------------------------------
    // R3 — currency missing or blank
    // -----------------------------------------------------------------------

    @Test
    void r3_nullCurrency_returnsInvalid() {
        DailyReturnRequest request = buildValidRequest();
        request.setCurrency(null);

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.reasons()).containsExactly("Currency is required.");
    }

    @Test
    void r3_blankCurrency_returnsInvalid() {
        DailyReturnRequest request = buildValidRequest();
        request.setCurrency("   ");

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.reasons()).containsExactly("Currency is required.");
    }

    // -----------------------------------------------------------------------
    // R4 — begin is zero but end is non-zero
    // -----------------------------------------------------------------------

    @Test
    void r4_beginZeroEndNonZero_returnsInvalid() {
        DailyReturnRequest request = buildValidRequest();
        request.setBeginMarketValue(0.0);
        request.setEndMarketValue(50000.0);

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.reasons()).containsExactly(
                "Begin market value is 0 but end market value is non-zero: cannot compute return.");
    }

    @Test
    void r4_bothZero_passes() {
        DailyReturnRequest request = buildValidRequest();
        request.setBeginMarketValue(0.0);
        request.setEndMarketValue(0.0);

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isTrue();
        assertThat(result.reasons()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Combined failures — multiple reasons accumulate (no short-circuit)
    // -----------------------------------------------------------------------

    @Test
    void combined_r1AndR2_accumulatesBothReasons() {
        DailyReturnRequest request = buildValidRequest();
        request.setBeginMarketValue(-100.0);
        request.setEndMarketValue(-200.0);

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.reasons()).hasSize(2);
        assertThat(result.reasons()).containsExactly(
                "Begin market value cannot be negative.",
                "End market value cannot be negative.");
    }

    @Test
    void combined_r2AndR4_accumulatesBothReasons() {
        // beginMarketValue=0, endMarketValue=-500 triggers R2 (negative end) and R4 (begin=0, end!=0)
        DailyReturnRequest request = buildValidRequest();
        request.setBeginMarketValue(0.0);
        request.setEndMarketValue(-500.0);

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.reasons()).hasSize(2);
        assertThat(result.reasons()).containsExactly(
                "End market value cannot be negative.",
                "Begin market value is 0 but end market value is non-zero: cannot compute return.");
    }

    @Test
    void combined_r1R2R3_accumulatesThreeReasons() {
        DailyReturnRequest request = buildValidRequest();
        request.setBeginMarketValue(-100.0);
        request.setEndMarketValue(-200.0);
        request.setCurrency(null);

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.reasons()).hasSize(3);
        assertThat(result.reasons()).containsExactly(
                "Begin market value cannot be negative.",
                "End market value cannot be negative.",
                "Currency is required.");
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private DailyReturnRequest buildValidRequest() {
        DailyReturnRequest request = new DailyReturnRequest();
        request.setPortfolioId("PF-1001");
        request.setValuationDate("2026-06-14");
        request.setBeginMarketValue(1_000_000.0);
        request.setEndMarketValue(1_035_000.0);
        request.setNetCashFlow(10_000.0);
        request.setBenchmarkReturnPct(1.8);
        request.setCurrency("USD");
        request.setRequestedBy("advisor01");
        return request;
    }
}
