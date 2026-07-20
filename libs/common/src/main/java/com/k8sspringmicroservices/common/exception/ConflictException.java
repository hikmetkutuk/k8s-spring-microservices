package com.k8sspringmicroservices.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApplicationException {

  public ConflictException(String message) {
    super(HttpStatus.CONFLICT, message);
  }
}
