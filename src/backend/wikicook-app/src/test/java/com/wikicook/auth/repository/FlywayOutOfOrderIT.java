package com.wikicook.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.support.AbstractPostgresIT;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Research R11: dev databases are already at V403 when V102-V104 arrive. A database built without those three files,
 * then migrated with {@code outOfOrder = true}, applies them and validates cleanly; without the flag Flyway refuses.
 * Runs on its own database inside the shared container, so the schema other ITs use is untouched.
 */
class FlywayOutOfOrderIT extends AbstractPostgresIT {

    private static final String DATABASE = "flyway_out_of_order";
    private static final Set<String> SPEC_010 = Set.of("V102", "V103", "V104");

    @TempDir
    Path preAuthMigrations;

    @Test
    void v102ToV104ApplyOutOfOrderAfterV403() throws Exception {
        createDatabase();
        String url = POSTGRES.getJdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/" + DATABASE + "$1");
        copyMigrationsWithout010(preAuthMigrations);

        Flyway.configure()
                .dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("filesystem:" + preAuthMigrations)
                .load()
                .migrate();

        Flyway strict = flyway(url, false);
        assertThat(java.util.Arrays.stream(strict.info().all()).filter(m -> m.getState() == MigrationState.IGNORED))
                .extracting(m -> m.getVersion().toString())
                .containsExactly("102", "103", "104");
        assertThatThrownBy(strict::validate).isInstanceOf(FlywayException.class);

        Flyway outOfOrder = flyway(url, true);
        var result = outOfOrder.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isEqualTo(3);
        assertThat(outOfOrder.info().pending()).isEmpty();
        assertThat(outOfOrder.info().applied()).extracting(MigrationInfo::getVersion)
                .extracting(Object::toString).contains("102", "103", "104", "403");
        outOfOrder.validate();
    }

    private static Flyway flyway(String url, boolean outOfOrder) {
        return Flyway.configure()
                .dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .outOfOrder(outOfOrder)
                .load();
    }

    private static void copyMigrationsWithout010(Path target) throws IOException {
        for (Resource resource : new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*.sql")) {
            String name = resource.getFilename();
            if (name != null && !SPEC_010.contains(name.substring(0, 4))) {
                Files.copy(resource.getInputStream(), target.resolve(name));
            }
        }
    }

    private static void createDatabase() throws SQLException {
        try (Connection connection = POSTGRES.createConnection("");
                Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + DATABASE);
            statement.execute("CREATE DATABASE " + DATABASE);
        }
    }
}
