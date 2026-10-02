package com.complyz.api;

public record ApiErrorEnvelope(ApiError error, ApiResponseMeta meta) {}
