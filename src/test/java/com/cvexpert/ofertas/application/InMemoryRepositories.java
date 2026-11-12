package com.cvexpert.ofertas.application;

import com.cvexpert.ofertas.domain.exception.AlreadyAppliedException;
import com.cvexpert.ofertas.domain.model.AlertSettings;
import com.cvexpert.ofertas.domain.model.ApplicationStatusChanged;
import com.cvexpert.ofertas.domain.model.JobApplication;
import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.NewJobMatched;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.PageResult;
import com.cvexpert.ofertas.domain.port.out.AlertSettingsRepository;
import com.cvexpert.ofertas.domain.port.out.EventPublisher;
import com.cvexpert.ofertas.domain.port.out.JobApplicationRepository;
import com.cvexpert.ofertas.domain.port.out.JobOfferRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Implementaciones en memoria de los puertos de salida. Con ellas los servicios se prueban sin
 * Spring ni base de datos: es la ventaja de que el dominio dependa de interfaces.
 */
public final class InMemoryRepositories {

    private InMemoryRepositories() {
    }

    public static final class Offers implements JobOfferRepository {
        private final Map<Long, JobOffer> rows = new LinkedHashMap<>();
        private final Set<Long> deleted = new HashSet<>();
        private long nextId = 1;

        @Override
        public JobOffer save(JobOffer offer) {
            JobOffer saved = offer.id() == null ? new JobOffer(nextId++, offer.details(), offer.postedAt()) : offer;
            rows.put(saved.id(), saved);
            return saved;
        }

        @Override
        public Optional<JobOffer> findById(long id) {
            return deleted.contains(id) ? Optional.empty() : Optional.ofNullable(rows.get(id));
        }

        @Override
        public PageResult<JobOffer> search(PageQuery query, java.util.Set<com.cvexpert.ofertas.domain.model.Modality> modalities,
                long postedAfter) {
            List<JobOffer> active = findAll(query.search(), modalities, postedAfter);
            int from = Math.min((query.page() - 1) * query.pageSize(), active.size());
            int to = Math.min(from + query.pageSize(), active.size());
            return new PageResult<>(active.subList(from, to), query.page(), query.pageSize(), active.size());
        }

        @Override
        public List<JobOffer> findAll(String search, java.util.Set<com.cvexpert.ofertas.domain.model.Modality> modalities,
                long postedAfter) {
            return rows.values().stream()
                    .filter(o -> !deleted.contains(o.id()))
                    .filter(o -> modalities.isEmpty() || modalities.contains(o.details().modality()))
                    .filter(o -> o.postedAt() >= postedAfter)
                    .filter(o -> search == null || o.details().title().toLowerCase().contains(search.toLowerCase()))
                    .toList();
        }

        @Override
        public boolean markDeleted(long id, long deletedAt) {
            return rows.containsKey(id) && deleted.add(id);
        }
    }

    public static final class Applications implements JobApplicationRepository {
        private final Map<Long, JobApplication> rows = new LinkedHashMap<>();
        private long nextId = 1;
        int saves;

        @Override
        public JobApplication save(JobApplication a) {
            saves++;
            if (a.id() == null) {
                if (existsByUserAndJobOffer(a.userId(), a.jobOfferId())) {
                    throw new AlreadyAppliedException();
                }
                a = new JobApplication(nextId++, a.userId(), a.jobOfferId(), a.jobTitle(), a.company(),
                        a.companyLogoUrl(), a.location(), a.modality(), a.status(), a.appliedAt(), a.updatedAt(),
                        a.notes());
            }
            rows.put(a.id(), a);
            return a;
        }

        @Override
        public Optional<JobApplication> findById(long id) {
            return Optional.ofNullable(rows.get(id));
        }

        @Override
        public boolean existsByUserAndJobOffer(String userId, long jobOfferId) {
            return rows.values().stream().anyMatch(a -> a.belongsTo(userId) && a.jobOfferId() == jobOfferId);
        }

        @Override
        public PageResult<JobApplication> searchByUser(String userId, PageQuery query) {
            List<JobApplication> own = rows.values().stream().filter(a -> a.belongsTo(userId)).toList();
            return new PageResult<>(own, query.page(), query.pageSize(), own.size());
        }

        @Override
        public void delete(long id) {
            rows.remove(id);
        }
    }

    /** Guarda los eventos en una lista para revisarlos en la prueba. */
    public static final class RecordingPublisher implements EventPublisher {
        public final List<ApplicationStatusChanged> published = new ArrayList<>();
        public final List<NewJobMatched> newJobs = new ArrayList<>();

        @Override
        public void publish(ApplicationStatusChanged event) {
            published.add(event);
        }

        @Override
        public void publish(NewJobMatched event) {
            newJobs.add(event);
        }
    }

    public static final class Alerts implements AlertSettingsRepository {
        private final Map<String, AlertSettings> rows = new LinkedHashMap<>();

        @Override
        public Optional<AlertSettings> findByUser(String userId) {
            return Optional.ofNullable(rows.get(userId));
        }

        @Override
        public AlertSettings save(AlertSettings settings) {
            rows.put(settings.userId(), settings);
            return settings;
        }

        @Override
        public List<AlertSettings> findEnabled() {
            return rows.values().stream().filter(AlertSettings::enabled).toList();
        }
    }
}
