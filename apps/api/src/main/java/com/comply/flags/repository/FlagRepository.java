package com.comply.flags.repository;

import com.comply.flags.entity.Flag;
import com.comply.flags.entity.FlagStatus;
import com.comply.flags.entity.FlagType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FlagRepository extends JpaRepository<Flag, UUID> {

  @Query(
      "SELECT COUNT(flagEntity) FROM Flag flagEntity "
          + "LEFT JOIN flagEntity.client directClient "
          + "LEFT JOIN flagEntity.transaction relatedTransaction "
          + "LEFT JOIN relatedTransaction.client transactionClient "
          + "WHERE directClient.id = :clientId "
          + "AND directClient.organization.id = :organizationId "
          + "AND (relatedTransaction IS NULL OR transactionClient.organization.id = :organizationId) "
          + "AND flagEntity.type = :type AND flagEntity.status = :status")
  long countByClientIdAndOrganizationIdAndTypeAndStatus(
      @Param("clientId") UUID clientId,
      @Param("organizationId") UUID organizationId,
      @Param("type") FlagType type,
      @Param("status") FlagStatus status);

  @Query(
      "SELECT COUNT(flagEntity) FROM Flag flagEntity "
          + "LEFT JOIN flagEntity.client directClient "
          + "LEFT JOIN flagEntity.transaction relatedTransaction "
          + "LEFT JOIN relatedTransaction.client transactionClient "
          + "WHERE relatedTransaction.id = :transactionId "
          + "AND transactionClient.organization.id = :organizationId "
          + "AND (directClient IS NULL OR directClient.organization.id = :organizationId) "
          + "AND flagEntity.type = :type")
  long countByTransactionIdAndOrganizationIdAndType(
      @Param("transactionId") UUID transactionId,
      @Param("organizationId") UUID organizationId,
      @Param("type") FlagType type);

  @Query(
      """
            SELECT flagEntity FROM Flag flagEntity
            LEFT JOIN flagEntity.client directClient
            LEFT JOIN flagEntity.transaction relatedTransaction
            LEFT JOIN relatedTransaction.client transactionClient
            LEFT JOIN flagEntity.reviewedBy reviewer
            WHERE (
                (directClient IS NOT NULL AND directClient.organization.id = :organizationId
                    AND (relatedTransaction IS NULL OR transactionClient.organization.id = :organizationId))
                OR (directClient IS NULL AND relatedTransaction IS NOT NULL
                    AND transactionClient.organization.id = :organizationId)
            )
              AND (reviewer IS NULL OR reviewer.organization.id = :organizationId)
              AND (:status IS NULL OR flagEntity.status = :status)
              AND (:type IS NULL OR flagEntity.type = :type)
            """)
  List<Flag> findAllByOrganizationId(
      @Param("organizationId") UUID organizationId,
      @Param("status") FlagStatus status,
      @Param("type") FlagType type,
      Sort sort);

  @Query(
      """
            SELECT flagEntity FROM Flag flagEntity
            LEFT JOIN flagEntity.client directClient
            LEFT JOIN flagEntity.transaction relatedTransaction
            LEFT JOIN relatedTransaction.client transactionClient
            LEFT JOIN flagEntity.reviewedBy reviewer
            WHERE flagEntity.id = :flagId AND (
                (directClient IS NOT NULL AND directClient.organization.id = :organizationId
                    AND (relatedTransaction IS NULL OR transactionClient.organization.id = :organizationId))
                OR (directClient IS NULL AND relatedTransaction IS NOT NULL
                    AND transactionClient.organization.id = :organizationId)
            )
              AND (reviewer IS NULL OR reviewer.organization.id = :organizationId)
            """)
  Optional<Flag> findByIdAndOrganizationId(
      @Param("flagId") UUID flagId, @Param("organizationId") UUID organizationId);

  @Query(
      """
            SELECT COUNT(flagEntity) FROM Flag flagEntity
            LEFT JOIN flagEntity.client directClient
            LEFT JOIN flagEntity.transaction relatedTransaction
            LEFT JOIN relatedTransaction.client transactionClient
            WHERE (
                (directClient IS NOT NULL AND directClient.organization.id = :organizationId
                    AND (relatedTransaction IS NULL OR transactionClient.organization.id = :organizationId))
                OR (directClient IS NULL AND relatedTransaction IS NOT NULL
                    AND transactionClient.organization.id = :organizationId)
            ) AND flagEntity.status = :status
            """)
  long countByOrganizationIdAndStatus(
      @Param("organizationId") UUID organizationId, @Param("status") FlagStatus status);

  @Query(
      """
            SELECT COUNT(flagEntity) FROM Flag flagEntity
            LEFT JOIN flagEntity.client directClient
            LEFT JOIN flagEntity.transaction relatedTransaction
            LEFT JOIN relatedTransaction.client transactionClient
            WHERE (
                (directClient IS NOT NULL AND directClient.organization.id = :organizationId
                    AND (relatedTransaction IS NULL OR transactionClient.organization.id = :organizationId))
                OR (directClient IS NULL AND relatedTransaction IS NOT NULL
                    AND transactionClient.organization.id = :organizationId)
            ) AND flagEntity.type = :type
            """)
  long countByOrganizationIdAndType(
      @Param("organizationId") UUID organizationId, @Param("type") FlagType type);

  @Query(
      """
            SELECT COUNT(flagEntity) FROM Flag flagEntity
            LEFT JOIN flagEntity.client directClient
            LEFT JOIN flagEntity.transaction relatedTransaction
            LEFT JOIN relatedTransaction.client transactionClient
            WHERE (
                (directClient IS NOT NULL AND directClient.organization.id = :organizationId
                    AND (relatedTransaction IS NULL OR transactionClient.organization.id = :organizationId))
                OR (directClient IS NULL AND relatedTransaction IS NOT NULL
                    AND transactionClient.organization.id = :organizationId)
            ) AND flagEntity.status = :status
              AND flagEntity.slaDeadline IS NOT NULL AND flagEntity.slaDeadline < :now
            """)
  long countOverdueSlaByOrganizationId(
      @Param("organizationId") UUID organizationId,
      @Param("status") FlagStatus status,
      @Param("now") Instant now);
}
