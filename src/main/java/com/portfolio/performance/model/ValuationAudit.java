package com.portfolio.performance.model;

import com.portfolio.performance.dto.ReturnStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "valuation_audit")
public class ValuationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String portfolioId;
    private String valuationDate;
    private Double beginMarketValue;
    private Double endMarketValue;
    private Double netCashFlow;
    private Double benchmarkReturnPct;
    private String currency;
    private String requestedBy;

    private Double portfolioReturnPct;
    private Double excessReturnPct;

    @Enumerated(EnumType.STRING)
    private ReturnStatus status;

    private String processedAt;

    public ValuationAudit() {}

    public Long getId() { return id; }

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

    public Double getPortfolioReturnPct() { return portfolioReturnPct; }
    public void setPortfolioReturnPct(Double portfolioReturnPct) { this.portfolioReturnPct = portfolioReturnPct; }

    public Double getExcessReturnPct() { return excessReturnPct; }
    public void setExcessReturnPct(Double excessReturnPct) { this.excessReturnPct = excessReturnPct; }

    public ReturnStatus getStatus() { return status; }
    public void setStatus(ReturnStatus status) { this.status = status; }

    public String getProcessedAt() { return processedAt; }
    public void setProcessedAt(String processedAt) { this.processedAt = processedAt; }
}
