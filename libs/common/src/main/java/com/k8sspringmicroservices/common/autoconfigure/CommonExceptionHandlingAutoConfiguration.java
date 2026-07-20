package com.k8sspringmicroservices.common.autoconfigure;

import com.k8sspringmicroservices.common.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class CommonExceptionHandlingAutoConfiguration {}
