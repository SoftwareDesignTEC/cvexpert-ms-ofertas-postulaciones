package com.cvexpert.ofertas;

import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.JobOfferDetails;
import com.cvexpert.ofertas.domain.model.JobType;
import com.cvexpert.ofertas.domain.model.Modality;
import com.cvexpert.ofertas.domain.model.Salary;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/** Datos de prueba compartidos. */
public final class Fixtures {

    /** 16 oct 2026, 00:00 UTC. */
    public static final long NOW = 1_792_108_800_000L;

    public static final Caller ANA = new Caller("google-sub-ana", "ana@example.com", null);
    public static final Caller LUIS = new Caller("google-sub-luis", "luis@example.com", null);
    public static final Caller ADMIN = new Caller("google-sub-admin", "admin@example.com", Caller.ADMIN_ROLE);

    private Fixtures() {
    }

    public static Clock clockAt(long epochMillis) {
        return Clock.fixed(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }

    public static JobOfferDetails details(String title) {
        return new JobOfferDetails(title, "Acme CR", "https://example.com/logo.png", "San José",
                Modality.HYBRID, JobType.FULL_TIME, "Tecnología", "Desarrollo de apps móviles",
                List.of("Bachillerato"), List.of("Kotlin", "Git"), 1,
                new Salary(new BigDecimal("600000.00"), new BigDecimal("900000.00"), "CRC"));
    }
}
