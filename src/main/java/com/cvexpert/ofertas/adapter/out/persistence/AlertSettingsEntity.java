package com.cvexpert.ofertas.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Fila de {@code alert_settings}. La llave es el {@code sub} de Google: un usuario tiene una sola
 * configuración.
 */
@Entity
@Table(name = "alert_settings")
class AlertSettingsEntity {

    @Id
    String userId;

    boolean enabled;
    int minMatch;

    @JdbcTypeCode(SqlTypes.ARRAY)
    List<String> skills;

    String location;

    @JdbcTypeCode(SqlTypes.ARRAY)
    List<String> modalities;

    @JdbcTypeCode(SqlTypes.ARRAY)
    List<String> jobTypes;

    @JdbcTypeCode(SqlTypes.ARRAY)
    List<String> sectors;

    int experienceYears;
    long updatedAt;

    protected AlertSettingsEntity() {
        // Lo pide JPA.
    }
}
