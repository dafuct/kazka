package com.kazka.story;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class Stories017MigrationIT extends AbstractIT {

    @Autowired JdbcTemplate jdbc;

    @Test
    void should_add_translated_content_nullable_text() {
        String isNullable = jdbc.queryForObject(
                "SELECT IS_NULLABLE FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'stories' " +
                "AND column_name = 'translated_content'",
                String.class);
        assertThat(isNullable).isEqualTo("YES");
    }

    @Test
    void should_add_translated_language_nullable_varchar2() {
        Integer maxLength = jdbc.queryForObject(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'stories' " +
                "AND column_name = 'translated_language'",
                Integer.class);
        assertThat(maxLength).isEqualTo(2);
    }
}
