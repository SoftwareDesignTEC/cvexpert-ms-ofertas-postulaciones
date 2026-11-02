package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.AlertSettings;
import com.cvexpert.ofertas.domain.model.Caller;

/** {@code GET /alerts}. Si el usuario nunca guardó su configuración, no existe (404). */
public interface GetAlertsUseCase {

    AlertSettings getAlerts(Caller caller);
}
