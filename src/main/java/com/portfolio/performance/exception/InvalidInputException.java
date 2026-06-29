package com.portfolio.performance.exception;

import com.portfolio.performance.dto.DailyReturnResponse;

public class InvalidInputException extends RuntimeException {

    private final DailyReturnResponse response;

    public InvalidInputException(DailyReturnResponse response) {
        super("Invalid input: " + response.getReasons());
        this.response = response;
    }

    public DailyReturnResponse getResponse() {
        return response;
    }
}
