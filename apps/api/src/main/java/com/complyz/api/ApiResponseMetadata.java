package com.complyz.api;

import java.time.Instant;

public record ApiResponseMetadata(Instant timestamp, String requestId) {}
