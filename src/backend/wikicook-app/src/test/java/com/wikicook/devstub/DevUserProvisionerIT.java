package com.wikicook.devstub;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** ai_requests.user_id references users, so the dev user must exist; no migration adds it. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DevUserProvisionerIT extends AbstractPostgresIT {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void insertsTheUserOnceWithTheGivenId() {
        DevUserProvisioner provisioner = new DevUserProvisioner(jdbc);

        provisioner.ensure(4242);
        provisioner.ensure(4242);
        new DevUserProvisioner(jdbc).ensure(4242);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE id = 4242", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT email FROM users WHERE id = 4242", String.class))
                .isEqualTo("dev-user-4242@wikicook.local");
    }

    @Test
    void laterSignUpsDoNotCollideWithTheDevUser() {
        new DevUserProvisioner(jdbc).ensure(5000);

        Long next = jdbc.queryForObject(
                "INSERT INTO users (email, display_name) VALUES ('someone@example.com', 'Someone') RETURNING id",
                Long.class);

        assertThat(next).isGreaterThan(5000);
    }

    @Test
    void keepsAnExistingUser() {
        Long id = jdbc.queryForObject(
                "INSERT INTO users (email, display_name) VALUES ('real@example.com', 'Real') RETURNING id", Long.class);

        new DevUserProvisioner(jdbc).ensure(id);

        assertThat(jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, id))
                .isEqualTo("real@example.com");
    }
}
