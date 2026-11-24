package com.museumhub.booking.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

/** Controlla la regola di {@link PasswordBcrypt}: al massimo 72 byte in UTF-8. */
public class PasswordBcryptValidator implements ConstraintValidator<PasswordBcrypt, String> {

    public static final int MAX_BYTE = 72;

    public static boolean entroIlLimite(String password) {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTE;
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        return entroIlLimite(password);
    }
}
