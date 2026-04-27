package com.kazka.story;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class StoryTableMigrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void storiesTableExists() {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'stories' AND table_schema = database()",
            Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
