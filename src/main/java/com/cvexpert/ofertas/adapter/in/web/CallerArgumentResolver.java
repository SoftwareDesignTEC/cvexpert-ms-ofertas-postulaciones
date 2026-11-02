package com.cvexpert.ofertas.adapter.in.web;

import com.cvexpert.ofertas.domain.model.Caller;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Arma el {@link Caller} de cada solicitud a partir de los encabezados que pone APIM. Con esto los
 * controladores reciben un {@code Caller} como parámetro y no repiten la lectura de encabezados.
 *
 * <p>Si falta {@code X-User-Id}, el constructor de {@code Caller} lanza la excepción que termina
 * en un 400.
 */
public class CallerArgumentResolver implements HandlerMethodArgumentResolver {

    static final String USER_ID = "X-User-Id";
    static final String USER_EMAIL = "X-User-Email";
    static final String USER_ROLE = "X-User-Role";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(Caller.class);
    }

    @Override
    public Caller resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest request, WebDataBinderFactory binderFactory) {
        return new Caller(request.getHeader(USER_ID), request.getHeader(USER_EMAIL), request.getHeader(USER_ROLE));
    }
}
