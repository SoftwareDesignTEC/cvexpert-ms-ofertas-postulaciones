package com.cvexpert.ofertas.adapter.in.web;

import static com.cvexpert.ofertas.Fixtures.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cvexpert.ofertas.Fixtures;
import com.cvexpert.ofertas.application.AlertService;
import com.cvexpert.ofertas.application.InMemoryRepositories;
import com.cvexpert.ofertas.application.JobOfferService;
import com.cvexpert.ofertas.domain.model.CompatibilityCalculator;
import com.cvexpert.ofertas.domain.model.MatchWeights;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Rutas de ofertas y alertas con los casos de uso reales y repositorios en memoria. */
@WebMvcTest({JobController.class, AlertController.class})
class JobAndAlertControllerTest {

    @TestConfiguration
    static class Beans {
        @Bean
        InMemoryRepositories.Offers offers() {
            return new InMemoryRepositories.Offers();
        }

        @Bean
        InMemoryRepositories.Alerts alerts() {
            return new InMemoryRepositories.Alerts();
        }

        @Bean
        InMemoryRepositories.RecordingPublisher events() {
            return new InMemoryRepositories.RecordingPublisher();
        }

        @Bean
        JobOfferService jobOfferService(InMemoryRepositories.Offers offers, InMemoryRepositories.Alerts alerts,
                InMemoryRepositories.RecordingPublisher events) {
            return new JobOfferService(offers, alerts, new CompatibilityCalculator(MatchWeights.DEFAULT), events,
                    Fixtures.clockAt(NOW));
        }

        @Bean
        AlertService alertService(InMemoryRepositories.Alerts alerts) {
            return new AlertService(alerts, Fixtures.clockAt(NOW));
        }
    }

    static final String OFFER = """
            {"title":"Desarrollador Android","company":"Acme CR","location":"San José","modality":"hybrid",
             "type":"full-time","sector":"Tecnología","description":"Apps móviles","skills":["Kotlin","Git"],
             "minExperienceYears":1,"salary":{"min":600000,"max":900000,"currency":"CRC"}}
            """;

    static final String ALERTS = """
            {"enabled":true,"minMatch":70,"profile":{"skills":["kotlin","git"],"location":"San José",
             "modalities":["hybrid","remote"],"jobTypes":["full-time"],"sectors":["Tecnología"],"experienceYears":2}}
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    InMemoryRepositories.RecordingPublisher events;

    private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, String userId) {
        return request.header("X-User-Id", userId).header("X-User-Email", userId + "@example.com");
    }

    private static MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return as(request, "admin").header("X-User-Role", "admin");
    }

    private String createOffer() throws Exception {
        String body = mvc.perform(asAdmin(post("/jobs")).contentType(MediaType.APPLICATION_JSON).content(OFFER))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*?\"id\":\"(\\d+)\".*", "$1");
    }

    @Test
    void adminCreatesAnOfferWithTheContractShape() throws Exception {
        mvc.perform(asAdmin(post("/jobs")).contentType(MediaType.APPLICATION_JSON).content(OFFER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.modality").value("hybrid"))
                .andExpect(jsonPath("$.type").value("full-time"))
                .andExpect(jsonPath("$.skills[0]").value("Kotlin"))
                .andExpect(jsonPath("$.salary.currency").value("CRC"))
                .andExpect(jsonPath("$.postedAt").value(NOW))
                .andExpect(jsonPath("$.matchScore").doesNotExist());
    }

    @Test
    void onlyAdminsWriteOffers() throws Exception {
        String id = createOffer();

        mvc.perform(as(post("/jobs"), "ana-w").contentType(MediaType.APPLICATION_JSON).content(OFFER))
                .andExpect(status().isForbidden());
        mvc.perform(as(put("/jobs/" + id), "ana-w").contentType(MediaType.APPLICATION_JSON).content(OFFER))
                .andExpect(status().isForbidden());
        mvc.perform(as(delete("/jobs/" + id), "ana-w")).andExpect(status().isForbidden());
    }

    @Test
    void invalidOffersAreBadRequests() throws Exception {
        mvc.perform(asAdmin(post("/jobs")).contentType(MediaType.APPLICATION_JSON)
                        .content(OFFER.replace("\"hybrid\"", "\"presencial\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(asAdmin(post("/jobs")).contentType(MediaType.APPLICATION_JSON)
                        .content(OFFER.replace("\"title\":\"Desarrollador Android\",", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletedOffersAreGone() throws Exception {
        String id = createOffer();

        mvc.perform(asAdmin(delete("/jobs/" + id))).andExpect(status().isNoContent());
        mvc.perform(as(get("/jobs/" + id), "ana-d")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"));
        mvc.perform(as(get("/jobs/abc"), "ana-d")).andExpect(status().isNotFound());
    }

    @Test
    void alertsDoNotExistUntilSaved() throws Exception {
        mvc.perform(as(get("/alerts"), "ana-new"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ALERTS_NOT_FOUND"));
    }

    @Test
    void savedAlertsComeBackAndScoreTheOffers() throws Exception {
        String id = createOffer();

        mvc.perform(as(put("/alerts"), "ana-a").contentType(MediaType.APPLICATION_JSON).content(ALERTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.minMatch").value(70))
                .andExpect(jsonPath("$.profile.jobTypes[0]").value("full-time"))
                .andExpect(jsonPath("$.updatedAt").value(NOW));
        mvc.perform(as(get("/alerts"), "ana-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.modalities[1]").value("remote"));

        mvc.perform(as(get("/jobs/" + id), "ana-a")).andExpect(jsonPath("$.matchScore").value(100));
        mvc.perform(as(get("/jobs"), "ana-a")).andExpect(jsonPath("$.items[0].matchScore").isNumber());
    }

    @Test
    void invalidAlertsAreBadRequests() throws Exception {
        mvc.perform(as(put("/alerts"), "ana-b").contentType(MediaType.APPLICATION_JSON)
                        .content(ALERTS.replace("\"minMatch\":70", "\"minMatch\":101")))
                .andExpect(status().isBadRequest());
        mvc.perform(as(put("/alerts"), "ana-b").contentType(MediaType.APPLICATION_JSON)
                        .content(ALERTS.replace("\"full-time\"", "\"full_time\"")))
                .andExpect(status().isBadRequest());
        mvc.perform(as(put("/alerts"), "ana-b").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"minMatch\":50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el campo profile"));
    }

    @Test
    void creatingAnOfferPublishesJobNewForMatchingUsers() throws Exception {
        mvc.perform(as(put("/alerts"), "ana-n").contentType(MediaType.APPLICATION_JSON).content(ALERTS))
                .andExpect(status().isOk());
        int before = (int) events.newJobs.stream().filter(e -> e.userId().equals("ana-n")).count();

        createOffer();

        long after = events.newJobs.stream().filter(e -> e.userId().equals("ana-n")).count();
        assertEquals(before + 1, after);
    }
}
