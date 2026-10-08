package com.complyz.users.repository;

import com.complyz.users.entity.AccountSession;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Metadata lifecycle queries; not a substitute for token, membership or effective-policy validation. */
public interface AccountSessionRepository extends Repository<AccountSession, UUID> {
    AccountSession save(AccountSession session);
    Optional<AccountSession> findByIdAndAccountId(UUID id, UUID accountId);

    @Query("""
            select s from AccountSession s, AccountExternalIdentity i
            where s.externalIdentityId = i.id and s.accountId = i.accountId and s.accountId = :accountId
              and i.unlinkedAt is null and s.revokedAt is null and i.linkedAt <= :at
              and s.createdAt <= :at and s.expiresAt > :at
            """)
    List<AccountSession> findActiveByAccountId(UUID accountId, Instant at);
}
