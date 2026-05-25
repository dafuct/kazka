package com.kazka.child.bedtime;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class Bedtime015MigrationIT extends AbstractIT {

    @Autowired JdbcTemplate jdbc;

    @Test
    void should_add_holiday_themes_enabled_column_with_default_true() {
        String defaultValue = jdbc.queryForObject(
                "SELECT COLUMN_DEFAULT FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'bedtime_schedules' " +
                "AND column_name = 'holiday_themes_enabled'",
                String.class);
        // MySQL formats boolean defaults as '1' for TRUE
        assertThat(defaultValue).isIn("1", "TRUE", "true");
    }

    @Test
    void should_mark_column_not_null() {
        String isNullable = jdbc.queryForObject(
                "SELECT IS_NULLABLE FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'bedtime_schedules' " +
                "AND column_name = 'holiday_themes_enabled'",
                String.class);
        assertThat(isNullable).isEqualTo("NO");
    }
}
