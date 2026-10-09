package com.comply.users.repository;

import com.comply.users.entity.AccountSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Metadata lifecycle queries; not a substitute for token, membership or effective-policy
 * validation.
 */
public interface AccountSessionRepository extends Repository<AccountSession, UUID> {
  /**
   * Persists session lifecycle metadata containing only a fingerprint of the provider session
   * identifier.
   *
   * @param session session metadata to persist
   * @return the saved entity; use this instance for subsequent changes
   */
  AccountSession save(AccountSession session);

  /**
   * Finds lifecycle metadata only within the supplied Account, including ended records. Callers
   * must derive the Account from authenticated identity context.
   *
   * @param id persistent record UUID
   * @param accountId Account UUID; repository callers must obtain it from authenticated identity
   *     context
   * @return the matching record, or empty when no record matches the query
   */
  Optional<AccountSession> findByIdAndAccountId(UUID id, UUID accountId);

  /**
   * Lists unrevoked sessions whose creation is at or before the supplied time and expiry is
   * strictly after it. The identity link must have started and remain linked; this metadata query
   * does not validate tokens or policies. Callers must derive the Account from authenticated
   * identity context.
   *
   * @param accountId Account UUID; repository callers must obtain it from authenticated identity
   *     context
   * @param at non-null evaluation time
   * @return matching records, or an empty list
   */
  @Query(
      """
            select s from AccountSession s, AccountExternalIdentity i
            where s.externalIdentityId = i.id and s.accountId = i.accountId and s.accountId = :accountId
              and i.unlinkedAt is null and s.revokedAt is null and i.linkedAt <= :at
              and s.createdAt <= :at and s.expiresAt > :at
            """)
  List<AccountSession> findActiveByAccountId(UUID accountId, Instant at);
}
