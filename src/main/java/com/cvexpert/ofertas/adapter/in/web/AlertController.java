package com.cvexpert.ofertas.adapter.in.web;

import static com.cvexpert.ofertas.adapter.in.web.WireValues.required;

import com.cvexpert.ofertas.domain.exception.InvalidRequestException;
import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.port.in.GetAlertsUseCase;
import com.cvexpert.ofertas.domain.port.in.SaveAlertsUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code /alerts}: las alertas y el perfil de emparejamiento de quien llama. */
@RestController
@RequestMapping("/alerts")
public class AlertController {

    private final GetAlertsUseCase getAlerts;
    private final SaveAlertsUseCase saveAlerts;

    public AlertController(GetAlertsUseCase getAlerts, SaveAlertsUseCase saveAlerts) {
        this.getAlerts = getAlerts;
        this.saveAlerts = saveAlerts;
    }

    @GetMapping
    public AlertBodies.Output get(Caller caller) {
        return AlertBodies.Output.from(getAlerts.getAlerts(caller));
    }

    @PutMapping
    public AlertBodies.Output save(Caller caller, @RequestBody AlertBodies.Input body) {
        if (body == null) {
            throw new InvalidRequestException("Falta el cuerpo de la solicitud");
        }
        boolean enabled = required(body.enabled(), "enabled");
        int minMatch = required(body.minMatch(), "minMatch");
        AlertBodies.Profile profile = required(body.profile(), "profile");
        return AlertBodies.Output.from(saveAlerts.saveAlerts(caller, enabled, minMatch, profile.toDomain()));
    }
}
