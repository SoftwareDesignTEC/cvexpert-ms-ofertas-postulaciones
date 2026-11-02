package com.cvexpert.ofertas.application;

import com.cvexpert.ofertas.domain.exception.NotFoundException;
import com.cvexpert.ofertas.domain.model.AlertSettings;
import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.MatchingProfile;
import com.cvexpert.ofertas.domain.port.in.GetAlertsUseCase;
import com.cvexpert.ofertas.domain.port.in.SaveAlertsUseCase;
import com.cvexpert.ofertas.domain.port.out.AlertSettingsRepository;
import java.time.Clock;

/** Casos de uso de las alertas y el perfil de emparejamiento. */
public class AlertService implements GetAlertsUseCase, SaveAlertsUseCase {

    private final AlertSettingsRepository alerts;
    private final Clock clock;

    public AlertService(AlertSettingsRepository alerts, Clock clock) {
        this.alerts = alerts;
        this.clock = clock;
    }

    @Override
    public AlertSettings getAlerts(Caller caller) {
        return alerts.findByUser(caller.userId())
                .orElseThrow(() -> new NotFoundException("ALERTS_NOT_FOUND",
                        "Todavía no guardaste tu configuración de alertas"));
    }

    @Override
    public AlertSettings saveAlerts(Caller caller, boolean enabled, int minMatch, MatchingProfile profile) {
        return alerts.save(new AlertSettings(caller.userId(), enabled, minMatch, profile, clock.millis()));
    }
}
