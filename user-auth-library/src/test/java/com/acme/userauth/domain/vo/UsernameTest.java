package com.acme.userauth.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UsernameTest {

    @Test
    void normalizesToTrimmedLowercase() {
        assertThat(new Username("  Andres.Dev ").value()).isEqualTo("andres.dev");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "con espacios", "acentuadoáé",
            "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"}) // 51 caracteres
    void rejectsInvalidValues(String raw) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Username(raw));
    }

    @Test
    void rejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> new Username(null));
    }
}
