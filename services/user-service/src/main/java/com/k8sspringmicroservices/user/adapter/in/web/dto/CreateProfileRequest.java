package com.k8sspringmicroservices.user.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProfileRequest(
    @NotBlank @Size(max = 100) String displayName, @Size(max = 500) String bio, String avatarUrl) {}
