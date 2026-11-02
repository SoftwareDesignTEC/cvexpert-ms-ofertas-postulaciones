package com.cvexpert.ofertas.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Consultas de Spring Data sobre {@code alert_settings}. */
interface AlertSettingsJpaRepository extends JpaRepository<AlertSettingsEntity, String> {

    List<AlertSettingsEntity> findByEnabledTrue();
}
