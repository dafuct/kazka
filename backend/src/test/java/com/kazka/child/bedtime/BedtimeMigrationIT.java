package com.kazka.child.bedtime;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class BedtimeMigrationIT extends AbstractIT {

    @Autowired JdbcTemplate jdbc;

    @Test
    void should_create_bedtime_schedules_table_with_expected_columns() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'bedtime_schedules'",
                Integer.class);
        assertThat(count).isEqualTo(11);
    }

    @Test
    void should_have_due_index_for_sweep_query() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics " +
                "WHERE table_schema = DATABASE() AND table_name = 'bedtime_schedules' " +
                "AND index_name = 'idx_bedtime_schedules_due'",
                Integer.class);
        assertThat(count).isGreaterThan(0);
    }

    @Test
    void should_cascade_delete_when_child_profile_removed() {
        String rule = jdbc.queryForObject(
                "SELECT DELETE_RULE FROM information_schema.referential_constraints " +
                "WHERE constraint_schema = DATABASE() AND constraint_name = 'fk_bedtime_schedules_profile'",
                String.class);
        assertThat(rule).isEqualTo("CASCADE");
    }
}
