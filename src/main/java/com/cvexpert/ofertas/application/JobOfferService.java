package com.cvexpert.ofertas.application;

import com.cvexpert.ofertas.domain.exception.NotFoundException;
import com.cvexpert.ofertas.domain.model.AlertSettings;
import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.CompatibilityCalculator;
import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.JobOfferDetails;
import com.cvexpert.ofertas.domain.model.MatchingProfile;
import com.cvexpert.ofertas.domain.model.NewJobMatched;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.PageResult;
import com.cvexpert.ofertas.domain.model.ScoredJobOffer;
import com.cvexpert.ofertas.domain.port.in.CreateJobUseCase;
import com.cvexpert.ofertas.domain.port.in.DeleteJobUseCase;
import com.cvexpert.ofertas.domain.port.in.GetJobUseCase;
import com.cvexpert.ofertas.domain.port.in.ListJobsUseCase;
import com.cvexpert.ofertas.domain.port.in.UpdateJobUseCase;
import com.cvexpert.ofertas.domain.port.out.AlertSettingsRepository;
import com.cvexpert.ofertas.domain.port.out.EventPublisher;
import com.cvexpert.ofertas.domain.port.out.JobOfferRepository;
import java.time.Clock;
import java.util.Optional;

/**
 * Casos de uso de las ofertas. Al listar o ver una oferta se le suma la compatibilidad con el
 * perfil de quien consulta; al crearla se avisa a cada usuario cuya compatibilidad alcanza el
 * mínimo que fijó ({@code job.new}).
 */
public class JobOfferService
        implements ListJobsUseCase, GetJobUseCase, CreateJobUseCase, UpdateJobUseCase, DeleteJobUseCase {

    private final JobOfferRepository offers;
    private final AlertSettingsRepository alerts;
    private final CompatibilityCalculator compatibility;
    private final EventPublisher events;
    private final Clock clock;

    public JobOfferService(JobOfferRepository offers, AlertSettingsRepository alerts,
            CompatibilityCalculator compatibility, EventPublisher events, Clock clock) {
        this.offers = offers;
        this.alerts = alerts;
        this.compatibility = compatibility;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public PageResult<ScoredJobOffer> listJobs(Caller caller, PageQuery query) {
        Optional<MatchingProfile> profile = profileOf(caller);
        return offers.search(query).map(offer -> scored(offer, profile));
    }

    @Override
    public ScoredJobOffer getJob(Caller caller, long id) {
        JobOffer offer = offers.findById(id).orElseThrow(() -> NotFoundException.jobOffer(id));
        return scored(offer, profileOf(caller));
    }

    @Override
    public JobOffer createJob(Caller caller, JobOfferDetails details) {
        caller.requireAdmin();
        JobOffer saved = offers.save(JobOffer.publish(details, clock.millis()));
        notifyMatchingUsers(saved);
        return saved;
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

    /** Un {@code job.new} por cada usuario con alertas activas que alcanza su mínimo. */
    private void notifyMatchingUsers(JobOffer offer) {
        for (AlertSettings settings : alerts.findEnabled()) {
            int score = compatibility.score(offer, settings.profile());
            if (settings.wantsAlertFor(score)) {
                events.publish(NewJobMatched.of(settings.userId(), offer, score));
            }
        }
    }

    private Optional<MatchingProfile> profileOf(Caller caller) {
        return alerts.findByUser(caller.userId()).map(AlertSettings::profile);
    }

    private ScoredJobOffer scored(JobOffer offer, Optional<MatchingProfile> profile) {
        return new ScoredJobOffer(offer, profile.map(p -> compatibility.score(offer, p)).orElse(null));
    }
}
