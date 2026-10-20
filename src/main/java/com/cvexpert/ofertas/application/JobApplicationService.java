package com.cvexpert.ofertas.application;

import com.cvexpert.ofertas.domain.exception.AlreadyAppliedException;
import com.cvexpert.ofertas.domain.exception.NotFoundException;
import com.cvexpert.ofertas.domain.model.ApplicationStatus;
import com.cvexpert.ofertas.domain.model.ApplicationStatusChanged;
import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.JobApplication;
import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.PageResult;
import com.cvexpert.ofertas.domain.port.in.AdminChangeStatusUseCase;
import com.cvexpert.ofertas.domain.port.in.ApplyToJobUseCase;
import com.cvexpert.ofertas.domain.port.in.GetApplicationUseCase;
import com.cvexpert.ofertas.domain.port.in.ListApplicationsUseCase;
import com.cvexpert.ofertas.domain.port.in.MoveApplicationUseCase;
import com.cvexpert.ofertas.domain.port.in.WithdrawApplicationUseCase;
import com.cvexpert.ofertas.domain.port.out.EventPublisher;
import com.cvexpert.ofertas.domain.port.out.JobApplicationRepository;
import com.cvexpert.ofertas.domain.port.out.JobOfferRepository;
import java.time.Clock;

/**
 * Casos de uso de las postulaciones.
 *
 * <p>Regla de los eventos: solo se publica cuando un administrador cambia el estado de la
 * postulación de otra persona. Si la persona se postula o mueve su propia tarjeta, no hay nada que
 * avisarle.
 */
public class JobApplicationService implements ListApplicationsUseCase, GetApplicationUseCase,
        ApplyToJobUseCase, MoveApplicationUseCase, WithdrawApplicationUseCase, AdminChangeStatusUseCase {

    private final JobApplicationRepository applications;
    private final JobOfferRepository offers;
    private final EventPublisher events;
    private final Clock clock;

    public JobApplicationService(JobApplicationRepository applications, JobOfferRepository offers,
            EventPublisher events, Clock clock) {
        this.applications = applications;
        this.offers = offers;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public PageResult<JobApplication> listApplications(Caller caller, PageQuery query) {
        return applications.searchByUser(caller.userId(), query);
    }

    @Override
    public JobApplication getApplication(Caller caller, long id) {
        return findOwn(caller, id);
    }

    @Override
    public JobApplication apply(Caller caller, long jobOfferId, String notes) {
        JobOffer offer = offers.findById(jobOfferId).orElseThrow(() -> NotFoundException.jobOffer(jobOfferId));
        if (applications.existsByUserAndJobOffer(caller.userId(), jobOfferId)) {
            throw new AlreadyAppliedException();
        }
        return applications.save(JobApplication.apply(caller.userId(), offer, notes, clock.millis()));
    }

    @Override
    public JobApplication moveApplication(Caller caller, long id, ApplicationStatus status) {
        JobApplication current = findOwn(caller, id);
        JobApplication moved = current.moveTo(status, clock.millis());
        return moved == current ? current : applications.save(moved);
    }

    @Override
    public void withdraw(Caller caller, long id) {
        findOwn(caller, id);
        applications.delete(id);
    }

    @Override
    public JobApplication changeStatus(Caller caller, long id, ApplicationStatus status) {
        caller.requireAdmin();
        JobApplication current = applications.findById(id).orElseThrow(() -> NotFoundException.application(id));
        JobApplication moved = current.moveTo(status, clock.millis());
        if (moved == current) {
            return current;
        }
        JobApplication saved = applications.save(moved);
        events.publish(ApplicationStatusChanged.between(current, saved));
        return saved;
    }

    /** La postulación {@code id} si es de quien llama; si es de otro, responde como si no existiera. */
    private JobApplication findOwn(Caller caller, long id) {
        return applications.findById(id)
                .filter(a -> a.belongsTo(caller.userId()))
                .orElseThrow(() -> NotFoundException.application(id));
    }
}
