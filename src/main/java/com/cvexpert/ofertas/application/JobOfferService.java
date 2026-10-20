package com.cvexpert.ofertas.application;

import com.cvexpert.ofertas.domain.exception.NotFoundException;
import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.JobOfferDetails;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.PageResult;
import com.cvexpert.ofertas.domain.port.in.CreateJobUseCase;
import com.cvexpert.ofertas.domain.port.in.DeleteJobUseCase;
import com.cvexpert.ofertas.domain.port.in.GetJobUseCase;
import com.cvexpert.ofertas.domain.port.in.ListJobsUseCase;
import com.cvexpert.ofertas.domain.port.in.UpdateJobUseCase;
import com.cvexpert.ofertas.domain.port.out.JobOfferRepository;
import java.time.Clock;

/**
 * Casos de uso de las ofertas. Cuando exista la compatibilidad, aquí se le suma
 * {@code matchScore} a cada oferta según el perfil de emparejamiento de quien llama.
 */
public class JobOfferService
        implements ListJobsUseCase, GetJobUseCase, CreateJobUseCase, UpdateJobUseCase, DeleteJobUseCase {

    private final JobOfferRepository offers;
    private final Clock clock;

    public JobOfferService(JobOfferRepository offers, Clock clock) {
        this.offers = offers;
        this.clock = clock;
    }

    @Override
    public PageResult<JobOffer> listJobs(Caller caller, PageQuery query) {
        return offers.search(query);
    }

    @Override
    public JobOffer getJob(Caller caller, long id) {
        return offers.findById(id).orElseThrow(() -> NotFoundException.jobOffer(id));
    }

    @Override
    public JobOffer createJob(Caller caller, JobOfferDetails details) {
        caller.requireAdmin();
        return offers.save(JobOffer.publish(details, clock.millis()));
    }

    @Override
    public JobOffer updateJob(Caller caller, long id, JobOfferDetails details) {
        caller.requireAdmin();
        JobOffer current = offers.findById(id).orElseThrow(() -> NotFoundException.jobOffer(id));
        return offers.save(current.withDetails(details));
    }

    @Override
    public void deleteJob(Caller caller, long id) {
        caller.requireAdmin();
        if (!offers.markDeleted(id, clock.millis())) {
            throw NotFoundException.jobOffer(id);
        }
    }
}
