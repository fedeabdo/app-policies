package com.reservas.whatsapp.config;

import com.reservas.whatsapp.model.UserSession;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseConstraintSyncConfig {

    private static final String TABLE_NAME = "user_sessions";
    private static final String CONSTRAINT_NAME = "user_sessions_state_check";

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void syncUserSessionStateConstraint() {
        try {
            String constraintDef = jdbcTemplate.query(
                    """
                    SELECT pg_get_constraintdef(c.oid)
                    FROM pg_constraint c
                    JOIN pg_class t ON c.conrelid = t.oid
                    WHERE t.relname = ? AND c.conname = ?
                    """,
                    rs -> rs.next() ? rs.getString(1) : null,
                    TABLE_NAME,
                    CONSTRAINT_NAME
            );

            String expectedStatesSql = Arrays.stream(UserSession.ConversationState.values())
                    .map(Enum::name)
                    .map(state -> "'" + state + "'")
                    .collect(Collectors.joining(", "));

            boolean needsUpdate = constraintDef == null
                    || Arrays.stream(UserSession.ConversationState.values())
                    .map(Enum::name)
                    .anyMatch(state -> !constraintDef.contains("'" + state + "'"));

            if (!needsUpdate) {
                log.info("Constraint {} ya está sincronizado", CONSTRAINT_NAME);
                return;
            }

            log.warn("Actualizando constraint {} para incluir estados actuales de UserSession", CONSTRAINT_NAME);

            jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " DROP CONSTRAINT IF EXISTS " + CONSTRAINT_NAME);
            jdbcTemplate.execute(
                    "ALTER TABLE " + TABLE_NAME + " ADD CONSTRAINT " + CONSTRAINT_NAME
                            + " CHECK (state IN (" + expectedStatesSql + "))"
            );

            log.info("Constraint {} actualizado correctamente", CONSTRAINT_NAME);
        } catch (DataAccessException ex) {
            log.error("No se pudo sincronizar el constraint {}: {}", CONSTRAINT_NAME, ex.getMessage());
        }
    }
}
