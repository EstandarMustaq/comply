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
  AccountFactorReference save(AccountFactorReference factor);

  Optional<AccountFactorReference> findByIdAndAccountId(UUID id, UUID accountId);

  /**
   * Lists unrevoked factor references registered by the supplied time whose identity link has
   * started and remains linked.
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
