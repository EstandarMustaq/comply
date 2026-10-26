package com.comply.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.comply.users.entity.Role;
import com.comply.users.entity.User;
import com.comply.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BootstrapUserInitializerTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Test
  void doesNotCreateUsersWhenBootstrapIsDisabled() {
    BootstrapProperties properties = new BootstrapProperties();
    BootstrapUserInitializer initializer =
        new BootstrapUserInitializer(userRepository, passwordEncoder, properties);

    initializer.run();

    verifyNoInteractions(userRepository, passwordEncoder);
  }

  @Test
  void rejectsEnabledBootstrapWithMissingCredentials() {
    BootstrapProperties properties = new BootstrapProperties();
    properties.setEnabled(true);
    BootstrapUserInitializer initializer =
        new BootstrapUserInitializer(userRepository, passwordEncoder, properties);

    IllegalStateException exception = assertThrows(IllegalStateException.class, initializer::run);

    assertEquals(
        "comply.bootstrap.enabled=true exige comply.bootstrap.admin.fullName",
        exception.getMessage());
    verifyNoInteractions(userRepository, passwordEncoder);
  }

  @Test
  void createsConfiguredUsersWithoutLoggingTheirPasswords() {
    BootstrapProperties properties = new BootstrapProperties();
    properties.setEnabled(true);
    properties.getAdmin().setFullName("Admin Teste");
    properties.getAdmin().setEmail("admin@example.test");
    properties.getAdmin().setPassword("temporary-admin-password");
    properties.getOcos().setFullName("OCOS Teste");
    properties.getOcos().setEmail("ocos@example.test");
    properties.getOcos().setPassword("temporary-ocos-password");

    when(userRepository.existsByEmail(any())).thenReturn(false);
    when(passwordEncoder.encode(any()))
        .thenAnswer(invocation -> "encoded:" + invocation.getArgument(0));

    BootstrapUserInitializer initializer =
        new BootstrapUserInitializer(userRepository, passwordEncoder, properties);
    initializer.run();

    ArgumentCaptor<User> users = ArgumentCaptor.forClass(User.class);
    verify(userRepository, times(2)).save(users.capture());
    verify(passwordEncoder).encode("temporary-admin-password");
    verify(passwordEncoder).encode("temporary-ocos-password");
    assertEquals(Role.ADMIN, users.getAllValues().get(0).getRole());
    assertEquals(Role.OCOS, users.getAllValues().get(1).getRole());
    assertEquals("encoded:temporary-admin-password", users.getAllValues().get(0).getPasswordHash());
    assertEquals("encoded:temporary-ocos-password", users.getAllValues().get(1).getPasswordHash());
  }
}
