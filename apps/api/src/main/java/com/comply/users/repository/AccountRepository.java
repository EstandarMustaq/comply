package com.comply.users.repository;

import com.comply.users.entity.Account;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Global identity records, owned by Identity &amp; Access; not a customer directory. */
public interface AccountRepository extends Repository<Account, UUID> {
  Account save(Account account);

  Optional<Account> findById(UUID id);

  Optional<Account> findByLegacyUserId(UUID legacyUserId);
}
