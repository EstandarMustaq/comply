package com.fincomp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincomp.sanctions.entity.SanctionsEntityType;
import com.fincomp.sanctions.entity.SanctionsListEntry;
import com.fincomp.sanctions.entity.SanctionsSourceList;
import com.fincomp.sanctions.repository.SanctionsListRepository;
import com.fincomp.users.entity.Role;
import com.fincomp.users.entity.User;
import com.fincomp.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SanctionsListRepository sanctionsListRepository;
    private final ObjectMapper objectMapper;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SanctionsListRepository sanctionsListRepository,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sanctionsListRepository = sanctionsListRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@fincomp.co.mz")) {
            User admin = new User(
                    "Administrador do Sistema",
                    "admin@fincomp.co.mz",
                    passwordEncoder.encode("Admin@FinComp2024!"),
                    Role.ADMIN
            );
            userRepository.save(admin);
            log.info("Utilizador ADMIN padrão criado: admin@fincomp.co.mz");
        }

        if (!userRepository.existsByEmail("ocos@fincomp.co.mz")) {
            User ocos = new User(
                    "Oficial de Cumprimento (OCOS)",
                    "ocos@fincomp.co.mz",
                    passwordEncoder.encode("Ocos@FinComp2024!"),
                    Role.OCOS
            );
            userRepository.save(ocos);
            log.info("Utilizador OCOS padrão criado: ocos@fincomp.co.mz");
        }

        if (sanctionsListRepository.count() == 0) {
            try (var is = getClass().getResourceAsStream("/sanctions_nacional_seed.json")) {
                if (is != null) {
                    List<Map<String, String>> entries = objectMapper.readValue(is, List.class);
                    for (Map<String, String> entry : entries) {
                        SanctionsListEntry sle = new SanctionsListEntry(
                                entry.get("fullName"),
                                SanctionsEntityType.valueOf(entry.get("entityType")),
                                SanctionsSourceList.valueOf(entry.get("sourceList")),
                                entry.get("rawReference")
                        );
                        sanctionsListRepository.save(sle);
                    }
                    log.info("Carga inicial de sanções concluída: {} registos carregados (Lista Nacional e ONU)", entries.size());
                }
            } catch (Exception e) {
                log.error("Erro ao carregar seed de sanções", e);
            }
        }
    }
}
