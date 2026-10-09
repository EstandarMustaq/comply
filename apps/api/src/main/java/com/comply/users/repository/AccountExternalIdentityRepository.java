package com.comply.users.repository;

import com.comply.users.entity.AccountExternalIdentity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Global identity lookup is only for verified issuer/subject claims; never link by email. */
public interface AccountExternalIdentityRepository
    extends Repository<AccountExternalIdentity, UUID> {
  AccountExternalIdentity save(AccountExternalIdentity identity);

  Optional<AccountExternalIdentity> findByIdAndAccountId(UUID id, UUID accountId);

  /**
   * Finds an exact issuer/subject link that has started and has not been unlinked. This global
   * lookup requires verified provider claims; email is never a linking key.
   */
  @Query(
      "select i from AccountExternalIdentity i where i.issuer = :issuer and i.subject = :subject and i.unlinkedAt is null and i.linkedAt <= :at")
  Optional<AccountExternalIdentity> findLinkedByIssuerAndSubject(
      String issuer, String subject, Instant at);
}
