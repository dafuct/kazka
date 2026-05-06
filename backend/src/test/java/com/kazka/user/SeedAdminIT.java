package com.kazka.user;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "kazka.auth.admin.email=seed-admin@example.com",
        "kazka.auth.admin.password=Seedpass1!"
})
class SeedAdminIT extends AbstractIT {

    @Autowired UserRepository users;

    @Test
    void should_createAdmin_when_envEmailAndPasswordAreSet() {
        var seeded = users.findByEmail("seed-admin@example.com").orElseThrow();
        assertThat(seeded.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(seeded.isEmailVerified()).isTrue();
    }
}
