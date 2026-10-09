package com.comply.users.repository;

import com.comply.users.entity.AccountFactorReference;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Callers derive Account identity from authenticated identity context, not Organization membership.
 */
public interface AccountFactorReferenceRepository extends Repository<AccountFactorReference, UUID> {
  /**
   * Persists factor metadata; the database enforces ownership by the same Account as its identity.
   *
   * @param factor non-secret factor metadata to persist
   * @return the saved entity; use this instance for subsequent changes
   */
  AccountFactorReference save(AccountFactorReference factor);

  /**
   * Finds lifecycle metadata only within the supplied Account, including ended records. Callers
   * must derive the Account from authenticated identity context.
   *
   * @param id persistent record UUID
   * @param accountId Account UUID; repository callers must obtain it from authenticated identity
   *     context
   * @return the matching record, or empty when no record matches the query
   */
  Optional<AccountFactorReference> findByIdAndAccountId(UUID id, UUID accountId);

  /**
   * Lists unrevoked factor references registered by the supplied time whose identity link has
   * started and remains linked. Callers must derive the Account from authenticated identity
   * context.
   *
   * @param accountId Account UUID; repository callers must obtain it from authenticated identity
   *     context
   * @param at non-null evaluation time
   * @return matching records, or an empty list
   */
  @Query(
      """
            select f from AccountFactorReference f, AccountExternalIdentity i
            where f.externalIdentityId = i.id and f.accountId = i.accountId and f.accountId = :accountId
              and i.unlinkedAt is null and f.revokedAt is null
              and i.linkedAt <= :at and f.registeredAt <= :at
            """)
  List<AccountFactorReference> findActiveByAccountId(UUID accountId, Instant at);
}
