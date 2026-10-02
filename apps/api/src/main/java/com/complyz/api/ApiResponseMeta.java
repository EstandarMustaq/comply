package com.complyz.api;

import java.time.Instant;

public record ApiResponseMeta(Instant timestamp, String requestId) {}
