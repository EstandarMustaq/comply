package com.comply.settings.entity;

import com.comply.users.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "system_config")
public class SystemConfig {

  @Id
  @Column(name = "key", length = 100)
  private String key;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "value", nullable = false, columnDefinition = "jsonb")
  private String value;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by")
  private User updatedBy;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public SystemConfig() {}

  public SystemConfig(String key, String value, User updatedBy) {
    this.key = key;
    this.value = value;
    this.updatedBy = updatedBy;
    this.updatedAt = Instant.now();
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public User getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(User updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
