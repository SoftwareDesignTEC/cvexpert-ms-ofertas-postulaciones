package com.cvexpert.ofertas.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Deja pasar solo las llamadas que traen el certificado de cliente de APIM.
 *
 * <p>App Service está en modo "certificado obligatorio": pide un certificado en el TLS y se lo pasa
 * a la app en el encabezado {@code X-ARR-ClientCert} (el certificado en base64). App Service solo
 * revisa que haya uno, no cuál es; este filtro compara su huella SHA-1 con
 * {@code APIM_CERT_THUMBPRINT} y además revisa que no esté vencido. Si no calza, responde 403.
 *
 * <p>{@code APIM_CERT_THUMBPRINT} acepta varias huellas separadas por coma, para poder cambiar el
 * certificado de APIM sin cortar el servicio. Si la variable está vacía (en local y en las
 * pruebas), el filtro no revisa nada.
 *
 * <p>{@code /actuator/health} queda fuera porque App Service no pide certificado en esa ruta: la usa
 * la prueba de humo del despliegue.
 */
@Component
public class ApimCertificateFilter extends OncePerRequestFilter {

    static final String CLIENT_CERT_HEADER = "X-ARR-ClientCert";
    private static final String HEALTH_PATH = "/actuator/health";
    private static final Logger log = LoggerFactory.getLogger(ApimCertificateFilter.class);

    private final Set<String> allowedThumbprints;
    private final Clock clock;

    @Autowired
    public ApimCertificateFilter(@Value("${apim.cert-thumbprint:}") String thumbprints) {
        this(thumbprints, Clock.systemUTC());
    }

    ApimCertificateFilter(String thumbprints, Clock clock) {
        this.allowedThumbprints = Arrays.stream(thumbprints.split(","))
                .map(ApimCertificateFilter::normalize)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.clock = clock;
        if (allowedThumbprints.isEmpty()) {
            log.warn("APIM_CERT_THUMBPRINT no está definida: no se valida el certificado de APIM");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return allowedThumbprints.isEmpty() || path.equals(HEALTH_PATH) || path.startsWith(HEALTH_PATH + "/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String reason = rejectionReason(request.getHeader(CLIENT_CERT_HEADER));
        if (reason != null) {
            log.warn("Llamada rechazada a {}: {}", request.getRequestURI(), reason);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"code\":\"INVALID_CLIENT_CERTIFICATE\",\"message\":\"Solo APIM puede llamar a este servicio\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    /** Por qué se rechaza el certificado, o {@code null} si es el de APIM y está vigente. */
    String rejectionReason(String header) {
        if (header == null || header.isBlank()) {
            return "no trae certificado";
        }
        X509Certificate cert;
        try {
            byte[] der = Base64.getMimeDecoder().decode(header);
            cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der));
        } catch (IllegalArgumentException | CertificateException e) {
            return "el certificado no se puede leer";
        }
        String thumbprint = thumbprint(cert);
        if (!allowedThumbprints.contains(thumbprint)) {
            return "certificado desconocido (" + thumbprint + ")";
        }
        var now = clock.instant();
        if (now.isBefore(cert.getNotBefore().toInstant()) || now.isAfter(cert.getNotAfter().toInstant())) {
            return "el certificado de APIM está vencido o todavía no es válido";
        }
        return null;
    }

    /** Huella SHA-1 en hexadecimal mayúscula, el mismo formato que muestran Azure y PowerShell. */
    static String thumbprint(X509Certificate cert) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(cert.getEncoded());
            return HexFormat.of().withUpperCase().formatHex(digest);
        } catch (NoSuchAlgorithmException | CertificateException e) {
            throw new IllegalStateException("No se pudo calcular la huella del certificado", e);
        }
    }

    /** Acepta la huella con espacios, dos puntos o en minúscula, como la copie quien la configure. */
    private static String normalize(String thumbprint) {
        return thumbprint.replaceAll("[\\s:]", "").toUpperCase(Locale.ROOT);
    }
}
