package com.portfolio.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DailyReturnRequest {

    @NotBlank(message = "portfolioId is required")
    private String portfolioId;

    @NotBlank(message = "valuationDate is required")
    private String valuationDate;

    @NotNull(message = "beginMarketValue is required")
    private Double beginMarketValue;

    @NotNull(message = "endMarketValue is required")
    private Double endMarketValue;

    @NotNull(message = "netCashFlow is required")
    private Double netCashFlow;

    @NotNull(message = "benchmarkReturnPct is required")
    private Double benchmarkReturnPct;

    @NotBlank(message = "currency is required")
    private String currency;

    @NotBlank(message = "requestedBy is required")
    private String requestedBy;

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public String getValuationDate() { return valuationDate; }
    public void setValuationDate(String valuationDate) { this.valuationDate = valuationDate; }

    public Double getBeginMarketValue() { return beginMarketValue; }
    public void setBeginMarketValue(Double beginMarketValue) { this.beginMarketValue = beginMarketValue; }

    public Double getEndMarketValue() { return endMarketValue; }
    public void setEndMarketValue(Double endMarketValue) { this.endMarketValue = endMarketValue; }

    public Double getNetCashFlow() { return netCashFlow; }
    public void setNetCashFlow(Double netCashFlow) { this.netCashFlow = netCashFlow; }

    public Double getBenchmarkReturnPct() { return benchmarkReturnPct; }
    public void setBenchmarkReturnPct(Double benchmarkReturnPct) { this.benchmarkReturnPct = benchmarkReturnPct; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
}
