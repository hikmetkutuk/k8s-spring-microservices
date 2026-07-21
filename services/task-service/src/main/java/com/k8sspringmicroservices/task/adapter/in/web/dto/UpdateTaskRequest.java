package com.k8sspringmicroservices.task.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateTaskRequest(@NotBlank String title, String description, @Min(1) int quantity) {}
