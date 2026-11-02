package com.cvexpert.ofertas.config;

import com.cvexpert.ofertas.adapter.in.web.CallerArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra el resolver que convierte los encabezados de APIM en un {@code Caller}. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CallerArgumentResolver());
    }
}
