package com.sowells.pay.webapp.gift.controller;

import com.sowells.pay.webapp.gift.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice(annotations = RestController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@ResponseBody
@Slf4j
public class GlobalRestExceptionHandler extends ResponseEntityExceptionHandler {
  @ExceptionHandler(BadRequestException.class)
  protected ResponseEntity<Object> handleBadRequest (Exception ex, WebRequest request) {
    log.warn("Bad request occurred with request - {}", ((ServletWebRequest)request).getRequest().getRequestURI(), ex);
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(Exception.class)
  protected ResponseEntity<Object> handleInternal (Exception ex, WebRequest request) {
    log.error("Internal error occurred with request - {}", ((ServletWebRequest)request).getRequest().getRequestURI(), ex);
    return new ResponseEntity<>("Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR);
  }
}