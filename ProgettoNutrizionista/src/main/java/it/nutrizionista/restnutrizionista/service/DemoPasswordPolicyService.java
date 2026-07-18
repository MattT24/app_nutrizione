package it.nutrizionista.restnutrizionista.service;

import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.exception.BadRequestException;

/** Policy server-side per password demo, indipendente dalla validazione UI. */
@Service
public class DemoPasswordPolicyService {
    public void valida(String password) {
        if (password == null || password.length() < 14 || password.length() > 128
                || password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().noneMatch(c -> !Character.isLetterOrDigit(c))) {
            throw new BadRequestException(
                    "La password demo deve avere 14-128 caratteri, maiuscola, minuscola, numero e simbolo");
        }
    }
}
