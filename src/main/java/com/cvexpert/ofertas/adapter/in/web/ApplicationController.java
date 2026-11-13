package com.cvexpert.ofertas.adapter.in.web;

import com.cvexpert.ofertas.domain.exception.InvalidRequestException;
import com.cvexpert.ofertas.domain.model.ApplicationStatus;
import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.port.in.ApplyToJobUseCase;
import com.cvexpert.ofertas.domain.port.in.GetApplicationUseCase;
import com.cvexpert.ofertas.domain.port.in.ListApplicationsUseCase;
import com.cvexpert.ofertas.domain.port.in.MoveApplicationUseCase;
import com.cvexpert.ofertas.domain.port.in.WithdrawApplicationUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las rutas de postulaciones de la Fase I: el tablero Kanban de quien llama. Cada método traduce
 * HTTP a un caso de uso y de vuelta; las reglas viven en el dominio.
 */
@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ListApplicationsUseCase listApplications;
    private final GetApplicationUseCase getApplication;
    private final ApplyToJobUseCase applyToJob;
    private final MoveApplicationUseCase moveApplication;
    private final WithdrawApplicationUseCase withdrawApplication;

    public ApplicationController(ListApplicationsUseCase listApplications, GetApplicationUseCase getApplication,
            ApplyToJobUseCase applyToJob, MoveApplicationUseCase moveApplication,
            WithdrawApplicationUseCase withdrawApplication) {
        this.listApplications = listApplications;
        this.getApplication = getApplication;
        this.applyToJob = applyToJob;
        this.moveApplication = moveApplication;
        this.withdrawApplication = withdrawApplication;
    }

    @GetMapping
    public PageResponse<JobApplicationResponse> list(Caller caller,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search) {
        return PageResponse.of(listApplications.listApplications(caller, new PageQuery(page, pageSize, search)),
                JobApplicationResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobApplicationResponse apply(Caller caller, @RequestBody ApplicationRequests.Create body) {
        if (body == null) {
            throw new InvalidRequestException("Falta el cuerpo de la solicitud");
        }
        long jobOfferId = WireValues.idFromBody(body.jobOfferId(), "jobOfferId");
        var status = body.initialStatus() == null || body.initialStatus().isBlank()
                ? ApplicationStatus.APPLIED
                : WireValues.status(body.initialStatus());
        return JobApplicationResponse.from(applyToJob.apply(caller, jobOfferId, body.notes(), status));
    }

    @GetMapping("/{id}")
    public JobApplicationResponse get(Caller caller, @PathVariable String id) {
        return JobApplicationResponse.from(getApplication.getApplication(caller, WireValues.applicationIdFromPath(id)));
    }

    @PutMapping("/{id}")
    public JobApplicationResponse move(Caller caller, @PathVariable String id,
            @RequestBody ApplicationRequests.UpdateStatus body) {
        long applicationId = WireValues.applicationIdFromPath(id);
        return JobApplicationResponse.from(
                moveApplication.moveApplication(caller, applicationId, WireValues.status(body)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(Caller caller, @PathVariable String id) {
        withdrawApplication.withdraw(caller, WireValues.applicationIdFromPath(id));
    }
}
