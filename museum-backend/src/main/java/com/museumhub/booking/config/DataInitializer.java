package com.museumhub.booking.config;

import com.museumhub.booking.entity.Ruolo;
import com.museumhub.booking.entity.StatoAccount;
import com.museumhub.booking.entity.Tariffa;
import com.museumhub.booking.entity.User;
import com.museumhub.booking.repository.TariffaRepository;
import com.museumhub.booking.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Dati minimi per far partire il sistema, creati solo se mancano (nessun duplicato ai riavvii):
 * le tariffe e il primo amministratore, letto dal file .env. Tutto il resto lo inserisce l'admin.
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TariffaRepository tariffaRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminNome;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           TariffaRepository tariffaRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.admin.nome}") String adminNome,
                           @Value("${app.admin.email}") String adminEmail,
                           @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.tariffaRepository = tariffaRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminNome = adminNome;
        this.adminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        creaTariffe();
        creaAdmin();
    }

    private void creaTariffe() {
        if (tariffaRepository.count() > 0) {
            return;
        }
        tariffaRepository.save(Tariffa.builder().nome("Intero").scontoPercentuale(0).predefinita(true).build());
        tariffaRepository.save(Tariffa.builder().nome("Under 16").scontoPercentuale(50).predefinita(false).build());
        log.info("Create le tariffe iniziali");
    }

    private void creaAdmin() {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        if (adminPassword == null || adminPassword.length() < 8) {
            throw new IllegalStateException("ADMIN_PASSWORD mancante o più corta di 8 caratteri nel file .env");
        }
        userRepository.save(User.builder()
                .nome(adminNome)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .ruolo(Ruolo.ADMIN)
                .stato(StatoAccount.ATTIVO)
                .build());
        log.info("Creato l'amministratore iniziale: {}", adminEmail);
    }
}