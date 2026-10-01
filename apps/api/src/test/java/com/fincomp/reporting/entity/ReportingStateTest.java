package com.fincomp.reporting.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportingStateTest {

    @Test
    void strSubmissionIsImmutableAndRequiresGifimReference() {
        SuspiciousActivityReport report = new SuspiciousActivityReport();

        assertThatThrownBy(() -> report.markAsSubmitted(" "))
                .isInstanceOf(IllegalArgumentException.class);

        report.markAsSubmitted("GIFIM-123");
        Instant submittedAt = report.getSubmittedAt();

        assertThat(report.isSubmitted()).isTrue();
        assertThat(report.getGifimRegistrationNumber()).isEqualTo("GIFIM-123");
        assertThat(report.getRetentionUntil()).isNotNull();
        assertThatThrownBy(() -> report.markAsSubmitted("GIFIM-456"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(report.getSubmittedAt()).isEqualTo(submittedAt);
    }

    @Test
    void ctrSubmissionIsImmutable() {
        CurrencyTransactionReport report = new CurrencyTransactionReport();

        report.markAsSubmitted();

        assertThat(report.isSubmitted()).isTrue();
        assertThat(report.getRetentionUntil()).isNotNull();
        assertThatThrownBy(report::markAsSubmitted)
                .isInstanceOf(IllegalStateException.class);
    }
}
