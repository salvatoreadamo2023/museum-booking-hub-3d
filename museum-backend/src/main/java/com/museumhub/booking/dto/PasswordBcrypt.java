package com.museumhub.booking.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * La password deve stare nei 72 byte che BCrypt usa davvero. Il limite e' in byte UTF-8, non in caratteri:
 * una lettera accentata occupa 2 byte e un'emoji 4, quindi 72 caratteri possono superarlo.
 * Oltre i 72 byte BCrypt ignorerebbe il resto della password senza dirlo.
 * Un valore assente o vuoto e' accettato: l'obbligo lo controllano @NotBlank o le altre regole del campo.
 */
@Documented
@Constraint(validatedBy = PasswordBcryptValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordBcrypt {

    String message() default "La password è troppo lunga: al massimo 72 byte (lettere accentate e simboli ne occupano più di uno)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
