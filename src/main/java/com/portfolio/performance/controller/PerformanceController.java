package com.portfolio.performance.controller;

import com.portfolio.performance.dto.DailyReturnRequest;
import com.portfolio.performance.dto.DailyReturnResponse;
import com.portfolio.performance.dto.ReturnStatus;
import com.portfolio.performance.exception.InvalidInputException;
import com.portfolio.performance.service.DailyReturnService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    private final DailyReturnService dailyReturnService;

    public PerformanceController(DailyReturnService dailyReturnService) {
        this.dailyReturnService = dailyReturnService;
    }

    @PostMapping("/daily-return")
    public ResponseEntity<DailyReturnResponse> calculateDailyReturn(
            @Valid @RequestBody DailyReturnRequest request) {

        DailyReturnResponse response = dailyReturnService.calculate(request);

        if (response.getStatus() == ReturnStatus.INVALID_INPUT) {
            throw new InvalidInputException(response);
        }

        return ResponseEntity.ok(response);
    }
}
