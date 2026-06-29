package com.portfolio.performance.dto;

import java.util.List;

public class DailyReturnResponse {

    private String portfolioId;
    private String valuationDate;
    private Double portfolioReturnPct;   // null when status is INVALID_INPUT
    private Double benchmarkReturnPct;
    private Double excessReturnPct;      // null when status is INVALID_INPUT
    private ReturnStatus status;
    private List<String> reasons;
    private String processedAt;

    private DailyReturnResponse() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DailyReturnResponse response = new DailyReturnResponse();

        public Builder portfolioId(String portfolioId) {
            response.portfolioId = portfolioId;
            return this;
        }
        public Builder valuationDate(String valuationDate) {
            response.valuationDate = valuationDate;
            return this;
        }
        public Builder portfolioReturnPct(Double portfolioReturnPct) {
            response.portfolioReturnPct = portfolioReturnPct;
            return this;
        }
        public Builder benchmarkReturnPct(Double benchmarkReturnPct) {
            response.benchmarkReturnPct = benchmarkReturnPct;
            return this;
        }
        public Builder excessReturnPct(Double excessReturnPct) {
            response.excessReturnPct = excessReturnPct;
            return this;
        }
        public Builder status(ReturnStatus status) {
            response.status = status;
            return this;
        }
        public Builder reasons(List<String> reasons) {
            response.reasons = reasons;
            return this;
        }
        public Builder processedAt(String processedAt) {
            response.processedAt = processedAt;
            return this;
        }
        public DailyReturnResponse build() {
            return response;
        }
    }

    public String getPortfolioId() { return portfolioId; }
    public String getValuationDate() { return valuationDate; }
    public Double getPortfolioReturnPct() { return portfolioReturnPct; }
    public Double getBenchmarkReturnPct() { return benchmarkReturnPct; }
    public Double getExcessReturnPct() { return excessReturnPct; }
    public ReturnStatus getStatus() { return status; }
    public List<String> getReasons() { return reasons; }
    public String getProcessedAt() { return processedAt; }
}
