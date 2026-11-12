package com.cvexpert.ofertas.adapter.in.web;

import com.cvexpert.ofertas.domain.exception.InvalidRequestException;
import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.JobFilter;
import com.cvexpert.ofertas.domain.model.JobOfferDetails;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.ScoredJobOffer;
import com.cvexpert.ofertas.domain.port.in.CreateJobUseCase;
import com.cvexpert.ofertas.domain.port.in.DeleteJobUseCase;
import com.cvexpert.ofertas.domain.port.in.GetJobUseCase;
import com.cvexpert.ofertas.domain.port.in.ListJobsUseCase;
import com.cvexpert.ofertas.domain.port.in.UpdateJobUseCase;
import java.util.HashSet;
import java.util.List;
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
 * Rutas de ofertas. Listar y ver son para cualquiera y traen la compatibilidad de quien consulta;
 * crear, editar y borrar son de administración.
 */
@RestController
@RequestMapping("/jobs")
public class JobController {

    private final ListJobsUseCase listJobs;
    private final GetJobUseCase getJob;
    private final CreateJobUseCase createJob;
    private final UpdateJobUseCase updateJob;
    private final DeleteJobUseCase deleteJob;

    public JobController(ListJobsUseCase listJobs, GetJobUseCase getJob, CreateJobUseCase createJob,
            UpdateJobUseCase updateJob, DeleteJobUseCase deleteJob) {
        this.listJobs = listJobs;
        this.getJob = getJob;
        this.createJob = createJob;
        this.updateJob = updateJob;
        this.deleteJob = deleteJob;
    }

    @GetMapping
    public PageResponse<JobOfferBodies.Output> list(Caller caller,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> modality,
            @RequestParam(required = false) Integer postedSinceDays,
            @RequestParam(required = false) Integer minMatch) {
        // modality acepta "remote,hybrid" o el parámetro repetido; Spring separa las comas.
        var modalities = modality == null ? null
                : new HashSet<>(WireValues.modalities(modality.stream().filter(v -> !v.isBlank()).toList(), "modality"));
        var filter = new JobFilter(modalities, postedSinceDays, minMatch);
        return PageResponse.of(listJobs.listJobs(caller, new PageQuery(page, pageSize, search), filter),
                JobController::output);
    }

    @GetMapping("/{id}")
    public JobOfferBodies.Output get(Caller caller, @PathVariable String id) {
        return output(getJob.getJob(caller, WireValues.jobIdFromPath(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobOfferBodies.Output create(Caller caller, @RequestBody JobOfferBodies.Input body) {
        // Se revisa el rol antes que el cuerpo, para que quien no es admin reciba 403 y no 400.
        caller.requireAdmin();
        return JobOfferBodies.Output.from(createJob.createJob(caller, details(body)), null);
    }

    @PutMapping("/{id}")
    public JobOfferBodies.Output update(Caller caller, @PathVariable String id,
            @RequestBody JobOfferBodies.Input body) {
        caller.requireAdmin();
        long jobId = WireValues.jobIdFromPath(id);
        return JobOfferBodies.Output.from(updateJob.updateJob(caller, jobId, details(body)), null);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Caller caller, @PathVariable String id) {
        caller.requireAdmin();
        deleteJob.deleteJob(caller, WireValues.jobIdFromPath(id));
    }

    private static JobOfferBodies.Output output(ScoredJobOffer scored) {
        return JobOfferBodies.Output.from(scored.offer(), scored.matchScore());
    }

    private static JobOfferDetails details(JobOfferBodies.Input body) {
        if (body == null) {
            throw new InvalidRequestException("Falta el cuerpo de la solicitud");
        }
        return body.toDetails();
    }
}
