package com.cvexpert.ofertas.application;

import com.cvexpert.ofertas.domain.exception.RefreshNotAvailableException;
import com.cvexpert.ofertas.domain.port.in.RefreshJobsUseCase;
import java.time.Clock;
import java.time.Duration;

/**
 * Corre el importador a pedido ({@code POST /jobs/refresh}), con una espera mínima entre
 * corridas: cada una hace todas las búsquedas configuradas y el plan gratis de JSearch da 200
 * consultas al mes.
 *
 * <p>{@code synchronized}: si dos personas tocan el botón a la vez, la segunda espera y recibe
 * el aviso de que fue hace poco, en vez de gastar consultas dos veces.
 */
public class JobRefreshService implements RefreshJobsUseCase {

    private final JobImportService importer;
    private final Clock clock;
    private final Duration cooldown;
    private long lastRun = Long.MIN_VALUE;

    /** @param importer {@code null} si el importador está apagado (sin clave de la API). */
    public JobRefreshService(JobImportService importer, Clock clock, Duration cooldown) {
        this.importer = importer;
        this.clock = clock;
        this.cooldown = cooldown;
    }

    @Override
    public synchronized Result refreshJobs() {
        if (importer == null) {
            throw RefreshNotAvailableException.disabled();
        }
        long now = clock.millis();
        long elapsed = now - lastRun;
        if (lastRun != Long.MIN_VALUE && elapsed < cooldown.toMillis()) {
            throw RefreshNotAvailableException.tooSoon((cooldown.toMillis() - elapsed + 999) / 1000);
        }
        lastRun = now;
        JobImportService.Result result = importer.importJobs();
        return new Result(result.created(), result.updated(), result.failedQueries());
    }
}
