package com.cvexpert.ofertas.adapter.in.web;

import static com.cvexpert.ofertas.Fixtures.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cvexpert.ofertas.Fixtures;
import com.cvexpert.ofertas.application.InMemoryRepositories;
import com.cvexpert.ofertas.application.JobApplicationService;
import com.cvexpert.ofertas.domain.model.ApplicationStatus;
import com.cvexpert.ofertas.domain.model.JobOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Prueba las rutas de postulaciones de punta a punta dentro del servicio: HTTP, controladores,
 * casos de uso reales y repositorios en memoria. Así se comprueba el contrato (códigos, ids como
 * texto, fechas en epoch) sin base de datos.
 */
@WebMvcTest({ApplicationController.class, AdminApplicationController.class})
class ApplicationControllerTest {

    @TestConfiguration
    static class Beans {
        @Bean
        InMemoryRepositories.Offers offers() {
            return new InMemoryRepositories.Offers();
        }

        @Bean
        InMemoryRepositories.Applications applications() {
            return new InMemoryRepositories.Applications();
        }

        @Bean
        InMemoryRepositories.RecordingPublisher events() {
            return new InMemoryRepositories.RecordingPublisher();
        }

        @Bean
        JobApplicationService jobApplicationService(InMemoryRepositories.Applications applications,
                InMemoryRepositories.Offers offers, InMemoryRepositories.RecordingPublisher events) {
            return new JobApplicationService(applications, offers, events, Fixtures.clockAt(NOW));
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    InMemoryRepositories.Offers offers;

    @Autowired
    InMemoryRepositories.RecordingPublisher events;

    private String offerId;

    @BeforeEach
    void publishAnOffer() {
        JobOffer offer = offers.save(JobOffer.publish(Fixtures.details("Desarrollador Android"), NOW - 1000));
        offerId = String.valueOf(offer.id());
        events.published.clear();
    }

    private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, String userId) {
        return request.header("X-User-Id", userId).header("X-User-Email", userId + "@example.com");
    }

    private String apply(String userId) throws Exception {
        String body = mvc.perform(as(post("/applications"), userId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobOfferId\":\"" + offerId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"id\":\"(\\d+)\".*", "$1");
    }

    @Test
    void applyingReturnsTheCardWithTheContractShape() throws Exception {
        mvc.perform(as(post("/applications"), "ana-apply").contentType(MediaType.APPLICATION_JSON)
                        // Campos que mandan los clientes de la Fase I: se aceptan y se ignoran.
                        .content("{\"jobOfferId\":\"" + offerId + "\",\"notes\":\"Me interesa\","
                                + "\"userId\":\"otro\",\"jobTitle\":\"Ignorado\",\"status\":\"rejected\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.jobOfferId").value(offerId))
                .andExpect(jsonPath("$.userId").value("ana-apply"))
                .andExpect(jsonPath("$.jobTitle").value("Desarrollador Android"))
                .andExpect(jsonPath("$.status").value("applied"))
                .andExpect(jsonPath("$.modality").value("hybrid"))
                .andExpect(jsonPath("$.appliedAt").value(NOW))
                .andExpect(jsonPath("$.notes").value("Me interesa"));
    }

    @Test
    void applyingTwiceIsAConflict() throws Exception {
        apply("ana-twice");
        mvc.perform(as(post("/applications"), "ana-twice").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobOfferId\":\"" + offerId + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPLICATION_ALREADY_EXISTS"));
    }

    @Test
    void badBodiesAreBadRequests() throws Exception {
        mvc.perform(as(post("/applications"), "ana-bad").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(as(post("/applications"), "ana-bad").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void applyingToAMissingOfferIsNotFound() throws Exception {
        mvc.perform(as(post("/applications"), "ana-missing").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobOfferId\":\"999999\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"));
    }

    @Test
    void withoutIdentityIsABadRequest() throws Exception {
        mvc.perform(get("/applications")).andExpect(status().isBadRequest());
    }

    @Test
    void listIsPaginatedAndOnlyShowsYourCards() throws Exception {
        apply("ana-list");
        apply("luis-list");

        mvc.perform(as(get("/applications"), "ana-list").param("page", "1").param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].userId").value("ana-list"));
        mvc.perform(as(get("/applications"), "ana-list").param("pageSize", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(as(get("/applications"), "ana-list").param("page", "uno"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void someoneElsesCardOrABadIdIsNotFound() throws Exception {
        String id = apply("ana-own");

        mvc.perform(as(get("/applications/" + id), "luis-own"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
        mvc.perform(as(get("/applications/abc"), "ana-own")).andExpect(status().isNotFound());
        mvc.perform(as(get("/applications/" + id), "ana-own")).andExpect(status().isOk());
    }

    @Test
    void movingYourCardDoesNotPublish() throws Exception {
        String id = apply("ana-move");

        mvc.perform(as(put("/applications/" + id), "ana-move").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"in-progress\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("in-progress"));
        // El valor viejo de los mocks de la Fase I ya no se acepta.
        mvc.perform(as(put("/applications/" + id), "ana-move").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"in_progress\"}"))
                .andExpect(status().isBadRequest());

        assertTrue(events.published.isEmpty());
    }

    @Test
    void withdrawingReturnsNoContent() throws Exception {
        String id = apply("ana-withdraw");

        mvc.perform(as(delete("/applications/" + id), "ana-withdraw")).andExpect(status().isNoContent());
        mvc.perform(as(get("/applications/" + id), "ana-withdraw")).andExpect(status().isNotFound());
    }

    @Test
    void adminRouteNeedsTheRoleAndPublishes() throws Exception {
        String id = apply("ana-admin");

        mvc.perform(as(put("/admin/applications/" + id + "/status"), "intruso")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"interview\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertTrue(events.published.isEmpty());

        mvc.perform(as(put("/admin/applications/" + id + "/status"), "admin").header("X-User-Role", "admin")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"interview\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("ana-admin"))
                .andExpect(jsonPath("$.status").value("interview"));

        assertEquals(1, events.published.size());
        assertEquals("ana-admin", events.published.get(0).userId());
        assertEquals(ApplicationStatus.INTERVIEW, events.published.get(0).newStatus());
    }
}
