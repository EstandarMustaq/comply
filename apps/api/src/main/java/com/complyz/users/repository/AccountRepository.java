package com.complyz.users.repository;

import com.complyz.users.entity.Account;
import org.springframework.data.repository.Repository;
import java.util.Optional;
import java.util.UUID;

/** Global identity records, owned by Identity &amp; Access; not a customer directory. */
public interface AccountRepository extends Repository<Account, UUID> {
    /**
     * Persists a global Account profile without creating memberships or credentials.
     * @param account Account profile to persist
     * @return the saved entity; use this instance for subsequent changes
     */
    Account save(Account account);
    /**
     * Finds a global Account profile by UUID; this lookup does not establish tenant access.
     * @param id persistent record UUID
     * @return the matching record, or empty when no record matches the query
     */
    Optional<Account> findById(UUID id);
    /**
     * Finds the Account created for a preserved legacy User identity.
     * @param legacyUserId preserved legacy User UUID
     * @return the matching record, or empty when no record matches the query
     */
    Optional<Account> findByLegacyUserId(UUID legacyUserId);
}
