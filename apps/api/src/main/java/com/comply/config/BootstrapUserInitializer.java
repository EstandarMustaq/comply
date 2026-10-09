package com.comply.config;

import com.comply.tenancy.repository.OrganizationRepository;
import com.comply.users.entity.Role;
import com.comply.users.entity.User;
import com.comply.users.repository.UserRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapUserInitializer implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(BootstrapUserInitializer.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final BootstrapProperties properties;
  private final OrganizationRepository organizationRepository;

  @Autowired
  public BootstrapUserInitializer(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      BootstrapProperties properties,
      OrganizationRepository organizationRepository) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.organizationRepository = organizationRepository;
  }

  public BootstrapUserInitializer(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      BootstrapProperties properties) {
    this(userRepository, passwordEncoder, properties, null);
  }

  @Override
  public void run(String... args) {
    if (!properties.isEnabled()) {
      log.info("User bootstrap is disabled; no initial credentials will be created.");
      return;
    }

    createUserIfConfigured(properties.getAdmin(), Role.ADMIN);
    createUserIfConfigured(properties.getOcos(), Role.OCOS);
  }

  private void createUserIfConfigured(BootstrapProperties.User configuredUser, Role role) {
    if (configuredUser == null) {
      throw new IllegalStateException("Missing bootstrap configuration for role " + role);
    }

    requireValue(configuredUser.getFullName(), role, "fullName");
    requireValue(configuredUser.getEmail(), role, "email");
    requireValue(configuredUser.getPassword(), role, "password");

    if (userRepository.existsByEmail(configuredUser.getEmail())) {
      log.info("Bootstrap user for {} already exists; no user was created.", role);
      return;
    }

    User user =
        new User(
            configuredUser.getFullName(),
            configuredUser.getEmail(),
            passwordEncoder.encode(configuredUser.getPassword()),
            role);
    if (organizationRepository != null) {
      user.setOrganization(
          organizationRepository
              .findBySlug("default")
              .orElseThrow(() -> new IllegalStateException("Default Organization not found.")));
    }
    userRepository.save(user);
    log.info("Initial user for role {} was created with the configured email.", role);
  }

  private void requireValue(String value, Role role, String field) {
    if (Objects.isNull(value) || value.isBlank()) {
      throw new IllegalStateException(
          "comply.bootstrap.enabled=true exige comply.bootstrap."
              + role.name().toLowerCase()
              + "."
              + field);
    }
  }
}
