package com.complyz.users.repository;

import com.complyz.users.entity.AccountExternalIdentity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Global identity lookup is only for verified issuer/subject claims; never link by email. */
public interface AccountExternalIdentityRepository extends Repository<AccountExternalIdentity, UUID> {
    /**
     * Persists identity link metadata, retaining its immutable Account and issuer/subject references.
     * @param identity external identity metadata to persist
     * @return the saved entity; use this instance for subsequent changes
     */
    AccountExternalIdentity save(AccountExternalIdentity identity);
    /**
     * Finds lifecycle metadata only within the supplied Account, including ended records.
     * Callers must derive the Account from authenticated identity context.
     * @param id persistent record UUID
     * @param accountId Account UUID; repository callers must obtain it from authenticated identity context
     * @return the matching record, or empty when no record matches the query
     */
    Optional<AccountExternalIdentity> findByIdAndAccountId(UUID id, UUID accountId);

    /**
     * Finds an exact issuer/subject link that has started and has not been unlinked.
     * This global lookup requires verified provider claims; email is never a linking key.
     * @param issuer nonblank exact issuer reference, at most 255 characters without surrounding whitespace
     * @param subject nonblank exact subject reference, at most 255 characters without surrounding whitespace
     * @param at non-null evaluation time
     * @return the matching record, or empty when no record matches the query
     */
    @Query("select i from AccountExternalIdentity i where i.issuer = :issuer and i.subject = :subject and i.unlinkedAt is null and i.linkedAt <= :at")
    Optional<AccountExternalIdentity> findLinkedByIssuerAndSubject(String issuer, String subject, Instant at);
}
