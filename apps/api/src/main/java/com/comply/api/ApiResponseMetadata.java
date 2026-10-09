package com.comply.api;

import java.time.Instant;

public record ApiResponseMetadata(Instant timestamp, String requestId) {}
