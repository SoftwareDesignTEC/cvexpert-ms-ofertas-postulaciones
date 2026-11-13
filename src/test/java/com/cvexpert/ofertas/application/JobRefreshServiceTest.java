package com.cvexpert.ofertas.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cvexpert.ofertas.domain.exception.RefreshNotAvailableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** La espera entre corridas y el importador apagado. */
class JobRefreshServiceTest {

    @Test
    void withoutImporterItIsUnavailable() {
        var service = new JobRefreshService(null, Clock.systemUTC(), Duration.ofMinutes(10));
        var e = assertThrows(RefreshNotAvailableException.class, service::refreshJobs);
        assertFalse(e.tooSoon());
    }

    @Test
    void secondRunWaitsForTheCooldown() {
        var importer = new JobImportServiceTest().importerReturning(2);
        var clock = new MutableClock();
        var service = new JobRefreshService(importer, clock, Duration.ofMinutes(10));

        assertEquals(2, service.refreshJobs().created());
        clock.advance(Duration.ofMinutes(5));
        var e = assertThrows(RefreshNotAvailableException.class, service::refreshJobs);
        assertTrue(e.tooSoon());
        clock.advance(Duration.ofMinutes(6));
        service.refreshJobs();
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-13T12:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
