package com.complyz.api;

public record ApiSuccessEnvelope<T>(T data, ApiResponseMetadata meta) {}
