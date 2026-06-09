package com.kazka.story;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class NarrationMigrationIT extends AbstractIT {

    @Autowired JdbcTemplate jdbc;

    @Test
    void should_add_narration_status_notNull_default_none() {
        String columnDefault = jdbc.queryForObject(
                "SELECT COLUMN_DEFAULT FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'stories' " +
                "AND column_name = 'narration_status'",
                String.class);
        assertThat(columnDefault).isEqualTo("NONE");
    }

    @Test
    void should_add_narration_key_nullable_varchar255() {
        Integer maxLength = jdbc.queryForObject(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'stories' " +
                "AND column_name = 'narration_key'",
                Integer.class);
        assertThat(maxLength).isEqualTo(255);
    }
}
