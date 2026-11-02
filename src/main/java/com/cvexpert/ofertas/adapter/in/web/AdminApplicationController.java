package com.cvexpert.ofertas.adapter.in.web;

import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.port.in.AdminChangeStatusUseCase;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code PUT /admin/applications/{id}/status}: un administrador cambia el estado de la postulación
 * de otra persona. Es la única ruta que publica {@code application.status_changed}. El caso de uso
 * revisa el rol ({@code X-User-Role: admin}).
 */
@RestController
public class AdminApplicationController {

    private final AdminChangeStatusUseCase changeStatus;

    public AdminApplicationController(AdminChangeStatusUseCase changeStatus) {
        this.changeStatus = changeStatus;
    }

    @PutMapping("/admin/applications/{id}/status")
    public JobApplicationResponse changeStatus(Caller caller, @PathVariable String id,
            @RequestBody ApplicationRequests.UpdateStatus body) {
        // Se revisa el rol antes que el id, para que quien no es admin reciba 403 y no 404.
        caller.requireAdmin();
        long applicationId = WireValues.applicationIdFromPath(id);
        return JobApplicationResponse.from(
                changeStatus.changeStatus(caller, applicationId, WireValues.status(body)));
    }
}
