package com.comply.users.repository;

import com.comply.users.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  @Query("SELECT user FROM User user WHERE user.organization.id = :organizationId")
  List<User> findAllByOrganizationId(@Param("organizationId") UUID organizationId);

  @Query(
      "SELECT user FROM User user WHERE user.id = :userId "
          + "AND user.organization.id = :organizationId")
  Optional<User> findByIdAndOrganizationId(
      @Param("userId") UUID userId, @Param("organizationId") UUID organizationId);
}
