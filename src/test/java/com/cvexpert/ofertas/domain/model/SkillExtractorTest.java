package com.cvexpert.ofertas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SkillExtractorTest {

    @Test
    void reconoceHabilidadesSinImportarTildesNiMayusculas() {
        assertThat(SkillExtractor.extract("Experiencia en ATENCIÓN AL CLIENTE y manejo de Excel. Inglés B2."))
                .containsExactly("Excel", "Inglés", "Servicio al cliente");
    }

    @Test
    void pidePalabraCompleta() {
        assertThat(SkillExtractor.extract("Frontend con JavaScript y React"))
                .contains("JavaScript", "React")
                .doesNotContain("Java");
    }

    @Test
    void redesSocialesNoEsRedesInformaticas() {
        assertThat(SkillExtractor.extract("Manejo de redes sociales"))
                .containsExactly("Redes sociales");
    }

    @Test
    void textoVacioNoTieneHabilidades() {
        assertThat(SkillExtractor.extract(" ")).isEmpty();
        assertThat(SkillExtractor.extract(null)).isEmpty();
    }
}
