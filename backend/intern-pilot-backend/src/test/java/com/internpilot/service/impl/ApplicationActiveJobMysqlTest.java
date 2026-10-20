package com.internpilot.service.impl;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationActiveJobMysqlTest {

    @Test
    void activeUserJobIsUniqueButDeletedHistoryAndOtherUsersAreAllowed() throws Exception {
        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")) {
            mysql.start();
            try (Connection connection = DriverManager.getConnection(
                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                 Statement statement = connection.createStatement()) {
                try (var version = statement.executeQuery("SELECT VERSION()")) {
                    version.next();
                    assertTrue(version.getString(1).startsWith("8.0."));
                }
                statement.execute("CREATE TABLE application_record ("
                        + "id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, "
                        + "job_id BIGINT NOT NULL, deleted TINYINT NOT NULL DEFAULT 0)");
                applyCandidateMigration(statement);

                insert(connection, 1, 10, 0);
                assertThrows(SQLException.class, () -> insert(connection, 1, 10, 0));
                insert(connection, 2, 10, 0);
                statement.executeUpdate("UPDATE application_record SET deleted = 1 WHERE user_id = 1");
                insert(connection, 1, 10, 0);
                insert(connection, 1, 10, 1);

                try (var result = statement.executeQuery("SELECT COUNT(*) FROM application_record")) {
                    result.next();
                    assertEquals(4, result.getInt(1));
                }

                // An isolated rollback rehearsal must not erase any application rows.
                statement.execute("ALTER TABLE application_record DROP INDEX uk_app_user_active_job, DROP COLUMN active_job_id");
                insert(connection, 1, 10, 0);
                // Dirty historical data is a stop condition: DDL must fail instead of silently merging rows.
                assertThrows(SQLException.class, () -> applyCandidateMigration(statement));
                statement.executeUpdate("UPDATE application_record SET deleted=1 "
                        + "WHERE user_id=1 AND job_id=10 AND deleted=0 ORDER BY id DESC LIMIT 1");
                applyCandidateMigration(statement);
                try (var result = statement.executeQuery("SELECT COUNT(*) FROM application_record")) {
                    result.next();
                    assertEquals(5, result.getInt(1));
                }
            }
        }
    }

    private void applyCandidateMigration(Statement statement) throws Exception {
        try (var stream = getClass().getResourceAsStream(
                "/sql/migration/V41__application_active_job_unique.sql")) {
            if (stream == null) {
                throw new IllegalStateException("V41 migration resource is missing");
            }
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            int start = sql.indexOf("ALTER TABLE application_record\n  ADD COLUMN");
            if (start < 0) {
                throw new IllegalStateException("V41 migration ALTER TABLE is missing");
            }
            statement.execute(sql.substring(start).trim());
        }
    }

    private void insert(Connection connection, long userId, long jobId, int deleted) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO application_record (user_id, job_id, deleted) VALUES (?, ?, ?)")) {
            statement.setLong(1, userId);
            statement.setLong(2, jobId);
            statement.setInt(3, deleted);
            statement.executeUpdate();
        }
    }
}
