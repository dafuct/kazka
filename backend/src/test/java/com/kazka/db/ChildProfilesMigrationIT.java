package com.kazka.db;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class ChildProfilesMigrationIT extends AbstractIT {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void should_create_child_profiles_table_with_expected_columns() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'child_profiles'",
                Integer.class);
        assertThat(count).isEqualTo(11);
    }

    @Test
    void should_create_characters_table_with_unique_profile_name() {
        Integer unique = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics " +
                "WHERE table_schema = DATABASE() AND table_name = 'characters' " +
                "AND index_name = 'uk_characters_profile_name'",
                Integer.class);
        assertThat(unique).isGreaterThan(0);
    }

    @Test
    void should_add_child_profile_id_and_extraction_status_to_stories() {
        Integer cpid = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'stories' " +
                "AND column_name = 'child_profile_id'",
                Integer.class);
        Integer estatus = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'stories' " +
                "AND column_name = 'extraction_status'",
                Integer.class);
        assertThat(cpid).isEqualTo(1);
        assertThat(estatus).isEqualTo(1);
    }

    @Test
    void should_create_story_characters_join_table() {
        Integer cols = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'story_characters'",
                Integer.class);
        assertThat(cols).isEqualTo(3);
    }
}
