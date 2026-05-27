package com.kazka.story;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class Stories016MigrationIT extends AbstractIT {

    @Autowired JdbcTemplate jdbc;

    @Test
    void should_add_is_branching_with_default_false() {
        String defaultValue = jdbc.queryForObject(
                "SELECT COLUMN_DEFAULT FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'stories' " +
                "AND column_name = 'is_branching'",
                String.class);
        assertThat(defaultValue).isIn("0", "FALSE", "false");
    }

    @Test
    void should_add_branching_state_with_default_complete() {
        String defaultValue = jdbc.queryForObject(
                "SELECT COLUMN_DEFAULT FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'stories' " +
                "AND column_name = 'branching_state'",
                String.class);
        assertThat(defaultValue).isEqualTo("complete");
    }

    @Test
    void should_add_pending_choices_nullable_json() {
        String isNullable = jdbc.queryForObject(
                "SELECT IS_NULLABLE FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'stories' " +
                "AND column_name = 'pending_choices'",
                String.class);
        assertThat(isNullable).isEqualTo("YES");
    }
}
