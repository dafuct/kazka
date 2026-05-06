package com.kazka.story;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class StoryTableMigrationTest extends AbstractIT {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void storiesTableExists() {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'stories' AND table_schema = database()",
            Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
