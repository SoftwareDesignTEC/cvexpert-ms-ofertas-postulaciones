package com.cvexpert.ofertas.domain.port.out;

import com.cvexpert.ofertas.domain.model.AlertSettings;
import java.util.List;
import java.util.Optional;

/** Dónde vive la configuración de alertas y el perfil de emparejamiento de cada usuario. */
public interface AlertSettingsRepository {

    Optional<AlertSettings> findByUser(String userId);

    /** Crea o reemplaza la configuración del usuario. */
    AlertSettings save(AlertSettings settings);

    /** Las configuraciones con alertas activas: son los candidatos a recibir {@code job.new}. */
    List<AlertSettings> findEnabled();
}
