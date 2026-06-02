package com.example.companysearch.api;

import com.example.companysearch.service.InvalidSearchRequestException;
import com.example.companysearch.scraper.ScrapingException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({InvalidSearchRequestException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(Exception exception) {
        return new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage()
        );
    }

    @ExceptionHandler(ScrapingException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse scrapingFailed(ScrapingException exception) {
        return new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_GATEWAY.value(),
                HttpStatus.BAD_GATEWAY.getReasonPhrase(),
                exception.getMessage()
        );
    }
}
