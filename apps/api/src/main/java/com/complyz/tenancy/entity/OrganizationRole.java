package com.complyz.tenancy.entity;

/** Organization-scoped grants. Legacy User roles remain separate during expansion. */
public enum OrganizationRole {
    OWNER,
    ADMIN,
    OCOS,
    COMPLIANCE_OFFICER,
    AUDITOR_READONLY
}
