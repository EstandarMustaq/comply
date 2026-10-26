package com.comply.api;

public record ApiErrorEnvelope(ApiError error, ApiResponseMetadata meta) {}
