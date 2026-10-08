package com.complyz.users.repository;

import com.complyz.users.entity.Account;
import org.springframework.data.repository.Repository;
import java.util.Optional;
import java.util.UUID;

/** Global identity records, owned by Identity & Access; not a customer directory. */
public interface AccountRepository extends Repository<Account, UUID> {
    Account save(Account account);
    Optional<Account> findById(UUID id);
    Optional<Account> findByLegacyUserId(UUID legacyUserId);
}
