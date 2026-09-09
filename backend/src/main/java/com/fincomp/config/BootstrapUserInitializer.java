package com.fincomp.config;

import com.fincomp.users.entity.Role;
import com.fincomp.users.entity.User;
import com.fincomp.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class BootstrapUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties properties;

    public BootstrapUserInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            BootstrapProperties properties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (!properties.isEnabled()) {
            log.info("Bootstrap de utilizadores desativado; nenhuma credencial inicial será criada.");
            return;
        }

        createUserIfConfigured(properties.getAdmin(), Role.ADMIN);
        createUserIfConfigured(properties.getOcos(), Role.OCOS);
    }

    private void createUserIfConfigured(BootstrapProperties.User configuredUser, Role role) {
        if (configuredUser == null) {
            throw new IllegalStateException("Configuração de bootstrap ausente para o perfil " + role);
        }

        requireValue(configuredUser.getFullName(), role, "fullName");
        requireValue(configuredUser.getEmail(), role, "email");
        requireValue(configuredUser.getPassword(), role, "password");

        if (userRepository.existsByEmail(configuredUser.getEmail())) {
            log.info("Utilizador de bootstrap para {} já existe; nenhum utilizador foi criado.", role);
            return;
        }

        User user = new User(
                configuredUser.getFullName(),
                configuredUser.getEmail(),
                passwordEncoder.encode(configuredUser.getPassword()),
                role
        );
        userRepository.save(user);
        log.info("Utilizador inicial para o perfil {} criado com o email configurado.", role);
    }

    private void requireValue(String value, Role role, String field) {
        if (Objects.isNull(value) || value.isBlank()) {
            throw new IllegalStateException(
                    "fincomp.bootstrap.enabled=true exige fincomp.bootstrap." + role.name().toLowerCase()
                            + "." + field
            );
        }
    }
}
