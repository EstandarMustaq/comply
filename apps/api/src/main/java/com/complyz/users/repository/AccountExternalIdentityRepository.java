package com.complyz.users.repository;

import com.complyz.users.entity.AccountExternalIdentity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Global identity lookup is only for verified issuer/subject claims; never link by email. */
public interface AccountExternalIdentityRepository extends Repository<AccountExternalIdentity, UUID> {
    AccountExternalIdentity save(AccountExternalIdentity identity);
    Optional<AccountExternalIdentity> findByIdAndAccountId(UUID id, UUID accountId);

    @Query("select i from AccountExternalIdentity i where i.issuer = :issuer and i.subject = :subject and i.unlinkedAt is null and i.linkedAt <= :at")
    Optional<AccountExternalIdentity> findLinkedByIssuerAndSubject(String issuer, String subject, Instant at);
}
