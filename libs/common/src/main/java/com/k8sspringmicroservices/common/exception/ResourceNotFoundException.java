package com.k8sspringmicroservices.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApplicationException {

  public ResourceNotFoundException(String message) {
    super(HttpStatus.NOT_FOUND, message);
  }

  public static ResourceNotFoundException forId(String resourceName, Object id) {
    return new ResourceNotFoundException(resourceName + " not found with id: " + id);
  }
}
