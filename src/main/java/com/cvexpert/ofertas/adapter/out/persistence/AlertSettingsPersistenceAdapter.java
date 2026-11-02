package com.cvexpert.ofertas.adapter.out.persistence;

import com.cvexpert.ofertas.domain.model.AlertSettings;
import com.cvexpert.ofertas.domain.port.out.AlertSettingsRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Implementa el puerto {@link AlertSettingsRepository} con PostgreSQL. */
@Component
class AlertSettingsPersistenceAdapter implements AlertSettingsRepository {

    private final AlertSettingsJpaRepository jpa;

    AlertSettingsPersistenceAdapter(AlertSettingsJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<AlertSettings> findByUser(String userId) {
        return jpa.findById(userId).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public AlertSettings save(AlertSettings settings) {
        AlertSettingsEntity entity = jpa.findById(settings.userId()).orElseGet(AlertSettingsEntity::new);
        PersistenceMapper.copy(settings, entity);
        return PersistenceMapper.toDomain(jpa.saveAndFlush(entity));
    }

    @Override
    public List<AlertSettings> findEnabled() {
        return jpa.findByEnabledTrue().stream().map(PersistenceMapper::toDomain).toList();
    }
}
