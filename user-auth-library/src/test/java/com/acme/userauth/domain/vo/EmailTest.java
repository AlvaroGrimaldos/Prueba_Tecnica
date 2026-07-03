package com.acme.userauth.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

    @Test
    void normalizesToTrimmedLowercase() {
        assertThat(new Email("  Andres@Acme.COM ").value()).isEqualTo("andres@acme.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sin-arroba", "a@b", "a@.com", "@acme.com", "a b@acme.com"})
    void rejectsInvalidValues(String raw) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Email(raw));
    }
}
