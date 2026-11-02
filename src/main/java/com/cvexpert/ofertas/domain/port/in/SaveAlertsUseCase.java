package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.AlertSettings;
import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.MatchingProfile;

/**
 * {@code PUT /alerts}: guarda las alertas y el perfil de emparejamiento de quien llama. Los clientes
 * lo mandan al guardar las alertas y cada vez que el usuario cambia su perfil.
 */
public interface SaveAlertsUseCase {

    AlertSettings saveAlerts(Caller caller, boolean enabled, int minMatch, MatchingProfile profile);
}
