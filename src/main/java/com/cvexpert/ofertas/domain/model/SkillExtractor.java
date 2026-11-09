package com.cvexpert.ofertas.domain.model;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Saca las habilidades de una oferta real a partir de su texto.
 *
 * <p>Las ofertas de la API externa no traen una lista de habilidades, solo título y descripción.
 * Pero la compatibilidad y el análisis de brecha necesitan esa lista, así que se buscan en el
 * texto las habilidades de un catálogo. Cada habilidad tiene un nombre (el que se guarda) y las
 * palabras con que suele aparecer, en español y en inglés. La búsqueda ignora mayúsculas y tildes,
 * y pide palabra completa: "Java" no aparece dentro de "JavaScript".
 */
public final class SkillExtractor {

    /** Nombre que se guarda → formas en que aparece en el texto. */
    private static final Map<String, List<String>> CATALOG = Map.ofEntries(
            Map.entry("Inglés", List.of("ingles", "english", "bilingue", "bilingual")),
            Map.entry("Excel", List.of("excel")),
            Map.entry("SQL", List.of("sql")),
            Map.entry("Java", List.of("java")),
            Map.entry("JavaScript", List.of("javascript", "js")),
            Map.entry("TypeScript", List.of("typescript")),
            Map.entry("Python", List.of("python")),
            Map.entry("Kotlin", List.of("kotlin")),
            Map.entry("Android", List.of("android")),
            Map.entry("React", List.of("react", "reactjs")),
            Map.entry("Spring Boot", List.of("spring boot", "spring")),
            Map.entry(".NET", List.of(".net", "c#", "dotnet")),
            Map.entry("Git", List.of("git", "github")),
            Map.entry("HTML", List.of("html")),
            Map.entry("CSS", List.of("css")),
            Map.entry("Power BI", List.of("power bi", "powerbi")),
            Map.entry("Redes", List.of("redes informaticas", "networking", "cisco", "tcp/ip")),
            Map.entry("Linux", List.of("linux")),
            Map.entry("Servicio al cliente", List.of("servicio al cliente", "atencion al cliente", "customer service",
                    "customer support")),
            Map.entry("Ventas", List.of("ventas", "sales")),
            Map.entry("Contabilidad", List.of("contabilidad", "accounting", "contable")),
            Map.entry("Comunicación", List.of("comunicacion", "communication skills")),
            Map.entry("Trabajo en equipo", List.of("trabajo en equipo", "teamwork")),
            Map.entry("Office", List.of("office", "microsoft office")),
            Map.entry("Diseño gráfico", List.of("diseno grafico", "graphic design")),
            Map.entry("Figma", List.of("figma")),
            Map.entry("Redes sociales", List.of("redes sociales", "social media")),
            Map.entry("SAP", List.of("sap")),
            Map.entry("Soporte técnico", List.of("soporte tecnico", "technical support", "help desk", "helpdesk")),
            Map.entry("Logística", List.of("logistica", "logistics", "inventario", "inventory")));

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private SkillExtractor() {
    }

    /** Las habilidades del catálogo que aparecen en el texto, sin repetir y en orden alfabético. */
    public static List<String> extract(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = normalize(text);
        Set<String> found = new LinkedHashSet<>();
        CATALOG.forEach((name, words) -> {
            if (words.stream().anyMatch(word -> containsWord(normalized, word))) {
                found.add(name);
            }
        });
        return found.stream().sorted().toList();
    }

    /** Minúsculas y sin tildes, para que "Atención" y "atencion" sean lo mismo. */
    private static String normalize(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return MARKS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT);
    }

    /** Palabra completa: no puede tener letras o números pegados antes ni después. */
    private static boolean containsWord(String text, String word) {
        Pattern pattern = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(word) + "(?![\\p{L}\\p{N}])");
        return pattern.matcher(text).find();
    }
}
