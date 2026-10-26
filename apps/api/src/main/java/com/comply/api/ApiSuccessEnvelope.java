package com.comply.api;

public record ApiSuccessEnvelope<T>(T data, ApiResponseMetadata meta) {}
