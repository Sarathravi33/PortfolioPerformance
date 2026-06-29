package com.portfolio.performance.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PerformanceControllerIntegrationTest {

    private static final String URL = "/api/performance/daily-return";

    @Autowired
    private MockMvc mockMvc;

    // -----------------------------------------------------------------------
    // 200 VALID — spec example full round-trip
    // -----------------------------------------------------------------------

    @Test
    void specExample_returns200_statusValid() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId":        "PF-1001",
                                  "valuationDate":      "2026-06-14",
                                  "beginMarketValue":   1000000,
                                  "endMarketValue":     1035000,
                                  "netCashFlow":        10000,
                                  "benchmarkReturnPct": 1.8,
                                  "currency":           "USD",
                                  "requestedBy":        "advisor01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value("PF-1001"))
                .andExpect(jsonPath("$.valuationDate").value("2026-06-14"))
                .andExpect(jsonPath("$.portfolioReturnPct").value(2.5))
                .andExpect(jsonPath("$.benchmarkReturnPct").value(1.8))
                .andExpect(jsonPath("$.excessReturnPct").value(0.7))
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.reasons").isEmpty())
                .andExpect(jsonPath("$.processedAt").isNotEmpty());
    }

    // -----------------------------------------------------------------------
    // 200 REVIEW_REQUIRED — excess return deviation > 5%
    // -----------------------------------------------------------------------

    @Test
    void excessReturnExceeds5Pct_returns200_statusReviewRequired() throws Exception {
        // portfolioReturn = ((1090000 - 1000000 - 0) / 1000000) * 100 = 9.0%
        // deviation from benchmark 1.8% = 7.2% → exceeds 5% threshold
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId":        "PF-1002",
                                  "valuationDate":      "2026-06-14",
                                  "beginMarketValue":   1000000,
                                  "endMarketValue":     1090000,
                                  "netCashFlow":        0,
                                  "benchmarkReturnPct": 1.8,
                                  "currency":           "USD",
                                  "requestedBy":        "advisor01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioReturnPct").value(9.0))
                .andExpect(jsonPath("$.status").value("REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.reasons").isNotEmpty())
                .andExpect(jsonPath("$.reasons[0]").isString());
    }

    // -----------------------------------------------------------------------
    // 200 REVIEW_REQUIRED — cash flow exceeds 20% of begin market value
    // -----------------------------------------------------------------------

    @Test
    void cashFlowExceeds20Pct_returns200_statusReviewRequired() throws Exception {
        // |cashFlow| = 250000 > 20% of 1000000 (= 200000)
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId":        "PF-1003",
                                  "valuationDate":      "2026-06-14",
                                  "beginMarketValue":   1000000,
                                  "endMarketValue":     1025000,
                                  "netCashFlow":        250000,
                                  "benchmarkReturnPct": 1.8,
                                  "currency":           "USD",
                                  "requestedBy":        "advisor01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.reasons").isNotEmpty());
    }

    // -----------------------------------------------------------------------
    // 422 INVALID_INPUT — negative begin market value (R1)
    // -----------------------------------------------------------------------

    @Test
    void negativeBeginMarketValue_returns422_statusInvalidInput() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId":        "PF-1004",
                                  "valuationDate":      "2026-06-14",
                                  "beginMarketValue":   -500,
                                  "endMarketValue":     1000,
                                  "netCashFlow":        0,
                                  "benchmarkReturnPct": 1.8,
                                  "currency":           "USD",
                                  "requestedBy":        "advisor01"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.portfolioReturnPct").doesNotExist())
                .andExpect(jsonPath("$.excessReturnPct").doesNotExist())
                .andExpect(jsonPath("$.reasons[0]").value("Begin market value cannot be negative."));
    }

    // -----------------------------------------------------------------------
    // 422 INVALID_INPUT — begin zero, end non-zero (R4)
    // -----------------------------------------------------------------------

    @Test
    void beginZeroEndNonZero_returns422_statusInvalidInput() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId":        "PF-1005",
                                  "valuationDate":      "2026-06-14",
                                  "beginMarketValue":   0,
                                  "endMarketValue":     50000,
                                  "netCashFlow":        0,
                                  "benchmarkReturnPct": 1.8,
                                  "currency":           "USD",
                                  "requestedBy":        "advisor01"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.reasons[0]").value(
                        "Begin market value is 0 but end market value is non-zero: cannot compute return."));
    }

    // -----------------------------------------------------------------------
    // 400 Bad Request — currency field omitted
    // -----------------------------------------------------------------------

    @Test
    void missingCurrency_returns400_withFieldError() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId":        "PF-1006",
                                  "valuationDate":      "2026-06-14",
                                  "beginMarketValue":   1000000,
                                  "endMarketValue":     1035000,
                                  "netCashFlow":        10000,
                                  "benchmarkReturnPct": 1.8,
                                  "requestedBy":        "advisor01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("currency"))
                .andExpect(jsonPath("$.errors[0].message").isString());
    }

    // -----------------------------------------------------------------------
    // 400 Bad Request — multiple required fields omitted
    // -----------------------------------------------------------------------

    @Test
    void missingMultipleFields_returns400_withMultipleFieldErrors() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "valuationDate": "2026-06-14",
                                  "beginMarketValue": 1000000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThan(1)));
    }
}
