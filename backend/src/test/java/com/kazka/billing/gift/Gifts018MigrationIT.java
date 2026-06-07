package com.kazka.billing.gift;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class Gifts018MigrationIT extends AbstractIT {

    @Autowired JdbcTemplate jdbc;

    @Test
    void should_create_gift_codes_table_with_pk_on_code() {
        @SuppressWarnings("unchecked")
        var rows = jdbc.queryForList(
                "SELECT column_name, is_nullable, data_type FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'gift_codes' AND column_name = 'code'");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("is_nullable")).isEqualTo("NO");
    }

    @Test
    void should_have_status_column_default_available() {
        @SuppressWarnings("unchecked")
        var rows = jdbc.queryForList(
                "SELECT column_default FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'gift_codes' AND column_name = 'status'");
        assertThat(rows).hasSize(1);
        String def = rows.get(0).get("column_default").toString();
        assertThat(def).contains("AVAILABLE");
    }

    @Test
    void should_have_indexes_on_redeemed_by_and_status() {
        @SuppressWarnings("unchecked")
        var rows = jdbc.queryForList(
                "SELECT index_name FROM information_schema.statistics " +
                "WHERE table_schema = DATABASE() AND table_name = 'gift_codes'");
        var names = rows.stream().map(row -> row.get("index_name").toString()).toList();
        assertThat(names).contains("idx_gift_codes_redeemed_by", "idx_gift_codes_status");
    }
}
