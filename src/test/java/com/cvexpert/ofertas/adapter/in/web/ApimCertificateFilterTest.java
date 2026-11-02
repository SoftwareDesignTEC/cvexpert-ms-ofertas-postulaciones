package com.cvexpert.ofertas.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Prueba el filtro con dos certificados de prueba (solo la parte pública, en
 * {@code src/test/resources/certs}). Las huellas esperadas las calculó OpenSSL por fuera
 * ({@code openssl x509 -fingerprint -sha1}), así la prueba no confía en el mismo código que prueba.
 */
class ApimCertificateFilterTest {

    /** Huella de {@code certs/apim.pem}, tal como la imprime OpenSSL. */
    static final String APIM = "26:01:8E:D0:60:B0:C9:D7:2F:2A:E3:29:3B:83:81:10:8B:A7:D8:37";

    /** Dentro de la vigencia de los certificados de prueba (2 nov 2026 a 30 oct 2036). */
    private final Clock now = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);

    private static String pemAsHeader(String name) throws IOException {
        try (InputStream in = ApimCertificateFilterTest.class.getResourceAsStream("/certs/" + name)) {
            // App Service manda el certificado en base64 sin las líneas BEGIN/END.
            return new String(in.readAllBytes(), StandardCharsets.US_ASCII)
                    .replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        }
    }

    private MockHttpServletResponse call(ApimCertificateFilter filter, String path, String header) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (header != null) {
            request.addHeader(ApimCertificateFilter.CLIENT_CERT_HEADER, header);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void theApimCertificatePasses() throws Exception {
        ApimCertificateFilter filter = new ApimCertificateFilter(APIM, now);

        assertEquals(200, call(filter, "/jobs", pemAsHeader("apim.pem")).getStatus());
    }

    @Test
    void anyOtherCertificateOrNoneIsForbidden() throws Exception {
        ApimCertificateFilter filter = new ApimCertificateFilter(APIM, now);

        MockHttpServletResponse other = call(filter, "/jobs", pemAsHeader("otro.pem"));
        assertEquals(403, other.getStatus());
        assertTrue(other.getContentAsString().contains("INVALID_CLIENT_CERTIFICATE"));
        assertEquals(403, call(filter, "/applications", null).getStatus());
        assertEquals(403, call(filter, "/applications", "esto-no-es-un-certificado").getStatus());
    }

    @Test
    void healthIsNotChecked() throws Exception {
        ApimCertificateFilter filter = new ApimCertificateFilter(APIM, now);

        assertEquals(200, call(filter, "/actuator/health", null).getStatus());
        assertEquals(403, call(filter, "/actuator/healthy", null).getStatus(), "solo la ruta exacta y sus hijas");
    }

    @Test
    void withoutAThumbprintNothingIsChecked() throws Exception {
        ApimCertificateFilter filter = new ApimCertificateFilter("", now);

        assertEquals(200, call(filter, "/jobs", null).getStatus());
    }

    @Test
    void thumbprintsAreReadWithAnyFormatAndSeveralAreAllowed() throws Exception {
        String lowerNoColons = APIM.replace(":", "").toLowerCase();
        ApimCertificateFilter filter = new ApimCertificateFilter("AAAA, " + lowerNoColons, now);

        assertEquals(200, call(filter, "/jobs", pemAsHeader("apim.pem")).getStatus());
    }

    @Test
    void anExpiredCertificateIsForbiddenEvenIfItIsTheRightOne() throws Exception {
        Clock in2040 = Clock.fixed(Instant.parse("2040-01-01T00:00:00Z"), ZoneOffset.UTC);
        ApimCertificateFilter filter = new ApimCertificateFilter(APIM, in2040);

        String reason = filter.rejectionReason(pemAsHeader("apim.pem"));
        assertNotNull(reason);
        assertTrue(reason.contains("vencido"));
    }

    @Test
    void aCertificateThatIsNotValidYetIsForbidden() throws Exception {
        Clock in2020 = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);

        assertNotNull(new ApimCertificateFilter(APIM, in2020).rejectionReason(pemAsHeader("apim.pem")));
    }

    @Test
    void theRightCertificateHasNoRejectionReason() throws Exception {
        assertNull(new ApimCertificateFilter(APIM, now).rejectionReason(pemAsHeader("apim.pem")));
    }
}
